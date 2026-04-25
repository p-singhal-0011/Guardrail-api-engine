package com.priyansh.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationScheduler {

    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedDelay = 300000)
    public void processNotifications() {

    	Set<String> keys = redisTemplate.keys("user:*:pending_notifs");

    	for (String listKey : keys) {
    	    List<Object> messages = redisTemplate.opsForList().range(listKey, 0, -1);

    	    if (messages != null && !messages.isEmpty()) {
    	        int count = messages.size();

    	        System.out.println(
    	            "Summarized Notification: " +
    	            messages.get(0) + " and " + (count - 1) + " others interacted"
    	        );

    	        redisTemplate.delete(listKey);
    	    }
    	}
    }
}