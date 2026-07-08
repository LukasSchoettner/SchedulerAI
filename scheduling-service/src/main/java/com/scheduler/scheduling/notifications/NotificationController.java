package com.scheduler.scheduling.notifications;

import com.scheduler.commoncode.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    public NotificationController(NotificationService notificationService, JwtUtil jwtUtil) {
        this.notificationService = notificationService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> list(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) Integer limit,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(notificationService.list(extractCustomerId(authHeader), status, limit));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> unread(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(notificationService.unread(extractCustomerId(authHeader)));
    }

    @GetMapping("/due")
    public ResponseEntity<List<NotificationDTO>> due(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(notificationService.due(extractCustomerId(authHeader)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> read(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(notificationService.markRead(extractCustomerId(authHeader), id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<List<NotificationDTO>> readAll(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(notificationService.markAllRead(extractCustomerId(authHeader)));
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<NotificationDTO> dismiss(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(notificationService.dismiss(extractCustomerId(authHeader), id));
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractCustomerId(token);
    }
}
