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
import com.bardales.SmartLearnApi.domain.entity.CourseWeek;
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
import com.bardales.SmartLearnApi.dto.course.CourseCreateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseModuleResponse;
import com.bardales.SmartLearnApi.dto.course.CourseParticipantSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseResponse;
import com.bardales.SmartLearnApi.dto.course.CourseSessionCreateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseSessionContentPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.course.CourseWeekSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseUpdateRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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

        @Mock
        private CoursePracticeWriteService coursePracticeWriteService;

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
                examGroupPracticeService,
                coursePracticeWriteService);
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
    void createCourseForcesJoinModeOpenWhenVisibilityIsPrivate() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull("PRIV-101")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            setBaseFields(saved, 801L);
            return saved;
        });
        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(801L)).thenReturn(List.of());
        when(courseExamRepository.findByCourseIdOrderByCreatedAtAsc(801L)).thenReturn(List.of());
        when(courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(801L)).thenReturn(List.of());
        when(courseCompetencyRepository.findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(801L))
                .thenReturn(List.of());

        courseService.createCourse(new CourseCreateRequest(
                1L,
                "Curso privado",
                null,
                null,
                "PRIV-101",
                "private",
                "request",
                null,
                null));

        ArgumentCaptor<Course> savedCourseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(savedCourseCaptor.capture());
        assertEquals("private", savedCourseCaptor.getValue().getVisibility());
        assertEquals("open", savedCourseCaptor.getValue().getJoinMode());
    }

    @Test
    void updateCourseForcesJoinModeOpenWhenVisibilityChangesToPrivate() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso publico");
        course.setVisibility("public");
        course.setJoinMode("request");
        course.setPriority("important");
        course.setSortOrder(0);
        course.setCode("PUB-101");
        setBaseFields(course, 802L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseRepository.findByIdAndUserIdAndDeletedAtIsNull(802L, 1L)).thenReturn(Optional.of(course));
        when(courseRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot("PUB-101", 802L)).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(802L)).thenReturn(List.of());
        when(courseExamRepository.findByCourseIdOrderByCreatedAtAsc(802L)).thenReturn(List.of());
        when(courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(802L)).thenReturn(List.of());
        when(courseCompetencyRepository.findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(802L))
                .thenReturn(List.of());

        courseService.updateCourse(
                802L,
                new CourseUpdateRequest(
                        1L,
                        "Curso privado",
                        null,
                        null,
                        "PUB-101",
                        "private",
                        "request",
                        "important",
                        0));

        assertEquals("private", course.getVisibility());
        assertEquals("open", course.getJoinMode());
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
        verify(coursePracticeWriteService, times(1))
                .ensureParticipantAnchoredExamMembership(eq(fixture.exam), eq(2L));
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
        when(coursePracticeWriteService.startAnchoredExamPracticeAttempt(fixture.exam, 2L)).thenReturn(expected);

        ExamPracticeStartResponse response =
                courseService.startCourseSessionContentExamPracticeAttempt(260L, 360L, 560L, 2L);

        assertEquals(9001L, response.attemptId());
        verify(coursePracticeWriteService)
                .startAnchoredExamPracticeAttempt(eq(fixture.exam), eq(2L));
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
        when(coursePracticeWriteService.joinAnchoredGroupPractice(fixture.exam, request)).thenReturn(expected);

        ExamGroupStateResponse response =
                courseService.joinCourseSessionContentGroupPractice(270L, 370L, 570L, request);

        assertEquals(fixture.exam.getId(), response.examId());
        verify(coursePracticeWriteService)
                .joinAnchoredGroupPractice(eq(fixture.exam), eq(request));
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
        when(coursePracticeWriteService.createAnchoredGroupPractice(fixture.exam, request)).thenReturn(expected);

        ExamGroupStateResponse response =
                courseService.createCourseSessionContentGroupPractice(280L, 380L, 580L, request);

        assertEquals(fixture.exam.getId(), response.examId());
        verify(coursePracticeWriteService)
                .createAnchoredGroupPractice(eq(fixture.exam), eq(request));
    }

    @Test
    void createCourseSessionDoesNotCreateDefaultWeekAutomatically() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso");
        course.setVisibility("private");
        course.setPriority("important");
        course.setSortOrder(0);
        setBaseFields(course, 100L);

        CourseSession savedSession = new CourseSession();
        savedSession.setCourse(course);
        savedSession.setName("SESION 1: Intro");
        savedSession.setWeeklyContent(null);
        setBaseFields(savedSession, 200L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseRepository.findByIdAndUserIdAndDeletedAtIsNull(100L, 1L)).thenReturn(Optional.of(course));
        when(courseSessionRepository.save(any(CourseSession.class))).thenAnswer(invocation -> savedSession);
        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(100L)).thenReturn(List.of(savedSession));
        when(courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(200L))
                .thenReturn(List.of());
        when(courseSessionContentRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(200L))
                .thenReturn(List.of());
        when(courseExamRepository.findByCourseIdOrderByCreatedAtAsc(100L)).thenReturn(List.of());
        when(courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(100L)).thenReturn(List.of());
        when(courseCompetencyRepository.findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(100L))
                .thenReturn(List.of());

        CourseResponse response =
                courseService.createCourseSession(100L, new CourseSessionCreateRequest(1L, "SESION 1: Intro", null));

        assertEquals(1, response.sessions().size());
        verify(courseWeekRepository, never()).save(any(CourseWeek.class));
    }

    @Test
    void addCourseWeekReturnsBadRequestWhenWeekOrderConflictsAtPersistence() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso");
        course.setVisibility("private");
        setBaseFields(course, 10L);

        CourseSession session = new CourseSession();
        session.setCourse(course);
        session.setName("SESION 1: Inicio");
        setBaseFields(session, 20L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseSessionRepository.findByIdAndCourseUserIdAndDeletedAtIsNull(20L, 1L)).thenReturn(Optional.of(session));
        when(courseWeekRepository.findMaxWeekOrderByCourseSessionId(20L)).thenReturn(0);
        when(courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(20L))
                .thenReturn(List.of());
        when(courseWeekRepository.save(any(CourseWeek.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> courseService.addCourseWeek(10L, 20L, new CourseWeekSaveRequest(1L, null, null, 1)));

        assertEquals("weekOrder ya existe en esta sesion", exception.getMessage());
    }

    @Test
    void addCourseWeekUsesNextOrderWhenRequestedOrderAlreadyExists() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso");
        course.setVisibility("private");
        setBaseFields(course, 70L);

        CourseSession session = new CourseSession();
        session.setCourse(course);
        session.setName("SESION 1: Inicio");
        setBaseFields(session, 80L);

        CourseWeek existingWeek = new CourseWeek();
        existingWeek.setCourseSession(session);
        existingWeek.setWeekOrder(1);
        existingWeek.setName("SEMANA 1: Inicio");
        setBaseFields(existingWeek, 81L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseSessionRepository.findByIdAndCourseUserIdAndDeletedAtIsNull(80L, 1L)).thenReturn(Optional.of(session));
                when(courseWeekRepository.findMaxWeekOrderByCourseSessionId(80L)).thenReturn(2);
        when(courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(80L))
                .thenAnswer(invocation -> List.of(existingWeek));
                when(courseWeekRepository.save(any(CourseWeek.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(70L)).thenReturn(List.of(session));
        when(courseSessionContentRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(80L))
                .thenReturn(List.of());
        when(courseExamRepository.findByCourseIdOrderByCreatedAtAsc(70L)).thenReturn(List.of());
        when(courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(70L)).thenReturn(List.of());
        when(courseCompetencyRepository.findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(70L))
                .thenReturn(List.of());

        courseService.addCourseWeek(70L, 80L, new CourseWeekSaveRequest(1L, null, null, 1));

        ArgumentCaptor<CourseWeek> savedWeekCaptor = ArgumentCaptor.forClass(CourseWeek.class);
                verify(courseWeekRepository, times(1)).save(savedWeekCaptor.capture());
                assertEquals(3, savedWeekCaptor.getValue().getWeekOrder());
    }

    @Test
    void updateCourseWeekAllowsClearingDescriptionWhenValueIsNull() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso");
        course.setVisibility("private");
        setBaseFields(course, 90L);

        CourseSession session = new CourseSession();
        session.setCourse(course);
        session.setName("SESION 1: Inicio");
        setBaseFields(session, 91L);

        CourseWeek week = new CourseWeek();
        week.setCourseSession(session);
        week.setWeekOrder(2);
        week.setName("SEMANA 2: Practica");
        week.setDescription("Texto previo");
        setBaseFields(week, 92L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseSessionRepository.findByIdAndCourseUserIdAndDeletedAtIsNull(91L, 1L)).thenReturn(Optional.of(session));
        when(courseWeekRepository.findByIdAndCourseSessionIdAndDeletedAtIsNull(92L, 91L)).thenReturn(Optional.of(week));
        when(courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(91L))
                .thenReturn(List.of(week));
        when(courseWeekRepository.save(any(CourseWeek.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(90L)).thenReturn(List.of(session));
        when(courseSessionContentRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(91L))
                .thenReturn(List.of());
        when(courseExamRepository.findByCourseIdOrderByCreatedAtAsc(90L)).thenReturn(List.of());
        when(courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(90L)).thenReturn(List.of());
        when(courseCompetencyRepository.findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(90L))
                .thenReturn(List.of());

        courseService.updateCourseWeek(90L, 91L, 92L, new CourseWeekSaveRequest(1L, "SEMANA 2: Practica", null, 2));

        ArgumentCaptor<CourseWeek> updatedWeekCaptor = ArgumentCaptor.forClass(CourseWeek.class);
        verify(courseWeekRepository, times(1)).save(updatedWeekCaptor.capture());
        assertNull(updatedWeekCaptor.getValue().getDescription());
    }

    @Test
    void updateCourseWeekReturnsBadRequestWhenWeekOrderConflictsAtPersistence() {
        User owner = new User();
        owner.setName("Owner");
        owner.setUsername("owner");
        owner.setEmail("owner@mail.com");
        setBaseFields(owner, 1L);

        Course course = new Course();
        course.setUser(owner);
        course.setName("Curso");
        course.setVisibility("private");
        setBaseFields(course, 30L);

        CourseSession session = new CourseSession();
        session.setCourse(course);
        session.setName("SESION 1: Inicio");
        setBaseFields(session, 40L);

        CourseWeek week = new CourseWeek();
        week.setCourseSession(session);
        week.setWeekOrder(1);
        week.setName("SEMANA 1: Inicio");
        setBaseFields(week, 50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseSessionRepository.findByIdAndCourseUserIdAndDeletedAtIsNull(40L, 1L)).thenReturn(Optional.of(session));
        when(courseWeekRepository.findByIdAndCourseSessionIdAndDeletedAtIsNull(50L, 40L)).thenReturn(Optional.of(week));
        when(courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(40L))
                .thenReturn(List.of(week));
        when(courseWeekRepository.save(any(CourseWeek.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> courseService.updateCourseWeek(30L, 40L, 50L, new CourseWeekSaveRequest(1L, "Semana", null, 2)));

        assertEquals("weekOrder ya existe en esta sesion", exception.getMessage());
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
