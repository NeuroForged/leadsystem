package com.neuroforged.leadsystem.config;

import io.github.cdimascio.dotenv.Dotenv;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DotEnvLoader {

    @PostConstruct
    public void init() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./") // root dir
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
    }
}
