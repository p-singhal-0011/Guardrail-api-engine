package com.priyansh.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.priyansh.entity.Comment;
import com.priyansh.repository.CommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public Comment addComment(Long postId, Comment comment) {

    	
    	if (comment.getDepthLevel() > 20) {
    	    throw new ResponseStatusException(
    	        HttpStatus.BAD_REQUEST,
    	        "Max depth exceeded"
    	    );
    	}
    	
    	boolean isBot = true;

        if (isBot) {
            String botKey = "post:" + postId + ":bot_count";

            Long newCount = redisTemplate.opsForValue().increment(botKey);

            if (newCount > 100) {
                redisTemplate.opsForValue().decrement(botKey);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot limit reached");
            }
        }
        
        
        Long botId = comment.getAuthorId();
        Long humanId = 1L;

        String cooldownKey = "cooldown:bot_" + botId + ":human_" + humanId;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Cooldown active: try after some time"
            );
        }

	     comment.setPostId(postId);
	     comment.setCreatedAt(LocalDateTime.now());
        
	     Comment saved = commentRepository.save(comment);

	             redisTemplate.opsForValue().set(
            cooldownKey,
            "1",
            Duration.ofMinutes(10)
	      );
	             
//        System.out.println("Bot count = " + count);
//        System.out.println("Updating Redis for post " + postId);
	     
	     String notifKey = "user:" + humanId + ":notif_cooldown";
	     String listKey = "user:" + humanId + ":pending_notifs";

	     String message = "Bot " + botId + " replied to your post";

	     if (Boolean.TRUE.equals(redisTemplate.hasKey(notifKey))) {
	         redisTemplate.opsForList().rightPush(listKey, message);
	     } else {
	         System.out.println("Push Notification Sent: " + message);

	         redisTemplate.opsForValue().set(
	             notifKey,
	             "1",
	             Duration.ofMinutes(15)
	         );
	     }
	     
	     
        updateVirality(postId, 50);
        
        return saved;
    }
    
    private void updateVirality(Long postId, int points) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, points);
    }
}
