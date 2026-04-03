package com.compiler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data 
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private Long contestId;

    private int timeLimit; 
    private int memoryLimit;

    @Lob
    @Column(columnDefinition = "TEXT") // Postgres friendly
    private String description;
}