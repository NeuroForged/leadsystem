package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByPrimaryEmail(String email);
}
