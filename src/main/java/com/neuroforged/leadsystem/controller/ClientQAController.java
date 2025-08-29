package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.entity.ClientQuestion;
import com.neuroforged.leadsystem.entity.ClientAnswer;
import com.neuroforged.leadsystem.repository.ClientQuestionRepository;
import com.neuroforged.leadsystem.repository.ClientAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients/{clientId}")
public class ClientQAController {

    private final ClientQuestionRepository questionRepository;
    private final ClientAnswerRepository answerRepository;

    @Autowired
    public ClientQAController(ClientQuestionRepository questionRepository, ClientAnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @GetMapping("/questions")
    public List<ClientQuestion> getQuestions(@PathVariable Long clientId) {
        return questionRepository.findByClientIdOrderByPositionAsc(clientId);
    }

    @PutMapping("/answers")
    public ResponseEntity<Void> upsertAnswers(@PathVariable Long clientId, @RequestBody Map<String, String> answers) {
        Instant now = Instant.now();
        answers.forEach((key, value) -> {
            ClientAnswer answer = answerRepository.findByClientIdAndQuestionKey(clientId, key).orElseGet(() -> {
                ClientAnswer newAnswer = new ClientAnswer();
                newAnswer.setClientId(clientId);
                newAnswer.setQuestionKey(key);
                return newAnswer;
            });
            answer.setValue(value);
            answer.setUpdatedAt(now);
            answerRepository.save(answer);
        });
        return ResponseEntity.noContent().build();
    }
}
