package com.planora.backend.repository;

import com.planora.backend.model.issue.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {

    Optional<Label> findByName(String name);
}
