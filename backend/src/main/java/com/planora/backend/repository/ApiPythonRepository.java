package com.planora.backend.repository;

import com.planora.backend.model.Job.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiPythonRepository extends JpaRepository<Job, Long> {
}
