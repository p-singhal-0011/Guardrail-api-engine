package com.priyansh.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long authorId;

    @Column(length = 1000)
    private String content;
    
    @Column(nullable = false)
    private int likeCount = 0;

    private LocalDateTime createdAt;
}