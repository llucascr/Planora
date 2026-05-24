package com.planora.backend.repository;

import com.planora.backend.model.Job.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByBoardId(Long boardId);
}
