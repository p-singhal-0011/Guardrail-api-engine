package com.priyansh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.priyansh.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
