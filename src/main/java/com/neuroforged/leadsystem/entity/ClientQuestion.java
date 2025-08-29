package com.neuroforged.leadsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ClientQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long clientId;
    @Column(nullable = false, length = 64)
    private String questionKey;
    @Column(nullable = false, length = 255)
    private String questionText;
    private String dataType;
    private boolean active = true;
    private int position = 0;
}
