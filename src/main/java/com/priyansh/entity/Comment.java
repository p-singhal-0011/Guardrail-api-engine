package com.priyansh.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long postId;

    private Long authorId;

    private String authorType; 
    
    private String content;

    private int depthLevel;

    private LocalDateTime createdAt;
}