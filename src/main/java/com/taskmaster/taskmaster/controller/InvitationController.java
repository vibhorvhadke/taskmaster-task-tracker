package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.dto.invitation.InvitationResponse;
import com.taskmaster.taskmaster.dto.invitation.InviteMemberRequest;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import com.taskmaster.taskmaster.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Invitation endpoints. All require a valid token.
 *
 * POST /api/teams/{teamId}/invitations      -> 201 Created  (team OWNER only)
 * GET  /api/invitations                     -> 200 OK, my pending invitations
 * POST /api/invitations/{invitationId}/accept  -> 200 OK  (invited user only)
 * POST /api/invitations/{invitationId}/decline -> 200 OK  (invited user only)
 */
@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/api/teams/{teamId}/invitations")
    public ResponseEntity<InvitationResponse> invite(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @PathVariable Long teamId,
                                                     @Valid @RequestBody InviteMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.invite(teamId, currentUser.id(), request));
    }

    @GetMapping("/api/invitations")
    public ResponseEntity<List<InvitationResponse>> getMyInvitations(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(invitationService.getMyPendingInvitations(currentUser.id()));
    }

    @PostMapping("/api/invitations/{invitationId}/accept")
    public ResponseEntity<InvitationResponse> accept(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @PathVariable Long invitationId) {
        return ResponseEntity.ok(invitationService.accept(invitationId, currentUser.id()));
    }

    @PostMapping("/api/invitations/{invitationId}/decline")
    public ResponseEntity<InvitationResponse> decline(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                      @PathVariable Long invitationId) {
        return ResponseEntity.ok(invitationService.decline(invitationId, currentUser.id()));
    }
}