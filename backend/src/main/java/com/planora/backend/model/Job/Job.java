package com.planora.backend.model.Job;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "job_table")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long boardId;
    private Long columnId;

    @Column(columnDefinition = "TEXT")
    private String jwtToken;

    private Long userId;
    private String repository;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private JobStatus status = JobStatus.PROCESSING;

}
