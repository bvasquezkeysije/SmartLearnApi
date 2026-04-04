package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bardales.SmartLearnApi.domain.entity.Course;
import com.bardales.SmartLearnApi.domain.entity.CourseMembership;
import com.bardales.SmartLearnApi.domain.entity.ShareLink;
import com.bardales.SmartLearnApi.domain.entity.ShareNotification;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.CourseMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.CourseRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.ShareLinkRepository;
import com.bardales.SmartLearnApi.domain.repository.ShareNotificationRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.share.ShareLinkClaimRequest;
import com.bardales.SmartLearnApi.dto.share.ShareLinkClaimResponse;
import com.bardales.SmartLearnApi.dto.share.ShareNotificationResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamMembershipRepository examMembershipRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMembershipRepository courseMembershipRepository;

    @Mock
    private ShareNotificationRepository shareNotificationRepository;

    @Mock
    private ExamService examService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private SalaService salaService;

    private ShareLinkService shareLinkService;

    @BeforeEach
    void setUp() {
        shareLinkService = new ShareLinkService(
                shareLinkRepository,
                userRepository,
                examRepository,
                examMembershipRepository,
                courseRepository,
                courseMembershipRepository,
                shareNotificationRepository,
                examService,
                scheduleService,
                salaService);
    }

    @Test
    void claimCourseLinkCreatesPendingInvitationWithoutAutoMembership() {
        User owner = buildUser(1L, "Owner", "owner");
        User recipient = buildUser(2L, "Recipient", "recipient");

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso Algebra");
        setBaseFields(course, 100L);

        ShareLink shareLink = new ShareLink();
        shareLink.setOwnerUser(owner);
        shareLink.setResourceType("course");
        shareLink.setResourceId(100L);
        shareLink.setToken("token-course");
        shareLink.setActive(Boolean.TRUE);
        shareLink.setClaimsCount(0);
        shareLink.setExpiresAt(LocalDateTime.now().plusHours(2));
        setBaseFields(shareLink, 200L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(shareLinkRepository.findByTokenAndActiveIsTrueAndDeletedAtIsNull("token-course"))
                .thenReturn(Optional.of(shareLink));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(shareNotificationRepository.findTopByShareLinkIdAndRecipientUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(200L, 2L))
                .thenReturn(Optional.empty());
        when(shareNotificationRepository.save(any(ShareNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShareLinkClaimResponse response = shareLinkService.claimShareLink(new ShareLinkClaimRequest(2L, "token-course"));

        assertEquals("course", response.resourceType());
        assertTrue(response.message().contains("Aceptala desde notificaciones"));
        verify(courseMembershipRepository, never()).save(any(CourseMembership.class));

        ArgumentCaptor<ShareNotification> notificationCaptor = ArgumentCaptor.forClass(ShareNotification.class);
        verify(shareNotificationRepository).save(notificationCaptor.capture());
        assertEquals("pending", notificationCaptor.getValue().getInvitationStatus());
    }

    @Test
    void acceptCourseInvitationUpsertsMembershipIdempotently() {
        User owner = buildUser(1L, "Owner", "owner");
        User recipient = buildUser(2L, "Recipient", "recipient");

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso Fisica");
        setBaseFields(course, 300L);

        ShareNotification notification = new ShareNotification();
        notification.setRecipientUser(recipient);
        notification.setResourceType("course");
        notification.setResourceId(300L);
        notification.setInvitationStatus("pending");
        setBaseFields(notification, 400L);

        CourseMembership existingMembership = new CourseMembership();
        existingMembership.setCourse(course);
        existingMembership.setUser(recipient);
        existingMembership.setRole("viewer");
        setBaseFields(existingMembership, 500L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(shareNotificationRepository.findByIdAndRecipientUserIdAndDeletedAtIsNull(400L, 2L))
                .thenReturn(Optional.of(notification));
        when(courseRepository.findById(300L)).thenReturn(Optional.of(course));
        when(courseMembershipRepository.findByCourseIdAndUserId(300L, 2L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingMembership));
        when(courseMembershipRepository.save(any(CourseMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shareNotificationRepository.save(any(ShareNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShareNotificationResponse first = shareLinkService.acceptNotificationInvitation(400L, 2L);
        ShareNotificationResponse second = shareLinkService.acceptNotificationInvitation(400L, 2L);

        assertEquals("accepted", first.invitationStatus());
        assertEquals("accepted", second.invitationStatus());
        verify(courseMembershipRepository, times(2)).save(any(CourseMembership.class));
    }

    @Test
    void rejectCourseInvitationDoesNotCreateMembership() {
        User recipient = buildUser(2L, "Recipient", "recipient");

        ShareNotification notification = new ShareNotification();
        notification.setRecipientUser(recipient);
        notification.setResourceType("course");
        notification.setResourceId(300L);
        notification.setInvitationStatus("pending");
        setBaseFields(notification, 401L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(shareNotificationRepository.findByIdAndRecipientUserIdAndDeletedAtIsNull(401L, 2L))
                .thenReturn(Optional.of(notification));
        when(shareNotificationRepository.save(any(ShareNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShareNotificationResponse response = shareLinkService.rejectNotificationInvitation(401L, 2L);

        assertEquals("rejected", response.invitationStatus());
        verify(courseMembershipRepository, never()).save(any(CourseMembership.class));
    }

    private User buildUser(Long id, String name, String username) {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setEmail(username + "@mail.com");
        setBaseFields(user, id);
        return user;
    }

    private static void setBaseFields(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "createdAt", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(target, "updatedAt", LocalDateTime.now());
    }
}
