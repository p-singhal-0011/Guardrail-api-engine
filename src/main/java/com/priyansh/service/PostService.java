package com.priyansh.service;


import com.priyansh.entity.Post;
import com.priyansh.repository.PostRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    
    public Post createPost(Post post) {
        post.setCreatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }
    
    public Post likePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        post.setLikeCount(post.getLikeCount() + 1);
        Post saved = postRepository.save(post);

        updateViralityScore(postId, 20);

        return saved;
    }
    
    public void updateViralityScore(Long postId, int points) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, points);
    }
    
}