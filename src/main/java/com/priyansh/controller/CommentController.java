package com.priyansh.controller;

import org.springframework.web.bind.annotation.*;

import com.priyansh.entity.Comment;
import com.priyansh.service.CommentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}/comments")
    public Comment addComment(@PathVariable Long postId,
                              @RequestBody Comment comment) {
        return commentService.addComment(postId, comment);
    }
}
