package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupRoomSession;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSession;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionEvent;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionMember;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionRound;
import com.bardales.SmartLearnApi.domain.entity.ExamMembership;
import com.bardales.SmartLearnApi.domain.entity.Option;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionAnswer;
import com.bardales.SmartLearnApi.domain.entity.Question;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionAnswerRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionMemberRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupRoomSessionRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionEventRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionRoundRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.OptionRepository;
import com.bardales.SmartLearnApi.domain.repository.QuestionRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStateResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAdvanceRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAnswerRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
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
    private ExamGroupRoomSessionRepository examGroupRoomSessionRepository;
    @Mock
    private ExamGroupSessionAnswerRepository examGroupSessionAnswerRepository;
    @Mock
    private ExamGroupSessionRoundRepository examGroupSessionRoundRepository;
    @Mock
    private ExamGroupSessionEventRepository examGroupSessionEventRepository;

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
                examGroupRoomSessionRepository,
                examGroupSessionAnswerRepository,
                examGroupSessionRoundRepository,
                examGroupSessionEventRepository);

        when(examGroupSessionRepository.save(any(ExamGroupSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(examGroupRoomSessionRepository.save(any(ExamGroupRoomSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(examGroupRoomSessionRepository
                .findTopBySessionIdAndUserIdAndDeletedAtIsNullAndRevokedAtIsNullOrderByIdDesc(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(examGroupRoomSessionRepository
                .findTopBySessionIdAndRoomTokenAndDeletedAtIsNullAndRevokedAtIsNullOrderByIdDesc(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(examGroupRoomSessionRepository.revokeActiveBySessionId(anyLong(), any())).thenReturn(0);
        when(examGroupSessionRoundRepository.findBySession_IdAndRoundNumberAndDeletedAtIsNull(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(examGroupSessionRoundRepository.save(any(ExamGroupSessionRound.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(examGroupSessionEventRepository.save(any(ExamGroupSessionEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(examGroupSessionAnswerRepository.findForQuestionRound(anyLong(), anyLong(), any())).thenReturn(List.of());
        when(examGroupSessionAnswerRepository.findAllForUserRound(anyLong(), anyLong(), any())).thenReturn(List.of());
        when(optionRepository.findByQuestionIdOrderByIdAsc(anyLong())).thenReturn(List.of());
    }

    @Test
    void closeRevokesActiveRoomSessions() {
        Fixture f = fixtureReviewSession(false);

        service.close(
                f.exam.getId(),
                new ExamGroupAdvanceRequest(f.owner.getId(), f.session.getId(), null));

        verify(examGroupRoomSessionRepository, times(1))
                .revokeActiveBySessionId(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void reviewTimeoutAdvancesToNextQuestionWhenNotLast() {
        Fixture f = fixtureReviewSession(false);

        Question q1 = question(101L, f.exam, 30, 10);
        Question q2 = question(102L, f.exam, 30, 10);

        when(questionRepository.findById(101L)).thenReturn(Optional.of(q1));
        when(questionRepository.findById(102L)).thenReturn(Optional.of(q2));
        when(optionRepository.findByQuestionIdOrderByIdAsc(102L)).thenReturn(List.of());

        ExamGroupStateResponse state = service.state(f.exam.getId(), f.session.getId(), f.owner.getId(), null);

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

        ExamGroupStateResponse state = service.state(f.exam.getId(), f.session.getId(), f.owner.getId(), null);

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

        ExamGroupStateResponse state = service.state(f.exam.getId(), f.session.getId(), f.owner.getId(), null);

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

        ExamGroupStateResponse firstPoll = service.state(f.exam.getId(), f.session.getId(), f.owner.getId(), null);
        ExamGroupStateResponse secondPoll = service.state(f.exam.getId(), f.session.getId(), secondUser.getId(), null);

        assertEquals(1, firstPoll.currentQuestionIndex());
        assertEquals(firstPoll.currentQuestionIndex(), secondPoll.currentQuestionIndex());
        assertEquals(firstPoll.phase(), secondPoll.phase());
        verify(examGroupSessionRepository, atLeastOnce()).save(any(ExamGroupSession.class));
    }

    @Test
    void joinHandlesConcurrentMemberInsertWithoutInternalError() {
        User owner = user(1L, "Owner", "owner");
        User participant = user(2L, "Participant", "participant");
        Exam exam = exam(900L, owner, "Exam concurrente");
        ExamMembership membership = new ExamMembership();
        membership.setExam(exam);
        membership.setUser(participant);
        membership.setRole("viewer");
        membership.setCanStartGroup(Boolean.FALSE);
        setBaseFields(membership, 901L);

        ExamGroupSession session = groupSession(910L, exam, owner, "waiting", null, 0, 0);
        session.setStartedAt(null);
        session.setCurrentQuestionStartedAt(null);
        session.setPhase("open");
        session.setPhaseStartedAt(null);
        session.setPhaseEndsAt(null);

        ExamGroupSessionMember ownerMember = sessionMember(session, owner, true);
        ExamGroupSessionMember participantMember = sessionMember(session, participant, true);

        when(userRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(examRepository.findByIdAndDeletedAtIsNull(exam.getId())).thenReturn(Optional.of(exam));
        when(examMembershipRepository.findByExamIdAndUserIdAndDeletedAtIsNull(exam.getId(), participant.getId()))
                .thenReturn(Optional.of(membership));
        when(examGroupSessionRepository
                .findTopByExamIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(exam.getId(), List.of("waiting", "active")))
                .thenReturn(Optional.of(session));
        when(examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(session.getId()))
                .thenReturn(List.of(ownerMember, participantMember));
        when(examGroupSessionMemberRepository
                .findBySessionIdAndUserIdAndDeletedAtIsNull(session.getId(), participant.getId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(participantMember));
        when(examMembershipRepository.findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(exam.getId()))
                .thenReturn(List.of(membership));

        when(examGroupSessionMemberRepository.save(any(ExamGroupSessionMember.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate member"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExamGroupStateResponse state = assertDoesNotThrow(
                () -> service.join(
                        exam.getId(),
                        new com.bardales.SmartLearnApi.dto.exam.ExamGroupJoinRequest(participant.getId(), null)));

        assertEquals("waiting", state.status());
        assertEquals(session.getId(), state.sessionId());
    }

    @Test
    void creatorAnswerDoesNotForceReviewWhenParticipantHeartbeatIsActive() {
        User owner = user(1L, "Owner", "owner");
        User participant = user(2L, "Participant", "participant");
        Exam exam = exam(920L, owner, "Exam heartbeat");

        ExamMembership participantMembership = new ExamMembership();
        participantMembership.setExam(exam);
        participantMembership.setUser(participant);
        participantMembership.setRole("viewer");
        participantMembership.setCanStartGroup(Boolean.FALSE);
        setBaseFields(participantMembership, 921L);

        ExamGroupSession session = groupSession(930L, exam, owner, "active", "101", 1, 0);
        session.setPhase("open");
        session.setPhaseStartedAt(LocalDateTime.now().minusSeconds(5));
        session.setPhaseEndsAt(LocalDateTime.now().plusSeconds(30));
        session.setCurrentQuestionStartedAt(LocalDateTime.now().minusSeconds(5));

        ExamGroupSessionMember ownerMember = sessionMember(session, owner, true);
        ExamGroupSessionMember participantMember = sessionMember(session, participant, false);
        participantMember.setLastSeenAt(LocalDateTime.now().minusSeconds(60));

        Question q1 = question(101L, exam, 30, 10);
        q1.setCorrectAnswer("git diff");
        Option optionA = new Option();
        optionA.setQuestion(q1);
        optionA.setOptionText("git diff");
        optionA.setIsCorrect(Boolean.TRUE);
        setBaseFields(optionA, 2001L);

        List<ExamGroupSessionAnswer> storedAnswers = new ArrayList<>();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(userRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(examRepository.findByIdAndDeletedAtIsNull(exam.getId())).thenReturn(Optional.of(exam));
        when(examMembershipRepository.findByExamIdAndUserIdAndDeletedAtIsNull(exam.getId(), participant.getId()))
                .thenReturn(Optional.of(participantMembership));
        when(examGroupSessionRepository.findByIdAndExamIdAndDeletedAtIsNull(session.getId(), exam.getId()))
                .thenReturn(Optional.of(session));
        when(examGroupSessionMemberRepository
                .findBySessionIdAndUserIdAndDeletedAtIsNull(session.getId(), owner.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(examGroupSessionMemberRepository
                .findBySessionIdAndUserIdAndDeletedAtIsNull(session.getId(), participant.getId()))
                .thenReturn(Optional.of(participantMember));
        when(examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(session.getId()))
                .thenReturn(List.of(ownerMember, participantMember));
        when(examMembershipRepository.findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(exam.getId()))
                .thenReturn(List.of(participantMembership));

        when(questionRepository.findById(q1.getId())).thenReturn(Optional.of(q1));
        when(optionRepository.findByQuestionIdOrderByIdAsc(q1.getId())).thenReturn(List.of(optionA));

        when(examGroupSessionMemberRepository.save(any(ExamGroupSessionMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(examGroupSessionAnswerRepository.findAllForUserRound(session.getId(), owner.getId(), 1))
                .thenAnswer(invocation -> storedAnswers.stream()
                        .filter(answer -> answer.getUser() != null && owner.getId().equals(answer.getUser().getId()))
                        .toList());
        when(examGroupSessionAnswerRepository.findForQuestionRound(session.getId(), q1.getId(), 1))
                .thenAnswer(invocation -> List.copyOf(storedAnswers));
        when(examGroupSessionAnswerRepository.save(any(ExamGroupSessionAnswer.class)))
                .thenAnswer(invocation -> {
                    ExamGroupSessionAnswer answer = invocation.getArgument(0);
                    if (answer.getId() == null) {
                        setBaseFields(answer, 3001L + storedAnswers.size());
                    }
                    storedAnswers.removeIf(existing -> existing.getId() != null
                            && answer.getId() != null
                            && existing.getId().equals(answer.getId()));
                    storedAnswers.add(answer);
                    return answer;
                });

        // Heartbeat de participante: debe reconectarlo antes de que responda el owner.
        ExamGroupStateResponse participantState = service.state(exam.getId(), session.getId(), participant.getId(), null);
        assertEquals("open", participantState.phase());

        ExamGroupStateResponse stateAfterOwnerAnswer = service.answer(
                exam.getId(),
                new ExamGroupAnswerRequest(owner.getId(), session.getId(), q1.getId(), 1, "a", null, null));

        assertEquals("open", stateAfterOwnerAnswer.phase());
        assertEquals("active", stateAfterOwnerAnswer.status());
    }

    @Test
    void creatorAnswerDoesNotForceReviewWhenAnotherMemberHasNotAnswered() {
        User owner = user(1L, "Owner", "owner");
        User participant = user(2L, "Participant", "participant");
        Exam exam = exam(940L, owner, "Exam all members rule");

        ExamMembership participantMembership = new ExamMembership();
        participantMembership.setExam(exam);
        participantMembership.setUser(participant);
        participantMembership.setRole("viewer");
        participantMembership.setCanStartGroup(Boolean.FALSE);
        setBaseFields(participantMembership, 941L);

        ExamGroupSession session = groupSession(950L, exam, owner, "active", "101", 1, 0);
        session.setPhase("open");
        session.setPhaseStartedAt(LocalDateTime.now().minusSeconds(3));
        session.setPhaseEndsAt(LocalDateTime.now().plusSeconds(25));
        session.setCurrentQuestionStartedAt(LocalDateTime.now().minusSeconds(3));

        ExamGroupSessionMember ownerMember = sessionMember(session, owner, true);
        ExamGroupSessionMember participantMember = sessionMember(session, participant, false);
        participantMember.setLastSeenAt(LocalDateTime.now().minusMinutes(2));

        Question q1 = question(101L, exam, 30, 10);
        q1.setCorrectAnswer("git diff");
        Option optionA = new Option();
        optionA.setQuestion(q1);
        optionA.setOptionText("git diff");
        optionA.setIsCorrect(Boolean.TRUE);
        setBaseFields(optionA, 2101L);

        List<ExamGroupSessionAnswer> storedAnswers = new ArrayList<>();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(examRepository.findByIdAndDeletedAtIsNull(exam.getId())).thenReturn(Optional.of(exam));
        when(examGroupSessionRepository.findByIdAndExamIdAndDeletedAtIsNull(session.getId(), exam.getId()))
                .thenReturn(Optional.of(session));
        when(examMembershipRepository.findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(exam.getId()))
                .thenReturn(List.of(participantMembership));
        when(examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(session.getId()))
                .thenReturn(List.of(ownerMember, participantMember));
        when(examGroupSessionMemberRepository.findBySessionIdAndUserIdAndDeletedAtIsNull(session.getId(), owner.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(examGroupSessionMemberRepository.save(any(ExamGroupSessionMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(questionRepository.findById(q1.getId())).thenReturn(Optional.of(q1));
        when(optionRepository.findByQuestionIdOrderByIdAsc(q1.getId())).thenReturn(List.of(optionA));

        when(examGroupSessionAnswerRepository.findAllForUserRound(session.getId(), owner.getId(), 1))
                .thenAnswer(invocation -> storedAnswers.stream()
                        .filter(answer -> answer.getUser() != null && owner.getId().equals(answer.getUser().getId()))
                        .toList());
        when(examGroupSessionAnswerRepository.findForQuestionRound(session.getId(), q1.getId(), 1))
                .thenAnswer(invocation -> List.copyOf(storedAnswers));
        when(examGroupSessionAnswerRepository.save(any(ExamGroupSessionAnswer.class)))
                .thenAnswer(invocation -> {
                    ExamGroupSessionAnswer answer = invocation.getArgument(0);
                    if (answer.getId() == null) {
                        setBaseFields(answer, 3201L + storedAnswers.size());
                    }
                    storedAnswers.removeIf(existing -> existing.getId() != null
                            && answer.getId() != null
                            && existing.getId().equals(answer.getId()));
                    storedAnswers.add(answer);
                    return answer;
                });

        ExamGroupStateResponse stateAfterOwnerAnswer = service.answer(
                exam.getId(),
                new ExamGroupAnswerRequest(owner.getId(), session.getId(), q1.getId(), 1, "a", null, null));

        assertEquals("open", stateAfterOwnerAnswer.phase());
        assertEquals("active", stateAfterOwnerAnswer.status());
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
