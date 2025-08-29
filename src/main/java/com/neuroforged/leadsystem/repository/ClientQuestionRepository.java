package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.ClientQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientQuestionRepository extends JpaRepository<ClientQuestion, Long> {
    List<ClientQuestion> findByClientIdOrderByPositionAsc(Long clientId);
}
