package com.taskmaster.taskmaster.dto.attachment;

import com.taskmaster.taskmaster.dto.user.UserSummary;
import com.taskmaster.taskmaster.entity.Attachment;

import java.time.Instant;

/**
 * Details of a file attached to a task.
 * The storage path on disk is internal and never returned to the client.
 */
public record AttachmentResponse(
        Long id,
        Long taskId,
        String fileName,
        String contentType,
        long fileSize,
        UserSummary uploadedBy,
        Instant uploadedAt,
        String downloadUrl
) {

    public static AttachmentResponse from(Attachment attachment) {
        Long taskId = attachment.getTask().getId();
        return new AttachmentResponse(
                attachment.getId(),
                taskId,
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                UserSummary.from(attachment.getUploadedBy()),
                attachment.getUploadedAt(),
                "/api/tasks/" + taskId + "/attachments/" + attachment.getId() + "/download"
        );
    }
}