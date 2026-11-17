package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.dto.team.CreateTeamRequest;
import com.taskmaster.taskmaster.dto.team.TeamMemberResponse;
import com.taskmaster.taskmaster.dto.team.TeamResponse;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import com.taskmaster.taskmaster.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Team endpoints. All require a valid token.
 *
 * POST /api/teams                    -> 201 Created + team (Location header points to the new team)
 * GET  /api/teams                    -> 200 OK + teams I belong to
 * GET  /api/teams/{teamId}           -> 200 OK + team      (members only)
 * GET  /api/teams/{teamId}/members   -> 200 OK + members   (members only)
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @Valid @RequestBody CreateTeamRequest request) {
        TeamResponse team = teamService.createTeam(currentUser.id(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(team.id())
                .toUri();

        return ResponseEntity.created(location).body(team);
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(teamService.getMyTeams(currentUser.id()));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeam(teamId, currentUser.id()));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                               @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getMembers(teamId, currentUser.id()));
    }
}