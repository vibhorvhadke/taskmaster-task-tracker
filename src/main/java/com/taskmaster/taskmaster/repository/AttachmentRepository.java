package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.entity.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Data access for task attachments (file metadata).
 */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /** All attachments of a task, oldest first, with the uploader loaded in the same query. */
    @EntityGraph(attributePaths = "uploadedBy")
    List<Attachment> findByTaskIdOrderByUploadedAtAsc(Long taskId);

    /** Only the file paths of a task's attachments (no entities loaded). */
    @Query("SELECT a.storagePath FROM Attachment a WHERE a.task.id = :taskId")
    List<String> findStoragePathsByTaskId(@Param("taskId") Long taskId);
}
