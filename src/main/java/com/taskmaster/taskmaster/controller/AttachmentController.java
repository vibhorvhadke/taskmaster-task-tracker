package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.dto.attachment.AttachmentResponse;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import com.taskmaster.taskmaster.service.AttachmentService;
import com.taskmaster.taskmaster.service.AttachmentService.FileDownload;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Attachment endpoints. All require a valid token.
 *
 * POST   /api/tasks/{taskId}/attachments                  -> 201 Created (multipart form field "file")
 * GET    /api/tasks/{taskId}/attachments                  -> 200 OK + list
 * GET    /api/tasks/{taskId}/attachments/{id}/download    -> 200 OK + file content
 * DELETE /api/tasks/{taskId}/attachments/{id}             -> 204 No Content (uploader or team OWNER)
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @PathVariable Long taskId,
                                                     @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(taskId, currentUser.id(), file));
    }

    @GetMapping
    public ResponseEntity<List<AttachmentResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                         @PathVariable Long taskId) {
        return ResponseEntity.ok(attachmentService.getAttachments(taskId, currentUser.id()));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @PathVariable Long taskId,
                                             @PathVariable Long attachmentId) {
        FileDownload file = attachmentService.download(taskId, attachmentId, currentUser.id());

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                       @PathVariable Long taskId,
                                       @PathVariable Long attachmentId) {
        attachmentService.delete(taskId, attachmentId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}