package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.bardales.SmartLearnApi.domain.repository.ExamAttemptRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.course.CourseCompetencySaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseModuleResponse;
import com.bardales.SmartLearnApi.dto.course.CourseParticipantSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseResponse;
import java.time.LocalDateTime;
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
    private CourseCompetencyRepository courseCompetencyRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExamService examService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                courseMembershipRepository,
                courseExamRepository,
                courseSessionRepository,
                courseSessionContentRepository,
                courseCompetencyRepository,
                examAttemptRepository,
                examRepository,
                userRepository,
                examService);
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

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(courseRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L)).thenReturn(List.of(course));
        when(courseMembershipRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(courseRepository.findByVisibilityIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc("public"))
                .thenReturn(List.of());
        when(examRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        when(courseSessionRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(10L)).thenReturn(List.of(session));
        when(courseSessionContentRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(12L))
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

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
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

    private static void setBaseFields(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "createdAt", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(target, "updatedAt", LocalDateTime.now());
    }
}

