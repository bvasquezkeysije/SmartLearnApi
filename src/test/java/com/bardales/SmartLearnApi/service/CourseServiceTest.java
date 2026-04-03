package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bardales.SmartLearnApi.domain.entity.Course;
import com.bardales.SmartLearnApi.domain.entity.CourseCompetency;
import com.bardales.SmartLearnApi.domain.entity.CourseExam;
import com.bardales.SmartLearnApi.domain.entity.CourseMembership;
import com.bardales.SmartLearnApi.domain.entity.CourseSession;
import com.bardales.SmartLearnApi.domain.entity.CourseSessionContent;
import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.ExamAttempt;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.CourseCompetencyRepository;
import com.bardales.SmartLearnApi.domain.repository.CourseExamRepository;
import com.bardales.SmartLearnApi.domain.repository.CourseMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.CourseRepository;
import com.bardales.SmartLearnApi.domain.repository.CourseSessionContentRepository;
import com.bardales.SmartLearnApi.domain.repository.CourseSessionRepository;
import com.bardales.SmartLearnApi.domain.repository.CourseWeekRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamAttemptRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.course.CourseCompetencySaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseModuleResponse;
import com.bardales.SmartLearnApi.dto.course.CourseParticipantSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseResponse;
import com.bardales.SmartLearnApi.dto.course.CourseSessionContentPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamRenameRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupJoinRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStateResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamSummaryResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMembershipRepository courseMembershipRepository;

    @Mock
    private CourseExamRepository courseExamRepository;

    @Mock
    private CourseSessionRepository courseSessionRepository;

    @Mock
    private CourseSessionContentRepository courseSessionContentRepository;

    @Mock
    private CourseWeekRepository courseWeekRepository;

    @Mock
    private CourseCompetencyRepository courseCompetencyRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExamService examService;

    @Mock
    private ExamGroupPracticeService examGroupPracticeService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                courseMembershipRepository,
                courseExamRepository,
                courseSessionRepository,
                courseWeekRepository,
                courseSessionContentRepository,
                courseCompetencyRepository,
                examAttemptRepository,
                examRepository,
                userRepository,
                examService,
                examGroupPracticeService);
    }

    @Test
    void getModuleReturnsParticipantsCompetenciesAndGrades() {
        User owner = new User();
        owner.setName("Owner User");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        User participantUser = new User();
        participantUser.setName("Student User");
        participantUser.setUsername("student");
        participantUser.setEmail("student@mail.com");
        setBaseFields(participantUser, 2L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso de prueba");
        course.setVisibility("private");
        course.setPriority("important");
        course.setSortOrder(0);
        setBaseFields(course, 10L);

        CourseMembership membership = new CourseMembership();
        membership.setCourse(course);
        membership.setUser(participantUser);
        membership.setRole("editor");
        setBaseFields(membership, 11L);

        CourseSession session = new CourseSession();
        session.setCourse(course);
        session.setName("SESION 1: Intro");
        setBaseFields(session, 12L);

        Exam exam = new Exam();
        exam.setUser(owner);
        exam.setName("Examen base");
        exam.setQuestionsCount(10);
        setBaseFields(exam, 13L);

        CourseExam courseExam = new CourseExam();
        courseExam.setCourse(course);
        courseExam.setExam(exam);
        setBaseFields(courseExam, 14L);

        CourseSessionContent content = new CourseSessionContent();
        content.setCourseSession(session);
        content.setType("exam");
        content.setTitle("Repaso");
        content.setSourceExam(exam);
        setBaseFields(content, 15L);

        CourseCompetency competency = new CourseCompetency();
        competency.setCourse(course);
        competency.setName("Resolucion de problemas");
        competency.setLevel("intermedio");
        competency.setSortOrder(1);
        setBaseFields(competency, 16L);

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExam(exam);
        attempt.setUser(participantUser);
        attempt.setTotalPoints(20);
        attempt.setScoredPoints(15);
        attempt.setFinishedAt(LocalDateTime.now().minusMinutes(2));
        setBaseFields(attempt, 17L);

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L)).thenReturn(List.of(course));
        when(courseMembershipRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(courseRepository.findByVisibilityIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc("public"))
                .thenReturn(List.of());
        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(10L)).thenReturn(List.of(session));
        when(courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(12L))
                .thenReturn(List.of());
        when(courseSessionContentRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(12L))
                .thenReturn(List.of(content));
        when(courseExamRepository.findByCourseIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(courseExam));
        when(courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(membership));
        when(courseCompetencyRepository.findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(10L))
                .thenReturn(List.of(competency));
        when(examAttemptRepository.findByExamIdInAndUserIdIn(anyList(), anyList())).thenReturn(List.of(attempt));

        CourseModuleResponse module = courseService.getModule(1L);

        assertEquals(1, module.courses().size());
        CourseResponse response = module.courses().getFirst();
        assertEquals(2, response.participants().size());
        assertEquals(1, response.competencies().size());
        assertEquals("Resolucion de problemas", response.competencies().getFirst().name());
        assertEquals(2, response.grades().size());

        var participantGrade = response.grades().stream()
                .filter(grade -> grade.userId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertEquals(1, participantGrade.attemptsCount());
        assertEquals(75.0d, participantGrade.averageScore());
    }

    @Test
    void addCourseParticipantReactivatesDeletedMembership() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        User participant = new User();
        participant.setName("Participant");
        participant.setUsername("student");
        participant.setEmail("student@mail.com");
        setBaseFields(participant, 2L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso");
        course.setVisibility("private");
        course.setPriority("important");
        course.setSortOrder(0);
        setBaseFields(course, 20L);

        CourseMembership softDeletedMembership = new CourseMembership();
        softDeletedMembership.setCourse(course);
        softDeletedMembership.setUser(participant);
        softDeletedMembership.setRole("viewer");
        softDeletedMembership.setDeletedAt(LocalDateTime.now().minusDays(1));
        setBaseFields(softDeletedMembership, 21L);

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseRepository.findByIdAndUserIdAndDeletedAtIsNull(20L, 1L)).thenReturn(Optional.of(course));
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("student", "student"))
                .thenReturn(Optional.of(participant));
        when(courseMembershipRepository.findByCourseIdAndUserId(20L, 2L)).thenReturn(Optional.of(softDeletedMembership));
        when(courseMembershipRepository.save(any(CourseMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(20L)).thenReturn(List.of());
        when(courseExamRepository.findByCourseIdOrderByCreatedAtAsc(20L)).thenReturn(List.of());
        when(courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(20L))
                .thenReturn(List.of(softDeletedMembership));
        when(courseCompetencyRepository.findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(20L))
                .thenReturn(List.of());

        CourseResponse response = courseService.addCourseParticipant(
                20L,
                new CourseParticipantSaveRequest(1L, "student", "editor"));

        assertNull(softDeletedMembership.getDeletedAt());
        assertEquals("editor", softDeletedMembership.getRole());
        assertEquals(2, response.participants().size());
        assertEquals("editor", response.participants().get(1).role());
    }

    @Test
    void addCourseCompetencyPersistsAndReturnsCourseData() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso");
        course.setVisibility("public");
        course.setPriority("important");
        course.setSortOrder(0);
        setBaseFields(course, 30L);

        CourseCompetency saved = new CourseCompetency();
        saved.setCourse(course);
        saved.setName("Analisis");
        saved.setDescription("Capacidad de analisis");
        saved.setLevel("avanzado");
        saved.setSortOrder(2);
        setBaseFields(saved, 31L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseRepository.findByIdAndUserIdAndDeletedAtIsNull(30L, 1L)).thenReturn(Optional.of(course));
        when(courseCompetencyRepository.save(any(CourseCompetency.class))).thenReturn(saved);

        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(30L)).thenReturn(List.of());
        when(courseExamRepository.findByCourseIdOrderByCreatedAtAsc(30L)).thenReturn(List.of());
        when(courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(30L)).thenReturn(List.of());
        when(courseCompetencyRepository.findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(30L))
                .thenReturn(List.of(saved));

        CourseResponse response = courseService.addCourseCompetency(
                30L,
                new CourseCompetencySaveRequest(1L, "Analisis", "Capacidad de analisis", "avanzado", 2));

        assertNotNull(response.competencies());
        assertEquals(1, response.competencies().size());
        assertEquals("Analisis", response.competencies().getFirst().name());
        assertEquals("avanzado", response.competencies().getFirst().level());
    }

    @Test
    void getCourseSessionContentExamSummaryReturnsExamServiceSummary() {
        AnchoredExamFixture fixture = buildAnchoredExamFixture(100L, "exam");

        when(examService.getExamSummary(400L, 1L))
                .thenReturn(new ExamSummaryResponse(
                        400L,
                        "Examen anclado",
                        "EXM-400",
                        null,
                        6,
                        2L,
                        1L,
                        0L,
                        true,
                        "sequential",
                        false,
                        1L,
                        "private",
                        "owner",
                        true,
                        true,
                        true,
                        true,
                        true,
                        3L,
                        null,
                        null,
                        null,
                        LocalDateTime.now()));

        ExamSummaryResponse response =
                courseService.getCourseSessionContentExamSummary(200L, 300L, 500L, 1L);

        assertEquals(400L, response.id());
        assertEquals("Examen anclado", response.name());
        verify(examService).getExamSummary(400L, 1L);
        verify(examService, never()).upsertExamMembership(
                any(Exam.class), any(User.class), any(String.class), any(Boolean.class), any(Boolean.class), any(Boolean.class));
        assertNotNull(fixture.course);
    }

    @Test
    void renameCourseSessionContentExamDelegatesToExamServiceWithSourceExamId() {
        buildAnchoredExamFixture(110L, "exam");
        ExamRenameRequest request = new ExamRenameRequest(1L, "Nuevo nombre");

        when(examService.renameExam(eq(410L), eq(request)))
                .thenReturn(new ExamSummaryResponse(
                        410L,
                        "Nuevo nombre",
                        "EXM-410",
                        null,
                        10,
                        0L,
                        0L,
                        0L,
                        true,
                        "sequential",
                        false,
                        1L,
                        "private",
                        "owner",
                        true,
                        true,
                        true,
                        true,
                        true,
                        1L,
                        null,
                        null,
                        null,
                        LocalDateTime.now()));

        ExamSummaryResponse updated =
                courseService.renameCourseSessionContentExam(210L, 310L, 510L, request);

        assertEquals("Nuevo nombre", updated.name());
        verify(examService).renameExam(410L, request);
    }

    @Test
    void deleteCourseSessionContentExamDelegatesToExamServiceWithSourceExamId() {
        buildAnchoredExamFixture(120L, "exam");

        courseService.deleteCourseSessionContentExam(220L, 320L, 520L, 1L);

        verify(examService).deleteExam(420L, 1L);
    }

    @Test
    void renameCourseSessionContentExamFailsWhenContentIsNotExam() {
        buildAnchoredExamFixture(130L, "video");
        ExamRenameRequest request = new ExamRenameRequest(1L, "No aplica");

        assertThrows(
                BadRequestException.class,
                () -> courseService.renameCourseSessionContentExam(230L, 330L, 530L, request));
        verify(examService, never()).renameExam(any(Long.class), any(ExamRenameRequest.class));
    }

    @Test
    void startCourseSessionContentPracticeForOwnerDoesNotUpsertMembership() {
        buildAnchoredExamFixture(140L, "exam");

        CourseSessionContentPracticeStartResponse response =
                courseService.startCourseSessionContentPractice(240L, 340L, 540L, 1L);

        assertEquals(440L, response.examId());
        assertEquals("Examen anclado", response.examName());
        verify(examService, never()).upsertExamMembership(
                any(Exam.class), any(User.class), any(String.class), any(Boolean.class), any(Boolean.class), any(Boolean.class));
    }

    @Test
    void startCourseSessionContentPracticeForParticipantUpsertsViewerMembership() {
        AnchoredExamFixture fixture = buildAnchoredExamFixture(150L, "exam");
        User participant = new User();
        participant.setName("Participant");
        participant.setUsername("participant");
        participant.setEmail("participant@mail.com");
        setBaseFields(participant, 2L);
        CourseMembership membership = new CourseMembership();
        membership.setCourse(fixture.course);
        membership.setUser(participant);
        membership.setRole("viewer");
        setBaseFields(membership, 999L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(courseMembershipRepository.findByCourseIdAndUserIdAndDeletedAtIsNull(fixture.course.getId(), 2L))
                .thenReturn(Optional.of(membership));

        CourseSessionContentPracticeStartResponse response =
                courseService.startCourseSessionContentPractice(250L, 350L, 550L, 2L);

        assertEquals(450L, response.examId());
        verify(examService, times(1)).upsertExamMembership(
                eq(fixture.exam),
                eq(participant),
                eq("viewer"),
                eq(Boolean.FALSE),
                eq(Boolean.FALSE),
                eq(Boolean.FALSE));
    }

    @Test
    void startCourseSessionContentExamPracticeAttemptParticipantDelegatesWithoutReadOnlyFailure() {
        AnchoredExamFixture fixture = buildAnchoredExamFixture(160L, "exam");
        User participant = buildParticipantUser(2L);
        CourseMembership membership = buildParticipantMembership(fixture.course, participant, "viewer", 1001L);
        ExamPracticeStartResponse expected =
                new ExamPracticeStartResponse(9001L, 5, "ordered", true, true, LocalDateTime.now());

        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(courseMembershipRepository.findByCourseIdAndUserIdAndDeletedAtIsNull(fixture.course.getId(), 2L))
                .thenReturn(Optional.of(membership));
        when(examService.startPracticeAttempt(fixture.exam.getId(), 2L)).thenReturn(expected);

        ExamPracticeStartResponse response =
                courseService.startCourseSessionContentExamPracticeAttempt(260L, 360L, 560L, 2L);

        assertEquals(9001L, response.attemptId());
        verify(examService).upsertExamMembership(
                eq(fixture.exam),
                eq(participant),
                eq("viewer"),
                eq(Boolean.FALSE),
                eq(Boolean.FALSE),
                eq(Boolean.FALSE));
        verify(examService).startPracticeAttempt(fixture.exam.getId(), 2L);
    }

    @Test
    void joinCourseSessionContentGroupPracticeParticipantDelegatesWithoutReadOnlyFailure() {
        AnchoredExamFixture fixture = buildAnchoredExamFixture(170L, "exam");
        User participant = buildParticipantUser(2L);
        CourseMembership membership = buildParticipantMembership(fixture.course, participant, "viewer", 1002L);
        ExamGroupJoinRequest request = new ExamGroupJoinRequest(2L);
        ExamGroupStateResponse expected = emptyGroupState(fixture.exam.getId(), fixture.exam.getName());

        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(courseMembershipRepository.findByCourseIdAndUserIdAndDeletedAtIsNull(fixture.course.getId(), 2L))
                .thenReturn(Optional.of(membership));
        when(examGroupPracticeService.join(fixture.exam.getId(), request)).thenReturn(expected);

        ExamGroupStateResponse response =
                courseService.joinCourseSessionContentGroupPractice(270L, 370L, 570L, request);

        assertEquals(fixture.exam.getId(), response.examId());
        verify(examService).upsertExamMembership(
                eq(fixture.exam),
                eq(participant),
                eq("viewer"),
                eq(Boolean.FALSE),
                eq(Boolean.FALSE),
                eq(Boolean.FALSE));
        verify(examGroupPracticeService).join(fixture.exam.getId(), request);
    }

    @Test
    void createCourseSessionContentGroupPracticeParticipantDelegatesWithoutReadOnlyFailure() {
        AnchoredExamFixture fixture = buildAnchoredExamFixture(180L, "exam");
        User participant = buildParticipantUser(2L);
        CourseMembership membership = buildParticipantMembership(fixture.course, participant, "viewer", 1003L);
        ExamGroupJoinRequest request = new ExamGroupJoinRequest(2L);
        ExamGroupStateResponse expected = emptyGroupState(fixture.exam.getId(), fixture.exam.getName());

        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(courseMembershipRepository.findByCourseIdAndUserIdAndDeletedAtIsNull(fixture.course.getId(), 2L))
                .thenReturn(Optional.of(membership));
        when(examGroupPracticeService.create(fixture.exam.getId(), request)).thenReturn(expected);

        ExamGroupStateResponse response =
                courseService.createCourseSessionContentGroupPractice(280L, 380L, 580L, request);

        assertEquals(fixture.exam.getId(), response.examId());
        verify(examService).upsertExamMembership(
                eq(fixture.exam),
                eq(participant),
                eq("viewer"),
                eq(Boolean.FALSE),
                eq(Boolean.FALSE),
                eq(Boolean.FALSE));
        verify(examGroupPracticeService).create(fixture.exam.getId(), request);
    }

    private AnchoredExamFixture buildAnchoredExamFixture(Long baseId, String contentType) {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso anclado");
        course.setVisibility("private");
        setBaseFields(course, baseId + 100L);

        CourseSession session = new CourseSession();
        session.setCourse(course);
        session.setName("Sesion anclada");
        setBaseFields(session, baseId + 200L);

        Exam exam = new Exam();
        exam.setUser(owner);
        exam.setName("Examen anclado");
        exam.setQuestionsCount(10);
        setBaseFields(exam, baseId + 300L);

        CourseSessionContent content = new CourseSessionContent();
        content.setCourseSession(session);
        content.setType(contentType);
        content.setSourceExam(exam);
        setBaseFields(content, baseId + 400L);

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(courseSessionContentRepository.findByIdAndCourseSessionIdAndDeletedAtIsNull(content.getId(), session.getId()))
                .thenReturn(Optional.of(content));

        return new AnchoredExamFixture(course, session, exam, content);
    }

        private User buildParticipantUser(Long id) {
                User participant = new User();
                participant.setName("Participant");
                participant.setUsername("participant");
                participant.setEmail("participant@mail.com");
                setBaseFields(participant, id);
                return participant;
        }

        private CourseMembership buildParticipantMembership(
                        Course course,
                        User participant,
                        String role,
                        Long membershipId) {
                CourseMembership membership = new CourseMembership();
                membership.setCourse(course);
                membership.setUser(participant);
                membership.setRole(role);
                setBaseFields(membership, membershipId);
                return membership;
        }

        private ExamGroupStateResponse emptyGroupState(Long examId, String examName) {
                return new ExamGroupStateResponse(
                                1L,
                                examId,
                                examName,
                                "waiting",
                                0,
                                0,
                                Boolean.FALSE,
                                Boolean.FALSE,
                                null,
                                Collections.emptyList(),
                                Collections.emptyList(),
                                Collections.emptyList(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                1,
                                Boolean.FALSE,
                                Boolean.FALSE,
                                0,
                                null,
                                null,
                                null);
        }

    private record AnchoredExamFixture(Course course, CourseSession session, Exam exam, CourseSessionContent content) {}

    private static void setBaseFields(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "createdAt", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(target, "updatedAt", LocalDateTime.now());
    }
}
