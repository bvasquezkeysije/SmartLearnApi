package com.bardales.SmartLearnApi.controller;

import com.bardales.SmartLearnApi.dto.share.ShareLinkClaimRequest;
import com.bardales.SmartLearnApi.dto.share.ShareLinkClaimResponse;
import com.bardales.SmartLearnApi.dto.share.ShareLinkCreateRequest;
import com.bardales.SmartLearnApi.dto.share.ShareLinkDistributeRequest;
import com.bardales.SmartLearnApi.dto.share.ShareLinkDistributeResponse;
import com.bardales.SmartLearnApi.dto.share.ShareNotificationRecipientResponse;
import com.bardales.SmartLearnApi.dto.share.ShareNotificationResponse;
import com.bardales.SmartLearnApi.dto.share.ShareLinkResponse;
import com.bardales.SmartLearnApi.service.ShareLinkService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/share-links")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    public ShareLinkController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @PostMapping("/exams/{examId}")
    public ShareLinkResponse createExamShareLink(
            @PathVariable Long examId, @Valid @RequestBody ShareLinkCreateRequest request) {
        return shareLinkService.createExamShareLink(examId, request);
    }

    @PostMapping("/courses/{courseId}")
    public ShareLinkResponse createCourseShareLink(
            @PathVariable Long courseId, @Valid @RequestBody ShareLinkCreateRequest request) {
        return shareLinkService.createCourseShareLink(courseId, request);
    }

    @PostMapping("/claim")
    public ShareLinkClaimResponse claimShareLink(@Valid @RequestBody ShareLinkClaimRequest request) {
        return shareLinkService.claimShareLink(request);
    }

    @GetMapping("/recipients")
    public List<ShareNotificationRecipientResponse> listRecipients(@RequestParam Long userId) {
        return shareLinkService.listRecipients(userId);
    }

    @PostMapping("/distribute")
    public ShareLinkDistributeResponse distribute(@Valid @RequestBody ShareLinkDistributeRequest request) {
        return shareLinkService.distributeShareLink(request);
    }

    @GetMapping("/notifications")
    public List<ShareNotificationResponse> listNotifications(@RequestParam Long userId) {
        return shareLinkService.listNotifications(userId);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ShareNotificationResponse markNotificationAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long userId) {
        return shareLinkService.markNotificationAsRead(notificationId, userId);
    }
}
