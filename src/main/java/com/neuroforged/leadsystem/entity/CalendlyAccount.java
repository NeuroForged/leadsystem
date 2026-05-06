package com.neuroforged.leadsystem.entity;

import com.neuroforged.leadsystem.config.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

@FilterDef(name = "longClientFilter", parameters = @ParamDef(name = "clientId", type = Long.class))
@Filter(name = "longClientFilter", condition = "client_id = :clientId")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendlyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 2000)
    private String accessToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 2000)
    private String refreshToken;
    private String owner;
    private String ownerType;
    private String organization;

    @Column(unique = true)
    private Long clientId;

    private LocalDateTime tokenIssuedAt;
    private boolean requiresReauth;

    private boolean usePolling;
    private LocalDateTime lastPolledAt;
}
