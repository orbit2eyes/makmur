package com.makmur.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class DataSourceConfig {

    @PostConstruct
    public void init() throws IOException {
        Path dataDir = Paths.get("data");
        Files.createDirectories(dataDir);
    }
}