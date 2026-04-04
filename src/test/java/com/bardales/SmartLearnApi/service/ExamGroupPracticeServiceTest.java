package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSession;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionMember;
import com.bardales.SmartLearnApi.domain.entity.ExamMembership;
import com.bardales.SmartLearnApi.domain.entity.Question;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionAnswerRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionMemberRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.OptionRepository;
import com.bardales.SmartLearnApi.domain.repository.QuestionRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStateResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamGroupPracticeServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExamMembershipRepository examMembershipRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private OptionRepository optionRepository;
    @Mock
    private ExamGroupSessionRepository examGroupSessionRepository;
    @Mock
    private ExamGroupSessionMemberRepository examGroupSessionMemberRepository;
    @Mock
    private ExamGroupSessionAnswerRepository examGroupSessionAnswerRepository;

    private ExamGroupPracticeService service;

    @BeforeEach
    void setUp() {
        service = new ExamGroupPracticeService(
                examRepository,
                userRepository,
                examMembershipRepository,
                questionRepository,
                optionRepository,
                examGroupSessionRepository,
                examGroupSessionMemberRepository,
                examGroupSessionAnswerRepository);

        when(examGroupSessionRepository.save(any(ExamGroupSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(examGroupSessionAnswerRepository.findForQuestion(anyLong(), anyLong())).thenReturn(List.of());
        when(optionRepository.findByQuestionIdOrderByIdAsc(anyLong())).thenReturn(List.of());
    }

    @Test
    void reviewTimeoutAdvancesToNextQuestionWhenNotLast() {
        Fixture f = fixtureReviewSession(false);

        Question q1 = question(101L, f.exam, 30, 10);
        Question q2 = question(102L, f.exam, 30, 10);

        when(questionRepository.findById(101L)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(102L)).thenReturn(Optional.of(q2));
        when(optionRepository.findByQuestionIdOrderByIdAsc(102L)).thenReturn(List.of());

        ExamGroupStateResponse state = service.state(f.exam.getId(), f.session.getId(), f.owner.getId());

        assertEquals("active", state.status());
        assertEquals(1, state.currentQuestionIndex());
        assertEquals("open", state.phase());
        assertNotNull(state.currentQuestion());
        assertEquals(102L, state.currentQuestion().id());
        verify(examGroupSessionRepository, atLeastOnce()).save(any(ExamGroupSession.class));
    }

    @Test
    void reviewTimeoutFinishesSessionWhenLastQuestion() {
        Fixture f = fixtureReviewSession(true);

        Question q1 = question(101L, f.exam, 30, 10);
        when(questionRepository.findById(101L)).thenReturn(Optional.of(q1));

        ExamGroupStateResponse state = service.state(f.exam.getId(), f.session.getId(), f.owner.getId());

        assertEquals("finished", state.status());
        assertEquals("open", state.phase());
        verify(examGroupSessionRepository, atLeastOnce()).save(any(ExamGroupSession.class));
    }

    @Test
    void reviewTimeoutAdvancesEvenWhenParticipantsHaveNoAnswers() {
        Fixture f = fixtureReviewSession(false);
        User secondUser = user(2L, "Student", "student");
        ExamGroupSessionMember secondMember = sessionMember(f.session, secondUser, true);

        when(userRepository.findById(secondUser.getId())).thenReturn(Optional.of(secondUser));

        Question q1 = question(101L, f.exam, 30, 10);
        Question q2 = question(102L, f.exam, 30, 10);
        when(questionRepository.findById(101L)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(102L)).thenReturn(Optional.of(q2));
        when(optionRepository.findByQuestionIdOrderByIdAsc(102L)).thenReturn(List.of());

        when(examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(f.session.getId()))
                .thenReturn(List.of(f.ownerMember, secondMember));
        when(examGroupSessionMemberRepository
                .findBySessionIdAndUserIdAndDeletedAtIsNull(f.session.getId(), secondUser.getId()))
                .thenReturn(Optional.of(secondMember));

        ExamMembership secondMembership = new ExamMembership();
        secondMembership.setExam(f.exam);
        secondMembership.setUser(secondUser);
        secondMembership.setRole("viewer");
        secondMembership.setCanStartGroup(Boolean.FALSE);
        setBaseFields(secondMembership, 501L);

        when(examMembershipRepository.findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(f.exam.getId()))
                .thenReturn(List.of(secondMembership));

        ExamGroupStateResponse state = service.state(f.exam.getId(), f.session.getId(), f.owner.getId());

        assertEquals(1, state.currentQuestionIndex());
        assertEquals("open", state.phase());
        verify(examGroupSessionRepository, atLeastOnce()).save(any(ExamGroupSession.class));
    }

    @Test
    void pollingUsersSeeSameIndexAfterReviewTimeoutAdvance() {
        Fixture f = fixtureReviewSession(false);
        User secondUser = user(2L, "Student", "student");
        ExamGroupSessionMember secondMember = sessionMember(f.session, secondUser, true);

        when(userRepository.findById(secondUser.getId())).thenReturn(Optional.of(secondUser));

        Question q1 = question(101L, f.exam, 30, 10);
        Question q2 = question(102L, f.exam, 30, 10);
        when(questionRepository.findById(101L)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(102L)).thenReturn(Optional.of(q2));
        when(optionRepository.findByQuestionIdOrderByIdAsc(102L)).thenReturn(List.of());

        when(examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(f.session.getId()))
                .thenReturn(List.of(f.ownerMember, secondMember));
        when(examGroupSessionMemberRepository
                .findBySessionIdAndUserIdAndDeletedAtIsNull(f.session.getId(), secondUser.getId()))
                .thenReturn(Optional.of(secondMember));

        ExamMembership secondMembership = new ExamMembership();
        secondMembership.setExam(f.exam);
        secondMembership.setUser(secondUser);
        secondMembership.setRole("viewer");
        secondMembership.setCanStartGroup(Boolean.FALSE);
        setBaseFields(secondMembership, 502L);

        when(examMembershipRepository.findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(f.exam.getId()))
                .thenReturn(List.of(secondMembership));
        when(examMembershipRepository.findByExamIdAndUserIdAndDeletedAtIsNull(f.exam.getId(), secondUser.getId()))
                .thenReturn(Optional.of(secondMembership));

        ExamGroupStateResponse firstPoll = service.state(f.exam.getId(), f.session.getId(), f.owner.getId());
        ExamGroupStateResponse secondPoll = service.state(f.exam.getId(), f.session.getId(), secondUser.getId());

        assertEquals(1, firstPoll.currentQuestionIndex());
        assertEquals(firstPoll.currentQuestionIndex(), secondPoll.currentQuestionIndex());
        assertEquals(firstPoll.phase(), secondPoll.phase());
        verify(examGroupSessionRepository, atLeastOnce()).save(any(ExamGroupSession.class));
    }

    private Fixture fixtureReviewSession(boolean lastQuestion) {
        User owner = user(1L, "Owner", "owner");
        Exam exam = exam(800L, owner, "Exam");

        String questionIds = lastQuestion ? "101" : "101,102";
        int total = lastQuestion ? 1 : 2;
        ExamGroupSession session = groupSession(600L, exam, owner, "active", questionIds, total, 0);
        session.setPhase("review");
        session.setPhaseStartedAt(LocalDateTime.now().minusSeconds(20));
        session.setPhaseEndsAt(LocalDateTime.now().minusSeconds(1));
        session.setCurrentQuestionStartedAt(LocalDateTime.now().minusSeconds(40));

        ExamGroupSessionMember ownerMember = sessionMember(session, owner, true);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(examRepository.findByIdAndDeletedAtIsNull(exam.getId())).thenReturn(Optional.of(exam));
        when(examGroupSessionRepository.findByIdAndExamIdAndDeletedAtIsNull(session.getId(), exam.getId()))
                .thenReturn(Optional.of(session));
        when(examGroupSessionMemberRepository
                .findBySessionIdAndUserIdAndDeletedAtIsNull(session.getId(), owner.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(session.getId()))
                .thenReturn(List.of(ownerMember));
        when(examMembershipRepository.findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(exam.getId()))
                .thenReturn(List.of());

        return new Fixture(owner, exam, session, ownerMember);
    }

    private User user(Long id, String name, String username) {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setEmail(username + "@mail.com");
        setBaseFields(user, id);
        return user;
    }

    private Exam exam(Long id, User owner, String name) {
        Exam exam = new Exam();
        exam.setUser(owner);
        exam.setName(name);
        exam.setVisibility("private");
        exam.setPracticeOrderMode("ordered");
        exam.setPracticeFeedbackEnabled(Boolean.TRUE);
        exam.setPracticeRepeatUntilCorrect(Boolean.FALSE);
        setBaseFields(exam, id);
        return exam;
    }

    private Question question(Long id, Exam exam, int timerSeconds, int reviewSeconds) {
        Question question = new Question();
        question.setExam(exam);
        question.setQuestionText("Pregunta " + id);
        question.setQuestionType("multiple_choice");
        question.setCorrectAnswer("A");
        question.setPoints(1);
        question.setTemporizadorSegundos(timerSeconds);
        question.setReviewSeconds(reviewSeconds);
        question.setTimerEnabled(Boolean.TRUE);
        setBaseFields(question, id);
        return question;
    }

    private ExamGroupSession groupSession(
            Long id,
            Exam exam,
            User owner,
            String status,
            String questionIds,
            int totalQuestions,
            int currentIndex) {
        ExamGroupSession session = new ExamGroupSession();
        session.setExam(exam);
        session.setCreatedByUser(owner);
        session.setStatus(status);
        session.setOrderMode("ordered");
        session.setQuestionIds(questionIds);
        session.setTotalQuestions(totalQuestions);
        session.setCurrentQuestionIndex(currentIndex);
        session.setQuestionVersion(1);
        session.setStartedAt(LocalDateTime.now().minusMinutes(1));
        setBaseFields(session, id);
        return session;
    }

    private ExamGroupSessionMember sessionMember(ExamGroupSession session, User user, boolean connected) {
        ExamGroupSessionMember member = new ExamGroupSessionMember();
        member.setSession(session);
        member.setUser(user);
        member.setConnected(connected);
        member.setLastSeenAt(LocalDateTime.now());
        setBaseFields(member, (session.getId() * 10) + user.getId());
        return member;
    }

    private static void setBaseFields(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "createdAt", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(target, "updatedAt", LocalDateTime.now());
    }

    private record Fixture(User owner, Exam exam, ExamGroupSession session, ExamGroupSessionMember ownerMember) {}
}
