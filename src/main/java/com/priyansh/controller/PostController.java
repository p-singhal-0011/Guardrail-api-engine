package com.priyansh.controller;

import com.priyansh.entity.Post;
import com.priyansh.service.PostService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public Post createPost(@RequestBody Post post) {
        return postService.createPost(post);
    }
    
    @PostMapping("/{postId}/like")
    public ResponseEntity<Post> likePost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.likePost(postId));
    }
}
