package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.attachment.AttachmentResponse;
import com.taskmaster.taskmaster.entity.Attachment;
import com.taskmaster.taskmaster.entity.Task;
import com.taskmaster.taskmaster.entity.User;
import com.taskmaster.taskmaster.enums.TeamRole;
import com.taskmaster.taskmaster.exception.BadRequestException;
import com.taskmaster.taskmaster.exception.ForbiddenException;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.taskmaster.repository.AttachmentRepository;
import com.taskmaster.taskmaster.repository.TeamMemberRepository;
import com.taskmaster.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Uploading, listing, downloading and deleting task attachments.
 *
 * Permissions:
 *  - upload / list / download : anyone who can see the task
 *  - delete                   : the uploader, or the team OWNER
 *
 * Disk and database are kept in sync with transaction callbacks:
 *  - upload : if the database insert rolls back, the file just written is removed
 *  - delete : the file is removed only after the database delete has committed
 */
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final String DEFAULT_CONTENT_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    private final AttachmentRepository attachmentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final FileStorageService fileStorageService;

    /** File content plus the details needed to send it back to the client. */
    public record FileDownload(String fileName, String contentType, long fileSize, Resource resource) {
    }

    @Transactional
    public AttachmentResponse upload(Long taskId, Long userId, MultipartFile file) {
        Task task = taskService.requireTaskAccess(taskId, userId);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required and must not be empty");
        }

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String storagePath = fileStorageService.store(taskId, file);
        afterCompletion(rolledBack -> {
            if (rolledBack) {
                fileStorageService.delete(storagePath);
            }
        });

        Attachment attachment = attachmentRepository.saveAndFlush(Attachment.builder()
                .task(task)
                .uploadedBy(uploader)
                .fileName(cleanFileName(file.getOriginalFilename()))
                .contentType(cleanContentType(file.getContentType()))
                .fileSize(file.getSize())
                .storagePath(storagePath)
                .build());

        return AttachmentResponse.from(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long taskId, Long userId) {
        taskService.requireTaskAccess(taskId, userId);
        return attachmentRepository.findByTaskIdOrderByUploadedAtAsc(taskId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FileDownload download(Long taskId, Long attachmentId, Long userId) {
        taskService.requireTaskAccess(taskId, userId);
        Attachment attachment = findAttachmentOfTask(taskId, attachmentId);

        return new FileDownload(
                attachment.getFileName(),
                attachment.getContentType() != null ? attachment.getContentType() : DEFAULT_CONTENT_TYPE,
                attachment.getFileSize(),
                fileStorageService.load(attachment.getStoragePath())
        );
    }

    @Transactional
    public void delete(Long taskId, Long attachmentId, Long userId) {
        Task task = taskService.requireTaskAccess(taskId, userId);
        Attachment attachment = findAttachmentOfTask(taskId, attachmentId);

        boolean isUploader = attachment.getUploadedBy().getId().equals(userId);
        if (!isUploader && !isTeamOwner(task, userId)) {
            throw new ForbiddenException("Only the uploader or the team owner can delete this attachment");
        }

        String storagePath = attachment.getStoragePath();
        attachmentRepository.delete(attachment);
        afterCompletion(rolledBack -> {
            if (!rolledBack) {
                fileStorageService.delete(storagePath);
            }
        });
    }

    // ---------------------------------------------------------------------

    /** An attachment ID from a different task is treated as "not found" for this task. */
    private Attachment findAttachmentOfTask(Long taskId, Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .filter(a -> a.getTask().getId().equals(taskId))
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
    }

    private boolean isTeamOwner(Task task, Long userId) {
        if (task.getTeam() == null) {
            return false;
        }
        return teamMemberRepository.findByTeamIdAndUserId(task.getTeam().getId(), userId)
                .map(membership -> membership.getRole() == TeamRole.OWNER)
                .orElse(false);
    }

    /** Runs the callback once the current transaction finishes; the flag says whether it rolled back. */
    private void afterCompletion(java.util.function.Consumer<Boolean> callback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                callback.accept(status == STATUS_ROLLED_BACK);
            }
        });
    }

    /** Keeps only the file name part (no folders) and limits it to the column length. */
    private String cleanFileName(String originalFileName) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(
                originalFileName == null ? "" : originalFileName));
        if (name == null || name.isBlank()) {
            return "file";
        }
        // Keep the end of very long names so the extension survives
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    /** Accepts only a valid MIME type that fits the column; otherwise uses application/octet-stream. */
    private String cleanContentType(String contentType) {
        if (contentType == null || contentType.length() > 100) {
            return DEFAULT_CONTENT_TYPE;
        }
        try {
            return MediaType.parseMediaType(contentType).toString();
        } catch (IllegalArgumentException ex) {
            return DEFAULT_CONTENT_TYPE;
        }
    }
}