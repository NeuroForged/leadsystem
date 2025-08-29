package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.ClientAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientAnswerRepository extends JpaRepository<ClientAnswer, Long> {
    Optional<ClientAnswer> findByClientIdAndQuestionKey(Long clientId, String questionKey);
}
