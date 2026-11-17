package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.config.StorageProperties;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Saves, loads and deletes attachment files on the local disk.
 *
 * Files are stored as  {upload-dir}/task-{taskId}/{random-uuid}.{ext}
 * The user's original file name is never used on disk, which prevents
 * path-traversal attacks (e.g. a file named "..\..\evil.exe").
 * Only the relative path is saved in the database.
 */
@Slf4j
@Service
public class FileStorageService {

    private final Path rootDir;

    public FileStorageService(StorageProperties properties) {
        this.rootDir = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create upload directory: " + rootDir, ex);
        }
        log.info("Attachment files will be stored in {}", rootDir);
    }

    /**
     * Writes the uploaded file to disk.
     *
     * @return the relative storage path to save in the database
     */
    public String store(Long taskId, MultipartFile file) {
        String relativePath = "task-" + taskId + "/" + UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        Path target = resolve(relativePath);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store file", ex);
        }
        return relativePath;
    }

    /**
     * @throws ResourceNotFoundException if the file is missing on disk
     */
    public Resource load(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.isReadable(path)) {
            throw new ResourceNotFoundException("File not found on the server");
        }
        return new FileSystemResource(path);
    }

    /** Deletes the file. A failure is logged but never breaks the request. */
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("Could not delete stored file {}: {}", relativePath, ex.getMessage());
        }
    }

    /** Resolves a relative path inside the upload folder and refuses anything outside it. */
    private Path resolve(String relativePath) {
        Path path = rootDir.resolve(relativePath).normalize();
        if (!path.startsWith(rootDir)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        return path;
    }

    /** Keeps a short, safe extension (letters/digits only), e.g. ".pdf". */
    private String extensionOf(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (extension == null || !extension.matches("[A-Za-z0-9]{1,10}")) {
            return "";
        }
        return "." + extension.toLowerCase(Locale.ROOT);
    }
}