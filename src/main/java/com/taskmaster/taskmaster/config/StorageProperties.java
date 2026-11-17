package com.taskmaster.taskmaster.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * File storage settings loaded from application.properties (prefix "app.storage").
 *
 * app.storage.upload-dir -> uploadDir (folder where attachment files are saved)
 */
@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(

        @NotBlank(message = "app.storage.upload-dir must be set")
        String uploadDir
) {
}