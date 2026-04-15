package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSession;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionAnswer;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionMember;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupRoomSession;
import com.bardales.SmartLearnApi.domain.entity.ExamMembership;
import com.bardales.SmartLearnApi.domain.entity.Option;
import com.bardales.SmartLearnApi.domain.entity.Question;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionAnswerRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionMemberRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupRoomSessionRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.OptionRepository;
import com.bardales.SmartLearnApi.domain.repository.QuestionRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAdvanceRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAnswerRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupCurrentAnswerResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupJoinRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupParticipantStateResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupRankingEntryResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStartRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStateResponse;
import com.bardales.SmartLearnApi.dto.exam.QuestionResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.ForbiddenException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import com.bardales.SmartLearnApi.exception.RoomSessionInvalidException;
import com.bardales.SmartLearnApi.security.JwtUserPrincipal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamGroupPracticeService {
    // Equilibrio entre evitar "usuarios fantasma" y tolerar jitter de red.
    private static final long MEMBER_PRESENCE_TIMEOUT_SECONDS = 30;
    private static final long ROOM_SESSION_TTL_MINUTES = 180;
    private static final String PHASE_OPEN = "open";
    private static final String PHASE_REVIEW = "review";
    private static final Logger log = LoggerFactory.getLogger(ExamGroupPracticeService.class);

    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final ExamMembershipRepository examMembershipRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ExamGroupSessionRepository examGroupSessionRepository;
    private final ExamGroupSessionMemberRepository examGroupSessionMemberRepository;
    private final ExamGroupRoomSessionRepository examGroupRoomSessionRepository;
    private final ExamGroupSessionAnswerRepository examGroupSessionAnswerRepository;

    public ExamGroupPracticeService(
            ExamRepository examRepository,
            UserRepository userRepository,
            ExamMembershipRepository examMembershipRepository,
            QuestionRepository questionRepository,
            OptionRepository optionRepository,
            ExamGroupSessionRepository examGroupSessionRepository,
            ExamGroupSessionMemberRepository examGroupSessionMemberRepository,
            ExamGroupRoomSessionRepository examGroupRoomSessionRepository,
            ExamGroupSessionAnswerRepository examGroupSessionAnswerRepository) {
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.examMembershipRepository = examMembershipRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.examGroupSessionRepository = examGroupSessionRepository;
        this.examGroupSessionMemberRepository = examGroupSessionMemberRepository;
        this.examGroupRoomSessionRepository = examGroupRoomSessionRepository;
        this.examGroupSessionAnswerRepository = examGroupSessionAnswerRepository;
    }

    @Transactional
    public ExamGroupStateResponse join(Long examId, ExamGroupJoinRequest request) {
        GroupAccess access = resolveGroupAccess(examId, request.userId());
        Exam exam = access.exam();

        if (!access.owner() && access.membership() == null) {
            ensureExamMembership(exam, access.user());
        }

        ExamGroupSession session = examGroupSessionRepository
                .findTopByExamIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(examId, List.of("waiting", "active"))
                .orElse(null);

        if (session == null) {
            throw new BadRequestException("No hay repaso grupal creado aun para este examen.");
        }
        session = refreshSessionPresence(session);
        if ("finished".equals(normalizeStatus(session.getStatus()))) {
            throw new BadRequestException("No hay repaso grupal creado aun para este examen.");
        }

        String requesterRoomSessionToken = ensureValidRoomSession(session, access.user(), request.roomSessionToken());
        session = refreshSessionPresence(session);
        session = syncSessionPhase(session);
        return toGroupState(session, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
    }

    @Transactional
    public ExamGroupStateResponse create(Long examId, ExamGroupJoinRequest request) {
        GroupAccess access = resolveGroupAccess(examId, request.userId());
        if (!access.canStartGroup()) {
            throw new BadRequestException("No tienes permiso para crear un repaso grupal.");
        }

        ExamGroupSession existing = examGroupSessionRepository
                .findTopByExamIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(examId, List.of("waiting", "active"))
                .orElse(null);
        if (existing != null) {
            existing = refreshSessionPresence(existing);
            existing = syncSessionPhase(existing);
            if ("finished".equals(normalizeStatus(existing.getStatus()))) {
                existing = null;
            }
        }
        if (existing != null) {
            Long existingCreatorId = existing.getCreatedByUser() == null ? null : existing.getCreatedByUser().getId();
            if (existingCreatorId != null && existingCreatorId.equals(access.user().getId())) {
                String requesterRoomSessionToken =
                        ensureValidRoomSession(existing, access.user(), request.roomSessionToken());
                existing = refreshSessionPresence(existing);
                return toGroupState(existing, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
            }
            throw new BadRequestException("Ya existe un repaso grupal creado para este examen. Debes unirte al existente.");
        }

        ExamGroupSession session = new ExamGroupSession();
        session.setExam(access.exam());
        session.setCreatedByUser(access.user());
        session.setStatus("waiting");
        session.setOrderMode("ordered");
        session.setQuestionIds(null);
        session.setTotalQuestions(0);
        session.setCurrentQuestionIndex(0);
        try {
            session = examGroupSessionRepository.save(session);
        } catch (DataIntegrityViolationException raceCondition) {
            // Refuerzo de concurrencia: si dos clientes crean simultáneamente,
            // recuperamos la sesión activa/espera que ganó la carrera.
            ExamGroupSession concurrent = examGroupSessionRepository
                    .findTopByExamIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(
                            examId, List.of("waiting", "active"))
                    .orElseThrow(() -> raceCondition);
            ensureSessionMember(concurrent, access.user());
            String requesterRoomSessionToken = ensureValidRoomSession(concurrent, access.user(), request.roomSessionToken());
            concurrent = refreshSessionPresence(concurrent);
            concurrent = syncSessionPhase(concurrent);

            Long existingCreatorId = concurrent.getCreatedByUser() == null ? null : concurrent.getCreatedByUser().getId();
            if (existingCreatorId != null && existingCreatorId.equals(access.user().getId())) {
                return toGroupState(concurrent, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
            }
            throw new BadRequestException("Ya existe un repaso grupal creado para este examen. Debes unirte al existente.");
        }

        String requesterRoomSessionToken = ensureValidRoomSession(session, access.user(), request.roomSessionToken());
        return toGroupState(session, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
    }

    @Transactional
    public ExamGroupStateResponse start(Long examId, ExamGroupStartRequest request) {
        GroupAccess access = resolveGroupAccess(examId, request.userId());
        if (!access.canStartGroup()) {
            throw new BadRequestException("No tienes permiso para iniciar el repaso grupal.");
        }

        ExamGroupSession session = requireSession(examId, request.sessionId());
        String requesterRoomSessionToken = ensureValidRoomSession(session, access.user(), request.roomSessionToken());
        session = refreshSessionPresence(session);

        String status = normalizeStatus(session.getStatus());
        if ("finished".equals(status)) {
            throw new BadRequestException("Esta sesion grupal ya finalizo.");
        }
        if ("active".equals(status)) {
            return toGroupState(session, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
        }

        List<ExamGroupSessionMember> members = examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                session.getId());
        long connectedCount = members.stream().filter(member -> Boolean.TRUE.equals(member.getConnected())).count();
        if (connectedCount < 2) {
            throw new BadRequestException("Se requieren al menos 2 participantes conectados para iniciar en grupo.");
        }

        List<Question> questions = questionRepository.findByExamIdOrderByIdAsc(examId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Este examen no tiene preguntas para iniciar repaso grupal.");
        }

        List<Long> questionIds = new ArrayList<>(questions.stream().map(Question::getId).toList());
        String orderMode = "random".equalsIgnoreCase(access.exam().getPracticeOrderMode()) ? "random" : "ordered";
        if ("random".equals(orderMode)) {
            Collections.shuffle(questionIds);
        }

        LocalDateTime now = LocalDateTime.now();
        session.setStatus("active");
        session.setOrderMode(orderMode);
        session.setQuestionIds(serializeQuestionIds(questionIds));
        session.setTotalQuestions(questionIds.size());
        session.setCurrentQuestionIndex(0);
        session.setStartedAt(now);
        session.setCurrentQuestionStartedAt(now);
        session.setPhase(PHASE_OPEN);
        session.setPhaseStartedAt(now);
        session.setPhaseEndsAt(resolveQuestionDeadline(resolveCurrentQuestion(session).orElse(null), now));
        session.setQuestionVersion(1);
        session.setFinishedAt(null);
        session = examGroupSessionRepository.save(session);

        return toGroupState(session, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
    }

    @Transactional
    public ExamGroupStateResponse state(Long examId, Long sessionId, Long userId, String roomSessionToken) {
        GroupAccess access = resolveGroupAccess(examId, userId);
        ExamGroupSession session = requireSession(examId, sessionId);
        requireSessionMemberReadAccess(session, access.user().getId());

        // Heartbeat de presencia: cada consulta de estado renueva conexion del usuario.
        // Esto evita que participantes activos se marquen como desconectados por timeout.
        String requesterRoomSessionToken = ensureValidRoomSession(session, access.user(), roomSessionToken);
        session = refreshSessionPresence(session);

        // Si el cliente consulta una sesion finalizada, solo redirigir a una sala
        // mas nueva creada DESPUES de que esta sesion termino (reinicio explicito).
        if ("finished".equals(normalizeStatus(session.getStatus()))) {
            ExamGroupSession latestSession = examGroupSessionRepository
                    .findTopByExamIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(examId, List.of("waiting", "active"))
                    .orElse(null);
            if (latestSession != null
                    && latestSession.getId() != null
                    && !latestSession.getId().equals(session.getId())
                    && isNewerSessionSinceFinished(session, latestSession)
                    && hasExistingSessionMembership(latestSession, access.user().getId())) {
                String latestRoomSessionToken = ensureValidRoomSession(latestSession, access.user(), null);
                return toGroupState(latestSession, userId, access.canStartGroup(), latestRoomSessionToken);
            }
        }

        // El servidor decide el avance por timeout de review, sin depender de clicks manuales.
        session = syncSessionPhase(session);

        return toGroupState(session, userId, access.canStartGroup(), requesterRoomSessionToken);
    }

    private void requireSessionMemberReadAccess(ExamGroupSession session, Long userId) {
        if (session == null || session.getId() == null || userId == null) {
            throw new ForbiddenException("No tienes permiso para ver el estado de esta sala grupal.");
        }
        if (hasExistingSessionMembership(session, userId)) {
            return;
        }
        throw new ForbiddenException("No perteneces a esta sala grupal.");
    }

    private boolean hasExistingSessionMembership(ExamGroupSession session, Long userId) {
        if (session == null || session.getId() == null || userId == null) {
            return false;
        }
        return examGroupSessionMemberRepository
                .findBySessionIdAndUserIdAndDeletedAtIsNull(session.getId(), userId)
                .isPresent();
    }

    private boolean isNewerSessionSinceFinished(ExamGroupSession finishedSession, ExamGroupSession candidateSession) {
        if (finishedSession == null || candidateSession == null) {
            return false;
        }

        LocalDateTime candidateCreatedAt = candidateSession.getCreatedAt();
        if (candidateCreatedAt == null) {
            return false;
        }

        LocalDateTime finishedAt = finishedSession.getFinishedAt();
        if (finishedAt == null) {
            return true;
        }

        // Incluye igualdad para tolerar precision de timestamp a nivel de BD.
        return !candidateCreatedAt.isBefore(finishedAt);
    }

    @Transactional
    public ExamGroupStateResponse answer(Long examId, ExamGroupAnswerRequest request) {
        GroupAccess access = resolveGroupAccess(examId, request.userId());
        ExamGroupSession session = requireSession(examId, request.sessionId());
        String requesterRoomSessionToken = ensureValidRoomSession(session, access.user(), request.roomSessionToken());
        session = refreshSessionPresence(session);
        session = syncSessionPhase(session);

        if (!"active".equals(normalizeStatus(session.getStatus()))) {
            throw new BadRequestException("La sesion grupal no esta activa.");
        }

        if (!PHASE_OPEN.equals(normalizePhase(session.getPhase()))) {
            throw new BadRequestException("La pregunta actual ya esta en revision.");
        }

        Question currentQuestion = resolveCurrentQuestion(session)
                .orElseThrow(() -> new BadRequestException("No hay pregunta activa en esta sesion grupal."));
        if (!currentQuestion.getId().equals(request.questionId())) {
            throw new BadRequestException("La pregunta enviada no coincide con la pregunta actual del grupo.");
        }
        if (request.questionVersion() != null
                && request.questionVersion() > 0
                && !request.questionVersion().equals(session.getQuestionVersion())) {
            throw new BadRequestException("Tu respuesta pertenece a una version anterior de la pregunta.");
        }

        List<ExamGroupSessionAnswer> storedAnswers = examGroupSessionAnswerRepository
                .findAllForUserQuestion(session.getId(), access.user().getId(), currentQuestion.getId());
        ExamGroupSessionAnswer answer = pickBestAnswer(storedAnswers);
        if (answer == null) {
            answer = new ExamGroupSessionAnswer();
            answer.setSession(session);
            answer.setUser(access.user());
            answer.setQuestion(currentQuestion);
        } else if (storedAnswers.size() > 1) {
            // Mantener una sola fila por usuario/pregunta/sesion para evitar lecturas inconsistentes.
            Long answerIdToKeep = answer.getId();
            List<Long> duplicateIds = storedAnswers.stream()
                    .filter(item -> item.getId() != null)
                    .filter(item -> !item.getId().equals(answerIdToKeep))
                    .map(ExamGroupSessionAnswer::getId)
                    .toList();
            if (!duplicateIds.isEmpty()) {
                examGroupSessionAnswerRepository.deleteAllByIds(duplicateIds);
            }
        }

        String selectedAnswer = resolveSubmittedAnswer(currentQuestion, request);

        // Nunca persistir envios vacios. Un timeout sin seleccion no cuenta como respuesta.
        if (trimOrNull(selectedAnswer) == null) {
            return toGroupState(session, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
        }

        // Si llega un envio vacio (por ejemplo auto-envio tardio), no sobreescribir
        // una respuesta ya guardada para este usuario en esta pregunta.
        boolean isCorrect = evaluateAnswer(currentQuestion, selectedAnswer);
        answer.setSelectedAnswer(selectedAnswer);
        answer.setIsCorrect(isCorrect);
        answer.setAnsweredAt(LocalDateTime.now());
        examGroupSessionAnswerRepository.save(answer);
        session = syncSessionPhase(session);
        return toGroupState(session, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
    }

    @Transactional
    public ExamGroupStateResponse next(Long examId, ExamGroupAdvanceRequest request) {
        GroupAccess access = resolveGroupAccess(examId, request.userId());
        if (!access.canStartGroup()) {
            throw new BadRequestException("No tienes permiso para avanzar la pregunta grupal.");
        }

        ExamGroupSession session = requireSession(examId, request.sessionId());
        String requesterRoomSessionToken = ensureValidRoomSession(session, access.user(), request.roomSessionToken());
        session = refreshSessionPresence(session);
        if (!"active".equals(normalizeStatus(session.getStatus()))) {
            throw new BadRequestException("La sesion grupal no esta activa.");
        }

        if (resolveCurrentQuestion(session).isEmpty()) {
            throw new BadRequestException("No hay pregunta activa en esta sesion grupal.");
        }

        int currentIndex = session.getCurrentQuestionIndex() == null ? 0 : session.getCurrentQuestionIndex();
        int totalQuestions = session.getTotalQuestions() == null ? 0 : session.getTotalQuestions();
        if (currentIndex + 1 >= totalQuestions) {
            session.setStatus("finished");
            session.setFinishedAt(LocalDateTime.now());
            session.setCurrentQuestionStartedAt(null);
            session.setPhase(PHASE_OPEN);
            session.setPhaseStartedAt(null);
            session.setPhaseEndsAt(null);
            revokeRoomSessions(session.getId());
        } else {
            session.setCurrentQuestionIndex(currentIndex + 1);
            LocalDateTime now = LocalDateTime.now();
            session.setCurrentQuestionStartedAt(now);
            session.setPhase(PHASE_OPEN);
            session.setPhaseStartedAt(now);
            session.setPhaseEndsAt(resolveQuestionDeadline(resolveCurrentQuestion(session).orElse(null), now));
            session.setQuestionVersion((session.getQuestionVersion() == null ? 1 : session.getQuestionVersion()) + 1);
        }

        session = examGroupSessionRepository.save(session);
        return toGroupState(session, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
    }

    @Transactional
    public ExamGroupStateResponse close(Long examId, ExamGroupAdvanceRequest request) {
        GroupAccess access = resolveGroupAccess(examId, request.userId());
        if (!access.canStartGroup()) {
            throw new BadRequestException("No tienes permiso para cerrar el repaso grupal.");
        }

        ExamGroupSession session = requireSession(examId, request.sessionId());
        String requesterRoomSessionToken = ensureValidRoomSession(session, access.user(), request.roomSessionToken());

        LocalDateTime now = LocalDateTime.now();
        session.setStatus("finished");
        if (session.getStartedAt() == null) {
            session.setStartedAt(now);
        }
        session.setCurrentQuestionStartedAt(null);
        session.setPhase(PHASE_OPEN);
        session.setPhaseStartedAt(null);
        session.setPhaseEndsAt(null);
        session.setFinishedAt(now);
        session = examGroupSessionRepository.save(session);
        revokeRoomSessions(session.getId());

        List<ExamGroupSessionMember> members =
                examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(session.getId());
        for (ExamGroupSessionMember member : members) {
            member.setConnected(Boolean.FALSE);
        }
        if (!members.isEmpty()) {
            examGroupSessionMemberRepository.saveAll(members);
        }

        return toGroupState(session, access.user().getId(), access.canStartGroup(), requesterRoomSessionToken);
    }

    @Transactional
    public ExamGroupStateResponse restart(Long examId, ExamGroupAdvanceRequest request) {
        GroupAccess access = resolveGroupAccess(examId, request.userId());
        if (!access.canStartGroup()) {
            throw new BadRequestException("No tienes permiso para reiniciar el repaso grupal.");
        }

        ExamGroupSession session = examGroupSessionRepository
            .findByIdAndExamIdAndDeletedAtIsNull(request.sessionId(), examId)
            .orElseGet(() -> examGroupSessionRepository
                .findTopByExamIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(
                    examId,
                    List.of("waiting", "active", "finished"))
                .orElseThrow(() -> new NotFoundException("Sesion grupal no encontrada.")));
        String requesterRoomSessionToken = ensureValidRoomSession(session, access.user(), request.roomSessionToken());

        // Conserva historial: finaliza la sesion actual y crea una nueva para el siguiente intento.
        LocalDateTime now = LocalDateTime.now();
        if (session.getStartedAt() == null) {
            session.setStartedAt(now);
        }
        if (!"finished".equals(normalizeStatus(session.getStatus()))) {
            session.setStatus("finished");
        }
        session.setCurrentQuestionStartedAt(null);
        if (session.getFinishedAt() == null) {
            session.setFinishedAt(now);
        }
        session = examGroupSessionRepository.save(session);
        revokeRoomSessions(session.getId());

        List<ExamGroupSessionMember> previousMembers =
                examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(session.getId());

        ExamGroupSession newSession = new ExamGroupSession();
        newSession.setExam(session.getExam());
        newSession.setCreatedByUser(access.user());
        newSession.setStatus("waiting");
        newSession.setOrderMode("ordered");
        newSession.setQuestionIds(null);
        newSession.setTotalQuestions(0);
        newSession.setCurrentQuestionIndex(0);
        newSession.setStartedAt(null);
        newSession.setCurrentQuestionStartedAt(null);
        newSession.setPhase(PHASE_OPEN);
        newSession.setPhaseStartedAt(null);
        newSession.setPhaseEndsAt(null);
        newSession.setQuestionVersion(1);
        newSession.setFinishedAt(null);
        newSession = examGroupSessionRepository.save(newSession);

        List<ExamGroupSessionMember> clonedMembers = new ArrayList<>();
        Map<Long, ExamGroupSessionMember> uniqueMembers = new HashMap<>();
        for (ExamGroupSessionMember previousMember : previousMembers) {
            User memberUser = previousMember.getUser();
            if (memberUser == null || memberUser.getId() == null) {
                continue;
            }
            uniqueMembers.putIfAbsent(memberUser.getId(), previousMember);
        }
        uniqueMembers.putIfAbsent(access.user().getId(), ensureSessionMember(session, access.user()));

        for (ExamGroupSessionMember sourceMember : uniqueMembers.values()) {
            if (sourceMember.getUser() == null || sourceMember.getUser().getId() == null) {
                continue;
            }
            ExamGroupSessionMember member = new ExamGroupSessionMember();
            member.setSession(newSession);
            member.setUser(sourceMember.getUser());
            member.setConnected(Boolean.TRUE);
            member.setLastSeenAt(now);
            member.setDeletedAt(null);
            clonedMembers.add(member);
        }
        if (!clonedMembers.isEmpty()) {
            examGroupSessionMemberRepository.saveAll(clonedMembers);
        }

        newSession = refreshSessionPresence(newSession);
        String newSessionRoomToken = ensureValidRoomSession(newSession, access.user(), null);
        return toGroupState(newSession, access.user().getId(), access.canStartGroup(), newSessionRoomToken);
    }

    private GroupAccess resolveGroupAccess(Long examId, Long userId) {
        ensureAuthenticatedUserMatchesRequest(userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        Exam exam = examRepository.findByIdAndDeletedAtIsNull(examId)
                .orElseThrow(() -> new NotFoundException("Examen no encontrado"));
        Long ownerUserId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerUserId != null && ownerUserId.equals(userId)) {
            return new GroupAccess(exam, user, true, true, null);
        }

        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(examId, userId)
                .orElse(null);
        if (membership == null && !"public".equals(normalizeVisibility(exam.getVisibility()))) {
            throw new NotFoundException("Examen no encontrado");
        }

        boolean canStartGroup = membership != null && Boolean.TRUE.equals(membership.getCanStartGroup());
        return new GroupAccess(exam, user, false, canStartGroup, membership);
    }

    private void ensureAuthenticatedUserMatchesRequest(Long requestedUserId) {
        if (requestedUserId == null) {
            throw new BadRequestException("userId es obligatorio.");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtUserPrincipal jwtPrincipal
                && !requestedUserId.equals(jwtPrincipal.userId())) {
            throw new ForbiddenException("No tienes permiso para operar con otro usuario.");
        }
    }

    private ExamGroupSession requireSession(Long examId, Long sessionId) {
        return examGroupSessionRepository
                .findByIdAndExamIdAndDeletedAtIsNull(sessionId, examId)
                .orElseThrow(() -> new NotFoundException("Sesion grupal no encontrada."));
    }

    private void ensureExamMembership(Exam exam, User user) {
        if (exam == null || user == null || exam.getId() == null || user.getId() == null) {
            return;
        }
        Long ownerId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerId != null && ownerId.equals(user.getId())) {
            return;
        }
        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(exam.getId(), user.getId())
                .orElse(null);
        if (membership == null) {
            membership = new ExamMembership();
            membership.setExam(exam);
            membership.setUser(user);
        }
        membership.setRole(normalizeRole(membership.getRole()));
        membership.setCanShare(Boolean.TRUE.equals(membership.getCanShare()));
        membership.setCanStartGroup(Boolean.TRUE.equals(membership.getCanStartGroup()));
        membership.setDeletedAt(null);
        try {
            examMembershipRepository.save(membership);
        } catch (DataIntegrityViolationException raceCondition) {
            ExamMembership concurrentMembership = examMembershipRepository
                    .findByExamIdAndUserIdAndDeletedAtIsNull(exam.getId(), user.getId())
                    .orElseGet(() -> examMembershipRepository
                            .findTopByExamIdAndUserIdOrderByIdDesc(exam.getId(), user.getId())
                            .orElseThrow(() -> raceCondition));
            concurrentMembership.setRole(normalizeRole(concurrentMembership.getRole()));
            concurrentMembership.setCanShare(Boolean.TRUE.equals(concurrentMembership.getCanShare()));
            concurrentMembership.setCanStartGroup(Boolean.TRUE.equals(concurrentMembership.getCanStartGroup()));
            concurrentMembership.setDeletedAt(null);
            examMembershipRepository.save(concurrentMembership);
        }
    }

    private ExamGroupSessionMember ensureSessionMember(ExamGroupSession session, User user) {
        ExamGroupSessionMember member = examGroupSessionMemberRepository
                .findBySessionIdAndUserIdAndDeletedAtIsNull(session.getId(), user.getId())
                .orElse(null);
        if (member == null) {
            member = new ExamGroupSessionMember();
            member.setSession(session);
            member.setUser(user);
        }
        member.setConnected(Boolean.TRUE);
        member.setLastSeenAt(LocalDateTime.now());
        member.setDeletedAt(null);
        try {
            ExamGroupSessionMember persisted = examGroupSessionMemberRepository.save(member);
            return persisted != null ? persisted : member;
        } catch (DataIntegrityViolationException raceCondition) {
            ExamGroupSessionMember concurrentMember = examGroupSessionMemberRepository
                    .findBySessionIdAndUserIdAndDeletedAtIsNull(session.getId(), user.getId())
                    .orElseGet(() -> examGroupSessionMemberRepository
                            .findTopBySessionIdAndUserIdOrderByIdDesc(session.getId(), user.getId())
                            .orElseThrow(() -> raceCondition));
            concurrentMember.setConnected(Boolean.TRUE);
            concurrentMember.setLastSeenAt(LocalDateTime.now());
            concurrentMember.setDeletedAt(null);
            ExamGroupSessionMember persisted = examGroupSessionMemberRepository.save(concurrentMember);
            return persisted != null ? persisted : concurrentMember;
        }
    }

    private String ensureValidRoomSession(ExamGroupSession session, User user, String providedRoomSessionToken) {
        ensureSessionMember(session, user);
        LocalDateTime now = LocalDateTime.now();
        String providedToken = trimOrNull(providedRoomSessionToken);

        if (providedToken != null) {
            ExamGroupRoomSession roomSession = examGroupRoomSessionRepository
                    .findTopBySessionIdAndRoomTokenAndDeletedAtIsNullAndRevokedAtIsNullOrderByIdDesc(
                            session.getId(), providedToken)
                    .orElseThrow(() ->
                            new RoomSessionInvalidException("Tu sesion de sala no es valida. Vuelve a unirte al repaso grupal."));
            Long roomSessionUserId = roomSession.getUser() == null ? null : roomSession.getUser().getId();
            if (roomSessionUserId == null || !roomSessionUserId.equals(user.getId())) {
                throw new RoomSessionInvalidException("Tu sesion de sala no es valida. Vuelve a unirte al repaso grupal.");
            }
            if (roomSession.getExpiresAt() == null || !roomSession.getExpiresAt().isAfter(now)) {
                throw new RoomSessionInvalidException("Tu sesion de sala no es valida. Vuelve a unirte al repaso grupal.");
            }
            roomSession.setExpiresAt(now.plusMinutes(ROOM_SESSION_TTL_MINUTES));
            roomSession.setDeletedAt(null);
            roomSession.setRevokedAt(null);
            ExamGroupRoomSession persisted = examGroupRoomSessionRepository.save(roomSession);
            return trimOrDefault(persisted.getRoomToken(), providedToken);
        }

        ExamGroupRoomSession activeRoomSession = examGroupRoomSessionRepository
                .findTopBySessionIdAndUserIdAndDeletedAtIsNullAndRevokedAtIsNullOrderByIdDesc(session.getId(), user.getId())
                .orElse(null);
        if (activeRoomSession != null
                && activeRoomSession.getExpiresAt() != null
                && activeRoomSession.getExpiresAt().isAfter(now.plusSeconds(10))) {
            activeRoomSession.setExpiresAt(now.plusMinutes(ROOM_SESSION_TTL_MINUTES));
            activeRoomSession.setDeletedAt(null);
            activeRoomSession.setRevokedAt(null);
            ExamGroupRoomSession persisted = examGroupRoomSessionRepository.save(activeRoomSession);
            return trimOrDefault(persisted.getRoomToken(), "");
        }

        ExamGroupRoomSession newRoomSession = new ExamGroupRoomSession();
        newRoomSession.setSession(session);
        newRoomSession.setUser(user);
        newRoomSession.setRoomToken(UUID.randomUUID().toString());
        newRoomSession.setIssuedAt(now);
        newRoomSession.setExpiresAt(now.plusMinutes(ROOM_SESSION_TTL_MINUTES));
        newRoomSession.setRevokedAt(null);
        newRoomSession.setDeletedAt(null);
        ExamGroupRoomSession persisted = examGroupRoomSessionRepository.save(newRoomSession);
        return trimOrDefault(persisted.getRoomToken(), "");
    }

    private ExamGroupSession refreshSessionPresence(ExamGroupSession session) {
        List<ExamGroupSessionMember> members =
                examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(session.getId());
        if (members.isEmpty()) {
            return session;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusSeconds(MEMBER_PRESENCE_TIMEOUT_SECONDS);
        long connectedCount = 0;
        boolean membersChanged = false;

        for (ExamGroupSessionMember member : members) {
            boolean connected = Boolean.TRUE.equals(member.getConnected());
            LocalDateTime lastSeenAt = member.getLastSeenAt();
            boolean stale = lastSeenAt == null || lastSeenAt.isBefore(cutoff);
            if (connected && stale) {
                member.setConnected(Boolean.FALSE);
                connected = false;
                membersChanged = true;
            }
            if (connected) {
                connectedCount++;
            }
        }

        if (membersChanged) {
            examGroupSessionMemberRepository.saveAll(members);
        }

        String status = normalizeStatus(session.getStatus());
        if (connectedCount == 0 && ("waiting".equals(status) || "active".equals(status))) {
            session.setStatus("finished");
            if ("active".equals(status) && session.getStartedAt() == null) {
                session.setStartedAt(now);
            }
            session.setCurrentQuestionStartedAt(null);
            session.setFinishedAt(now);
            revokeRoomSessions(session.getId());
            return examGroupSessionRepository.save(session);
        }

        return session;
    }

    private ExamGroupStateResponse toGroupState(
            ExamGroupSession session, Long requesterUserId, boolean requesterCanStartGroup, String roomSessionToken) {
        String status = normalizeStatus(session.getStatus());
        Question currentQuestion = resolveCurrentQuestion(session).orElse(null);
        Map<Long, ExamGroupSessionAnswer> answerByUserId = new HashMap<>();
        List<ExamGroupSessionAnswer> currentQuestionAnswers = new ArrayList<>();
        LocalDateTime firstAnsweredAt = null;
        Long firstAnsweredUserId = null;
        if (currentQuestion != null) {
            for (ExamGroupSessionAnswer answer :
                    examGroupSessionAnswerRepository.findForQuestion(session.getId(), currentQuestion.getId())) {
                if (answer.getUser() == null || answer.getUser().getId() == null) {
                    continue;
                }
                Long answeredUserId = answer.getUser().getId();
                ExamGroupSessionAnswer existing = answerByUserId.get(answeredUserId);
                if (existing == null || compareAnswerPriority(answer, existing) > 0) {
                    answerByUserId.put(answeredUserId, answer);
                }
            }

            currentQuestionAnswers.addAll(answerByUserId.values());

            for (ExamGroupSessionAnswer answer : currentQuestionAnswers) {
                Long answeredUserId = answer.getUser() == null ? null : answer.getUser().getId();
                LocalDateTime answeredAt = answer.getAnsweredAt();
                if (answeredUserId != null && answeredAt != null
                        && (firstAnsweredAt == null || answeredAt.isBefore(firstAnsweredAt))) {
                    firstAnsweredAt = answeredAt;
                    firstAnsweredUserId = answeredUserId;
                }
            }
        }

        currentQuestionAnswers.sort(Comparator
                .comparing(ExamGroupSessionAnswer::getAnsweredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ExamGroupSessionAnswer::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ExamGroupSessionAnswer::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Option> currentQuestionOptions = currentQuestion == null
            ? List.of()
            : optionRepository.findByQuestionIdOrderByIdAsc(currentQuestion.getId());

        List<ExamGroupCurrentAnswerResponse> currentAnswers = currentQuestionAnswers.stream()
                .map(answer -> {
                    User answerUser = answer.getUser();
                    if (answerUser == null || answerUser.getId() == null) {
                        return null;
                    }
                String selectedAnswer = trimOrDefault(answer.getSelectedAnswer(), "");
                    return new ExamGroupCurrentAnswerResponse(
                            answerUser.getId(),
                            trimOrDefault(answerUser.getName(), "Usuario"),
                            trimOrDefault(answerUser.getUsername(), ""),
                            resolveUserProfileImage(answerUser),
                    selectedAnswer,
                    resolveSelectedOptionKey(currentQuestion, currentQuestionOptions, selectedAnswer),
                            answer.getIsCorrect(),
                            answer.getAnsweredAt());
                })
                .filter(item -> item != null)
                .toList();

        List<ExamGroupSessionMember> members =
                examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(session.getId());
        List<ExamMembership> memberships = examMembershipRepository.findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                session.getExam().getId());
        Map<Long, ExamMembership> membershipByUserId = new HashMap<>();
        for (ExamMembership membership : memberships) {
            if (membership.getUser() != null && membership.getUser().getId() != null) {
                membershipByUserId.put(membership.getUser().getId(), membership);
            }
        }

        Long ownerUserId = session.getExam().getUser() == null ? null : session.getExam().getUser().getId();
        List<ExamGroupParticipantStateResponse> participants = new ArrayList<>();
        for (ExamGroupSessionMember member : members) {
            User participant = member.getUser();
            if (participant == null || participant.getId() == null) {
                continue;
            }
            Long participantUserId = participant.getId();
            boolean owner = ownerUserId != null && ownerUserId.equals(participantUserId);
            ExamMembership membership = membershipByUserId.get(participantUserId);
            String role = owner ? "owner" : normalizeRole(membership == null ? null : membership.getRole());
            boolean canStart = owner || (membership != null && Boolean.TRUE.equals(membership.getCanStartGroup()));
            ExamGroupSessionAnswer answer = answerByUserId.get(participantUserId);
            participants.add(new ExamGroupParticipantStateResponse(
                    participantUserId,
                    trimOrDefault(participant.getName(), "Usuario"),
                    trimOrDefault(participant.getUsername(), ""),
                    resolveUserProfileImage(participant),
                    role,
                    canStart,
                    owner,
                    Boolean.TRUE.equals(member.getConnected()),
                    answer != null && trimOrNull(answer.getSelectedAnswer()) != null,
                    answer == null ? null : answer.getIsCorrect()));
        }

        boolean allAnsweredCurrent = false;
        if (currentQuestion != null && !participants.isEmpty()) {
            long connectedCount = participants.stream().filter(participant -> Boolean.TRUE.equals(participant.connected())).count();
            long answeredCount = participants.stream()
                    .filter(participant -> Boolean.TRUE.equals(participant.connected()))
                    .filter(participant -> Boolean.TRUE.equals(participant.answeredCurrent()))
                    .count();
            allAnsweredCurrent = connectedCount > 0 && answeredCount >= connectedCount;
        }

        String firstResponderName = null;
        if (firstAnsweredUserId != null) {
            for (ExamGroupParticipantStateResponse participant : participants) {
                if (firstAnsweredUserId.equals(participant.userId())) {
                    firstResponderName = participant.name();
                    break;
                }
            }
        }

        LocalDateTime questionStartedAt = null;
        Long questionStartedAtEpochMs = null;
        Integer firstAnswerElapsedSeconds = null;
        String phase = normalizePhase(session.getPhase());
        LocalDateTime phaseStartedAt = session.getPhaseStartedAt();
        LocalDateTime phaseEndsAt = session.getPhaseEndsAt();
        Long phaseStartedAtEpochMs = phaseStartedAt == null ? null : phaseStartedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long phaseEndsAtEpochMs = phaseEndsAt == null ? null : phaseEndsAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Integer questionVersion = session.getQuestionVersion() == null ? 1 : session.getQuestionVersion();
        Boolean revealAnswers = Boolean.FALSE;
        Boolean reviewActive = Boolean.FALSE;
        Integer reviewSecondsRemaining = 0;
        Long serverNowEpochMs = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if ("active".equals(status) && currentQuestion != null) {
            questionStartedAt = session.getCurrentQuestionStartedAt() != null
                    ? session.getCurrentQuestionStartedAt()
                    : session.getStartedAt();
            if (questionStartedAt != null) {
                questionStartedAtEpochMs = questionStartedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            if (questionStartedAt != null && firstAnsweredAt != null) {
                long seconds = Duration.between(questionStartedAt, firstAnsweredAt).getSeconds();
                if (seconds >= 0 && seconds <= Integer.MAX_VALUE) {
                    firstAnswerElapsedSeconds = (int) seconds;
                }
            }
            if (PHASE_REVIEW.equals(phase) && phaseEndsAt != null) {
                long remaining = Math.max(0, Duration.between(LocalDateTime.now(), phaseEndsAt).getSeconds());
                reviewActive = remaining > 0;
                revealAnswers = reviewActive;
                reviewSecondsRemaining = (int) remaining;
            }
        }

            List<ExamGroupRankingEntryResponse> finalRanking = "finished".equals(status)
                ? buildFinalRanking(session, participants)
                : List.of();

        return new ExamGroupStateResponse(
                session.getId(),
                trimOrDefault(roomSessionToken, ""),
                session.getExam().getId(),
                session.getExam().getName(),
                status,
                session.getTotalQuestions() == null ? 0 : session.getTotalQuestions(),
                session.getCurrentQuestionIndex() == null ? 0 : session.getCurrentQuestionIndex(),
                allAnsweredCurrent,
                requesterCanStartGroup,
                currentQuestion == null ? null : toQuestionResponse(currentQuestion),
                currentAnswers,
                participants,
                finalRanking,
                firstResponderName,
                firstAnswerElapsedSeconds,
                questionStartedAt,
                questionStartedAtEpochMs,
                phase,
                phaseStartedAt,
                phaseEndsAt,
                phaseStartedAtEpochMs,
                phaseEndsAtEpochMs,
                questionVersion,
                revealAnswers,
                reviewActive,
                reviewSecondsRemaining,
                serverNowEpochMs,
                session.getStartedAt(),
                session.getFinishedAt());
    }

    private List<ExamGroupRankingEntryResponse> buildFinalRanking(
            ExamGroupSession session,
            List<ExamGroupParticipantStateResponse> participants) {
        if (session == null || session.getId() == null || participants == null || participants.isEmpty()) {
            return List.of();
        }

        Map<Long, RankingAccumulator> rankingByUserId = new LinkedHashMap<>();
        for (ExamGroupParticipantStateResponse participant : participants) {
            Long userId = participant.userId();
            if (userId == null) {
                continue;
            }
            rankingByUserId.put(
                    userId,
                    new RankingAccumulator(
                            userId,
                            trimOrDefault(participant.name(), "Usuario"),
                            trimOrDefault(participant.username(), ""),
                            trimOrDefault(participant.profileImageUrl(), "")));
        }

        List<Long> questionIds = parseQuestionIds(session.getQuestionIds());
        if (questionIds.isEmpty()) {
            return rankAccumulators(rankingByUserId);
        }

        Map<Long, Question> questionById = new HashMap<>();
        for (Question question : questionRepository.findAllById(questionIds)) {
            if (question != null && question.getId() != null) {
                questionById.put(question.getId(), question);
            }
        }

        for (Long questionId : questionIds) {
            if (questionId == null) {
                continue;
            }

            Question question = questionById.get(questionId);
            if (question == null) {
                continue;
            }

            int questionPoints = question.getPoints() == null || question.getPoints() <= 0 ? 1 : question.getPoints();
            List<ExamGroupSessionAnswer> answers = examGroupSessionAnswerRepository.findForQuestion(session.getId(), questionId);

            Map<Long, ExamGroupSessionAnswer> finalAnswerByUserId = new HashMap<>();
            for (ExamGroupSessionAnswer answer : answers) {
                if (answer == null || answer.getUser() == null || answer.getUser().getId() == null) {
                    continue;
                }
                Long answerUserId = answer.getUser().getId();
                ExamGroupSessionAnswer existing = finalAnswerByUserId.get(answerUserId);
                if (existing == null || compareAnswerPriority(answer, existing) > 0) {
                    finalAnswerByUserId.put(answerUserId, answer);
                }
            }

            ExamGroupSessionAnswer firstCorrectAnswer = null;
            for (ExamGroupSessionAnswer answer : finalAnswerByUserId.values()) {
                if (!Boolean.TRUE.equals(answer.getIsCorrect()) || answer.getAnsweredAt() == null) {
                    continue;
                }
                if (firstCorrectAnswer == null || compareFirstCorrectAnswer(answer, firstCorrectAnswer) < 0) {
                    firstCorrectAnswer = answer;
                }
            }

            for (ExamGroupSessionAnswer answer : finalAnswerByUserId.values()) {
                if (answer == null || answer.getUser() == null || answer.getUser().getId() == null) {
                    continue;
                }
                RankingAccumulator accumulator = rankingByUserId.get(answer.getUser().getId());
                if (accumulator == null) {
                    continue;
                }
                if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                    accumulator.correctCount += 1;
                    accumulator.baseScore += questionPoints;
                } else {
                    accumulator.wrongCount += 1;
                }
            }

            if (firstCorrectAnswer != null && firstCorrectAnswer.getUser() != null && firstCorrectAnswer.getUser().getId() != null) {
                RankingAccumulator fastest = rankingByUserId.get(firstCorrectAnswer.getUser().getId());
                if (fastest != null) {
                    fastest.speedBonus += 1;
                }
            }
        }

        return rankAccumulators(rankingByUserId);
    }

    private List<ExamGroupRankingEntryResponse> rankAccumulators(Map<Long, RankingAccumulator> rankingByUserId) {
        List<RankingAccumulator> ordered = new ArrayList<>(rankingByUserId.values());
        ordered.sort((left, right) -> {
            int leftFinal = left.baseScore + left.speedBonus;
            int rightFinal = right.baseScore + right.speedBonus;
            if (leftFinal != rightFinal) {
                return Integer.compare(rightFinal, leftFinal);
            }
            if (left.correctCount != right.correctCount) {
                return Integer.compare(right.correctCount, left.correctCount);
            }
            if (left.wrongCount != right.wrongCount) {
                return Integer.compare(left.wrongCount, right.wrongCount);
            }
            return left.name.compareToIgnoreCase(right.name);
        });

        List<ExamGroupRankingEntryResponse> ranked = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            RankingAccumulator accumulator = ordered.get(index);
            ranked.add(new ExamGroupRankingEntryResponse(
                    index + 1,
                    accumulator.userId,
                    accumulator.name,
                    accumulator.username,
                    accumulator.profileImageUrl,
                    accumulator.correctCount,
                    accumulator.wrongCount,
                    accumulator.baseScore,
                    accumulator.speedBonus,
                    accumulator.baseScore + accumulator.speedBonus));
        }
        return ranked;
    }

    private int compareFirstCorrectAnswer(ExamGroupSessionAnswer left, ExamGroupSessionAnswer right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        LocalDateTime leftAnsweredAt = left.getAnsweredAt();
        LocalDateTime rightAnsweredAt = right.getAnsweredAt();
        if (leftAnsweredAt != null && rightAnsweredAt != null) {
            int byAnsweredAt = leftAnsweredAt.compareTo(rightAnsweredAt);
            if (byAnsweredAt != 0) {
                return byAnsweredAt;
            }
        } else if (leftAnsweredAt != null) {
            return -1;
        } else if (rightAnsweredAt != null) {
            return 1;
        }

        Long leftId = left.getId();
        Long rightId = right.getId();
        if (leftId != null && rightId != null) {
            return leftId.compareTo(rightId);
        }
        if (leftId != null) {
            return -1;
        }
        if (rightId != null) {
            return 1;
        }
        return 0;
    }

    private Optional<Question> resolveCurrentQuestion(ExamGroupSession session) {
        if (!"active".equals(normalizeStatus(session.getStatus()))) {
            return Optional.empty();
        }
        List<Long> questionIds = parseQuestionIds(session.getQuestionIds());
        if (questionIds.isEmpty()) {
            return Optional.empty();
        }
        int index = session.getCurrentQuestionIndex() == null ? 0 : session.getCurrentQuestionIndex();
        if (index < 0) {
            index = 0;
        }
        if (index >= questionIds.size()) {
            index = questionIds.size() - 1;
        }
        Long questionId = questionIds.get(index);
        return questionRepository.findById(questionId);
    }

    private String resolveSubmittedAnswer(Question question, ExamGroupAnswerRequest request) {
        if ("multiple_choice".equals(question.getQuestionType())) {
            String selectedOption = trimOrNull(request.selectedOption());
            if (selectedOption == null) {
                return null;
            }
            String optionKey = selectedOption.toLowerCase(Locale.ROOT);
            List<Option> options = optionRepository.findByQuestionIdOrderByIdAsc(question.getId());
            return switch (optionKey) {
                case "a" -> options.size() > 0 ? options.get(0).getOptionText() : null;
                case "b" -> options.size() > 1 ? options.get(1).getOptionText() : null;
                case "c" -> options.size() > 2 ? options.get(2).getOptionText() : null;
                case "d" -> options.size() > 3 ? options.get(3).getOptionText() : null;
                default -> null;
            };
        }
        return trimOrNull(request.writtenAnswer());
    }

    private boolean evaluateAnswer(Question question, String submittedAnswer) {
        if (submittedAnswer == null) {
            return false;
        }
        String expected = normalizeAnswer(question.getCorrectAnswer());
        String submitted = normalizeAnswer(submittedAnswer);
        return !expected.isEmpty() && expected.equals(submitted);
    }

    private String serializeQuestionIds(List<Long> questionIds) {
        return questionIds.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
    }

    private List<Long> parseQuestionIds(String questionIds) {
        String raw = trimOrNull(questionIds);
        if (raw == null) {
            return List.of();
        }
        List<Long> values = new ArrayList<>();
        for (String token : raw.split(",")) {
            String value = trimOrNull(token);
            if (value == null) {
                continue;
            }
            try {
                values.add(Long.parseLong(value));
            } catch (NumberFormatException exception) {
                return List.of();
            }
        }
        return values;
    }

    private ExamGroupSession syncSessionPhase(ExamGroupSession session) {
        if (session == null) {
            return null;
        }
        if (!"active".equals(normalizeStatus(session.getStatus()))) {
            return session;
        }

        Question currentQuestion = resolveCurrentQuestion(session).orElse(null);
        if (currentQuestion == null) {
            return session;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        String phase = normalizePhase(session.getPhase());
        LocalDateTime questionStartedAt = session.getCurrentQuestionStartedAt() != null
                ? session.getCurrentQuestionStartedAt()
                : session.getStartedAt();
        if (questionStartedAt == null) {
            questionStartedAt = now;
            session.setCurrentQuestionStartedAt(now);
            changed = true;
        }

        if (PHASE_OPEN.equals(phase)) {
            if (session.getPhaseStartedAt() == null) {
                session.setPhaseStartedAt(questionStartedAt);
                changed = true;
            }
            LocalDateTime expectedDeadline = resolveQuestionDeadline(currentQuestion, questionStartedAt);
            if ((session.getPhaseEndsAt() == null && expectedDeadline != null)
                    || (session.getPhaseEndsAt() != null && expectedDeadline == null)
                    || (session.getPhaseEndsAt() != null && expectedDeadline != null && !session.getPhaseEndsAt().equals(expectedDeadline))) {
                session.setPhaseEndsAt(expectedDeadline);
                changed = true;
            }

            boolean timerExpired = session.getPhaseEndsAt() != null && !now.isBefore(session.getPhaseEndsAt());
            boolean allConnectedAnswered = hasAllConnectedAnswered(session.getId(), currentQuestion.getId());
            if (timerExpired || allConnectedAnswered) {
                int reviewSeconds = Math.max(1, currentQuestion.getReviewSeconds() == null ? 10 : currentQuestion.getReviewSeconds());
                session.setPhase(PHASE_REVIEW);
                session.setPhaseStartedAt(now);
                session.setPhaseEndsAt(now.plusSeconds(reviewSeconds));
                log.info(
                        "GROUP_PHASE_AUTO_OPEN_TO_REVIEW sessionId={} examId={} questionIndex={} reason={} reviewSeconds={}",
                        session.getId(),
                        session.getExam() == null ? null : session.getExam().getId(),
                        session.getCurrentQuestionIndex(),
                        timerExpired ? "timer_expired" : "all_answered",
                        reviewSeconds);
                changed = true;
                phase = PHASE_REVIEW;
            }
        }

        if (PHASE_REVIEW.equals(phase)) {
            LocalDateTime reviewEndsAt = session.getPhaseEndsAt();
            if (reviewEndsAt != null && !now.isBefore(reviewEndsAt)) {
                int currentIndex = session.getCurrentQuestionIndex() == null ? 0 : session.getCurrentQuestionIndex();
                int totalQuestions = session.getTotalQuestions() == null ? 0 : session.getTotalQuestions();
                if (currentIndex + 1 >= totalQuestions) {
                    session.setStatus("finished");
                    session.setFinishedAt(now);
                    session.setCurrentQuestionStartedAt(null);
                    session.setPhase(PHASE_OPEN);
                    session.setPhaseStartedAt(null);
                    session.setPhaseEndsAt(null);
                    revokeRoomSessions(session.getId());
                    log.info(
                            "GROUP_PHASE_AUTO_REVIEW_TO_FINISHED sessionId={} examId={} lastQuestionIndex={} totalQuestions={}",
                            session.getId(),
                            session.getExam() == null ? null : session.getExam().getId(),
                            currentIndex,
                            totalQuestions);
                } else {
                    session.setCurrentQuestionIndex(currentIndex + 1);
                    session.setCurrentQuestionStartedAt(now);
                    session.setPhase(PHASE_OPEN);
                    session.setPhaseStartedAt(now);
                    Question nextQuestion = resolveCurrentQuestion(session).orElse(null);
                    session.setPhaseEndsAt(resolveQuestionDeadline(nextQuestion, now));
                    session.setQuestionVersion((session.getQuestionVersion() == null ? 1 : session.getQuestionVersion()) + 1);
                    log.info(
                            "GROUP_PHASE_AUTO_REVIEW_TO_OPEN sessionId={} examId={} fromQuestionIndex={} toQuestionIndex={} nextDeadline={} questionVersion={}",
                            session.getId(),
                            session.getExam() == null ? null : session.getExam().getId(),
                            currentIndex,
                            session.getCurrentQuestionIndex(),
                            session.getPhaseEndsAt(),
                            session.getQuestionVersion());
                }
                changed = true;
            }
        }

        return changed ? examGroupSessionRepository.save(session) : session;
    }

    private void revokeRoomSessions(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        examGroupRoomSessionRepository.revokeActiveBySessionId(sessionId, LocalDateTime.now());
    }

    private boolean hasAllConnectedAnswered(Long sessionId, Long questionId) {
        if (sessionId == null || questionId == null) {
            return false;
        }
        List<ExamGroupSessionMember> members =
                examGroupSessionMemberRepository.findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(sessionId);
        // Regla de negocio: para cerrar una pregunta por "todos respondieron",
        // se exige respuesta de todos los integrantes de la sala.
        // No se debe adelantar solo porque algun miembro quede temporalmente desconectado.
        List<Long> participantUserIds = members.stream()
                .map(member -> member.getUser() == null ? null : member.getUser().getId())
                .filter(userId -> userId != null)
                .toList();
        if (participantUserIds.isEmpty()) {
            return false;
        }
        Map<Long, ExamGroupSessionAnswer> answerByUserId = new HashMap<>();
        for (ExamGroupSessionAnswer answer : examGroupSessionAnswerRepository.findForQuestion(sessionId, questionId)) {
            if (answer == null || answer.getUser() == null || answer.getUser().getId() == null) {
                continue;
            }
            Long userId = answer.getUser().getId();
            ExamGroupSessionAnswer existing = answerByUserId.get(userId);
            if (existing == null || compareAnswerPriority(answer, existing) > 0) {
                answerByUserId.put(userId, answer);
            }
        }
        for (Long participantUserId : participantUserIds) {
            ExamGroupSessionAnswer answer = answerByUserId.get(participantUserId);
            if (answer == null || trimOrNull(answer.getSelectedAnswer()) == null) {
                return false;
            }
        }
        return true;
    }

    private LocalDateTime resolveQuestionDeadline(Question question, LocalDateTime startedAt) {
        if (question == null || startedAt == null) {
            return null;
        }
        int timerSeconds = Math.max(0, question.getTemporizadorSegundos() == null ? 0 : question.getTemporizadorSegundos());
        if (timerSeconds <= 0) {
            return null;
        }
        return startedAt.plusSeconds(timerSeconds);
    }

    private String normalizeStatus(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "waiting";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if ("active".equals(normalized) || "finished".equals(normalized)) {
            return normalized;
        }
        return "waiting";
    }

    private String normalizePhase(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return PHASE_OPEN;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return PHASE_REVIEW.equals(normalized) ? PHASE_REVIEW : PHASE_OPEN;
    }

    private String normalizeVisibility(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "private";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return "public".equals(normalized) ? "public" : "private";
    }

    private String normalizeRole(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "viewer";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return "editor".equals(normalized) ? "editor" : "viewer";
    }

    private String normalizeAnswer(String value) {
        String safe = value == null ? "" : value;
        String noAccents = Normalizer.normalize(safe, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "")
                .trim();
    }

    private String resolveSelectedOptionKey(Question question, List<Option> options, String selectedAnswer) {
        if (question == null || !"multiple_choice".equals(question.getQuestionType())) {
            return null;
        }
        String normalizedSelected = normalizeAnswer(selectedAnswer);
        if (normalizedSelected.isEmpty()) {
            return null;
        }

        for (int index = 0; index < options.size() && index < 4; index++) {
            Option option = options.get(index);
            String normalizedOption = normalizeAnswer(option == null ? null : option.getOptionText());
            if (!normalizedOption.isEmpty() && normalizedOption.equals(normalizedSelected)) {
                return switch (index) {
                    case 0 -> "a";
                    case 1 -> "b";
                    case 2 -> "c";
                    case 3 -> "d";
                    default -> null;
                };
            }
        }
        return null;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimOrDefault(String value, String defaultValue) {
        String trimmed = trimOrNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String resolveUserProfileImage(User user) {
        if (user == null) {
            return "";
        }
        String custom = trimOrNull(user.getProfileImageData());
        if (custom != null) {
            return custom;
        }
        return trimOrDefault(user.getGooglePictureUrl(), "");
    }

    private ExamGroupSessionAnswer pickBestAnswer(List<ExamGroupSessionAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        ExamGroupSessionAnswer best = null;
        for (ExamGroupSessionAnswer candidate : answers) {
            if (candidate == null) {
                continue;
            }
            if (best == null || compareAnswerPriority(candidate, best) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    private int compareAnswerPriority(ExamGroupSessionAnswer left, ExamGroupSessionAnswer right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }

        boolean leftHasAnswer = trimOrNull(left.getSelectedAnswer()) != null;
        boolean rightHasAnswer = trimOrNull(right.getSelectedAnswer()) != null;
        if (leftHasAnswer != rightHasAnswer) {
            return leftHasAnswer ? 1 : -1;
        }

        LocalDateTime leftAnsweredAt = left.getAnsweredAt();
        LocalDateTime rightAnsweredAt = right.getAnsweredAt();
        if (leftAnsweredAt != null && rightAnsweredAt != null) {
            int answeredCompare = leftAnsweredAt.compareTo(rightAnsweredAt);
            if (answeredCompare != 0) {
                return answeredCompare;
            }
        } else if (leftAnsweredAt != null) {
            return 1;
        } else if (rightAnsweredAt != null) {
            return -1;
        }

        LocalDateTime leftCreatedAt = left.getCreatedAt();
        LocalDateTime rightCreatedAt = right.getCreatedAt();
        if (leftCreatedAt != null && rightCreatedAt != null) {
            int createdCompare = leftCreatedAt.compareTo(rightCreatedAt);
            if (createdCompare != 0) {
                return createdCompare;
            }
        } else if (leftCreatedAt != null) {
            return 1;
        } else if (rightCreatedAt != null) {
            return -1;
        }

        Long leftId = left.getId();
        Long rightId = right.getId();
        if (leftId != null && rightId != null) {
            return leftId.compareTo(rightId);
        }
        if (leftId != null) {
            return 1;
        }
        if (rightId != null) {
            return -1;
        }
        return 0;
    }

    private QuestionResponse toQuestionResponse(Question question) {
        List<Option> options = optionRepository.findByQuestionIdOrderByIdAsc(question.getId());
        String optionA = options.size() > 0 ? options.get(0).getOptionText() : null;
        String optionB = options.size() > 1 ? options.get(1).getOptionText() : null;
        String optionC = options.size() > 2 ? options.get(2).getOptionText() : null;
        String optionD = options.size() > 3 ? options.get(3).getOptionText() : null;

        String correctOption = null;
        for (int index = 0; index < options.size(); index++) {
            if (Boolean.TRUE.equals(options.get(index).getIsCorrect())) {
                correctOption = switch (index) {
                    case 0 -> "a";
                    case 1 -> "b";
                    case 2 -> "c";
                    case 3 -> "d";
                    default -> null;
                };
                break;
            }
        }

        return new QuestionResponse(
                question.getId(),
                question.getExam() == null ? null : question.getExam().getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getPoints(),
                question.getTemporizadorSegundos() != null ? question.getTemporizadorSegundos() : question.getTimeLimit(),
                question.getReviewSeconds() == null || question.getReviewSeconds() <= 0 ? 10 : question.getReviewSeconds(),
                question.getTimerEnabled(),
                optionA,
                optionB,
                optionC,
                optionD,
                correctOption);
    }

    private record GroupAccess(
            Exam exam,
            User user,
            boolean owner,
            boolean canStartGroup,
            ExamMembership membership) {}

    private static final class RankingAccumulator {
        private final Long userId;
        private final String name;
        private final String username;
        private final String profileImageUrl;
        private int correctCount;
        private int wrongCount;
        private int baseScore;
        private int speedBonus;

        private RankingAccumulator(Long userId, String name, String username, String profileImageUrl) {
            this.userId = userId;
            this.name = name;
            this.username = username;
            this.profileImageUrl = profileImageUrl;
            this.correctCount = 0;
            this.wrongCount = 0;
            this.baseScore = 0;
            this.speedBonus = 0;
        }
    }
}
