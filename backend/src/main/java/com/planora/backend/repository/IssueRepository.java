package com.planora.backend.repository;

import com.planora.backend.model.issue.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, Long> {
}
