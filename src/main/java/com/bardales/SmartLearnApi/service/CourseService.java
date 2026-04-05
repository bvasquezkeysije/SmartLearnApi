package com.bardales.SmartLearnApi.service;

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
import com.bardales.SmartLearnApi.dto.course.CourseCompetencyItemResponse;
import com.bardales.SmartLearnApi.dto.course.CourseCompetencySaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseCreateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseExamItemResponse;
import com.bardales.SmartLearnApi.dto.course.CourseGradeItemResponse;
import com.bardales.SmartLearnApi.dto.course.CourseJoinResponse;
import com.bardales.SmartLearnApi.dto.course.CourseModuleResponse;
import com.bardales.SmartLearnApi.dto.course.CourseParticipantItemResponse;
import com.bardales.SmartLearnApi.dto.course.CourseParticipantRoleUpdateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseParticipantSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseResponse;
import com.bardales.SmartLearnApi.dto.course.CourseSessionContentItemResponse;
import com.bardales.SmartLearnApi.dto.course.CourseSessionContentPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.course.CourseSessionContentSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseSessionCreateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseSessionItemResponse;
import com.bardales.SmartLearnApi.dto.course.CourseSessionUpdateRequest;
import com.bardales.SmartLearnApi.dto.course.CourseWeekContentReorderRequest;
import com.bardales.SmartLearnApi.dto.course.CourseWeekItemResponse;
import com.bardales.SmartLearnApi.dto.course.CourseWeekSaveRequest;
import com.bardales.SmartLearnApi.dto.course.CourseSetExamsRequest;
import com.bardales.SmartLearnApi.dto.course.CourseUpdateRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamParticipantResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamParticipantPermissionUpdateRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAdvanceRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupAnswerRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupJoinRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStartRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamGroupStateResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamIndividualPracticeSettingsRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeSettingsRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeSettingsResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamRenameRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamSummaryResponse;
import com.bardales.SmartLearnApi.dto.exam.QuestionResponse;
import com.bardales.SmartLearnApi.dto.exam.ManualQuestionUpsertRequest;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMembershipRepository courseMembershipRepository;
    private final CourseExamRepository courseExamRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final CourseWeekRepository courseWeekRepository;
    private final CourseSessionContentRepository courseSessionContentRepository;
    private final CourseCompetencyRepository courseCompetencyRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final ExamService examService;
    private final ExamGroupPracticeService examGroupPracticeService;
    private final CoursePracticeWriteService coursePracticeWriteService;

    public CourseService(
            CourseRepository courseRepository,
            CourseMembershipRepository courseMembershipRepository,
            CourseExamRepository courseExamRepository,
            CourseSessionRepository courseSessionRepository,
            CourseWeekRepository courseWeekRepository,
            CourseSessionContentRepository courseSessionContentRepository,
            CourseCompetencyRepository courseCompetencyRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamRepository examRepository,
            UserRepository userRepository,
            ExamService examService,
            ExamGroupPracticeService examGroupPracticeService,
            CoursePracticeWriteService coursePracticeWriteService) {
        this.courseRepository = courseRepository;
        this.courseMembershipRepository = courseMembershipRepository;
        this.courseExamRepository = courseExamRepository;
        this.courseSessionRepository = courseSessionRepository;
        this.courseWeekRepository = courseWeekRepository;
        this.courseSessionContentRepository = courseSessionContentRepository;
        this.courseCompetencyRepository = courseCompetencyRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.examService = examService;
        this.examGroupPracticeService = examGroupPracticeService;
        this.coursePracticeWriteService = coursePracticeWriteService;
    }

    @Transactional(readOnly = true)
    public CourseModuleResponse getModule(Long userId) {
        requireUser(userId);

        Map<Long, Course> coursesById = new LinkedHashMap<>();

        List<Course> ownedCourses = courseRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        for (Course course : ownedCourses) {
            if (course == null || course.getId() == null) {
                continue;
            }
            coursesById.put(course.getId(), course);
        }

        List<CourseMembership> sharedMemberships =
                courseMembershipRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        for (CourseMembership membership : sharedMemberships) {
            if (membership == null) {
                continue;
            }
            Course sharedCourse = membership.getCourse();
            if (sharedCourse == null || sharedCourse.getDeletedAt() != null || sharedCourse.getId() == null) {
                continue;
            }
            coursesById.putIfAbsent(sharedCourse.getId(), sharedCourse);
        }

        List<Course> publicCourses = courseRepository.findByVisibilityIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc("public");
        for (Course publicCourse : publicCourses) {
            if (publicCourse == null || publicCourse.getId() == null) {
                continue;
            }
            coursesById.putIfAbsent(publicCourse.getId(), publicCourse);
        }

        List<CourseResponse> courses = coursesById.values().stream().map(this::toCourseResponse).toList();

        List<CourseExamItemResponse> availableExams = examService.listExams(userId)
                .stream()
                .map(this::toExamItem)
                .toList();

        return new CourseModuleResponse(courses, availableExams);
    }

    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        User user = requireUser(request.userId());
        String name = trimOrNull(request.name());

        if (name == null) {
            throw new BadRequestException("name es obligatorio");
        }

        Course course = new Course();
        course.setUser(user);
        course.setName(name);
        course.setDescription(trimOrNull(request.description()));
        course.setCoverImageData(trimOrNull(request.coverImageData()));
        course.setCode(resolveCourseCode(request.code(), name, null));
        String visibility = normalizeCourseVisibility(request.visibility(), true);
        String joinMode = normalizeJoinModeForVisibility(visibility, normalizeCourseJoinMode(request.joinMode(), true));
        course.setVisibility(visibility);
        course.setJoinMode(joinMode);
        course.setPriority(normalizeCoursePriority(request.priority(), true));
        course.setSortOrder(normalizeCourseSortOrder(request.sortOrder(), true));
        course = courseRepository.save(course);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse addCourseParticipant(Long courseId, CourseParticipantSaveRequest request) {
        Course course = requireCourseOwned(courseId, request.userId());

        String identifier = trimOrNull(request.identifier());
        if (identifier == null) {
            throw new BadRequestException("identifier es obligatorio");
        }
        User participantUser = findUserByIdentifier(identifier);
        Long ownerUserId = course.getUser() == null ? null : course.getUser().getId();
        Long participantUserId = participantUser.getId();
        if (ownerUserId != null && ownerUserId.equals(participantUserId)) {
            throw new BadRequestException("El creador del curso ya forma parte del curso");
        }

        String role = normalizeCourseMembershipRole(request.role(), true);
        CourseMembership membership = courseMembershipRepository
                .findByCourseIdAndUserId(course.getId(), participantUserId)
                .orElseGet(() -> {
                    CourseMembership created = new CourseMembership();
                    created.setCourse(course);
                    created.setUser(participantUser);
                    return created;
                });
        membership.setCourse(course);
        membership.setUser(participantUser);
        membership.setRole(role);
        membership.setDeletedAt(null);
        courseMembershipRepository.save(membership);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse updateCourseParticipantRole(
            Long courseId, Long participantUserId, CourseParticipantRoleUpdateRequest request) {
        Course course = requireCourseOwned(courseId, request.userId());
        Long ownerUserId = course.getUser() == null ? null : course.getUser().getId();
        if (ownerUserId != null && ownerUserId.equals(participantUserId)) {
            throw new BadRequestException("No se puede cambiar el rol del creador del curso");
        }

        CourseMembership membership = courseMembershipRepository
                .findByCourseIdAndUserIdAndDeletedAtIsNull(course.getId(), participantUserId)
                .orElseThrow(() -> new NotFoundException("Participante no encontrado"));
        membership.setRole(normalizeCourseMembershipRole(request.role(), true));
        courseMembershipRepository.save(membership);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse removeCourseParticipant(Long courseId, Long participantUserId, Long userId) {
        Course course = requireCourseOwned(courseId, userId);
        Long ownerUserId = course.getUser() == null ? null : course.getUser().getId();
        if (ownerUserId != null && ownerUserId.equals(participantUserId)) {
            throw new BadRequestException("No se puede quitar al creador del curso");
        }

        CourseMembership membership = courseMembershipRepository
                .findByCourseIdAndUserIdAndDeletedAtIsNull(course.getId(), participantUserId)
                .orElseThrow(() -> new NotFoundException("Participante no encontrado"));
        membership.setDeletedAt(LocalDateTime.now());
        courseMembershipRepository.save(membership);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse addCourseCompetency(Long courseId, CourseCompetencySaveRequest request) {
        Course course = requireCourseOwned(courseId, request.userId());
        String name = trimOrNull(request.name());
        if (name == null) {
            throw new BadRequestException("name es obligatorio");
        }

        CourseCompetency competency = new CourseCompetency();
        competency.setCourse(course);
        competency.setName(name);
        competency.setDescription(trimOrNull(request.description()));
        competency.setLevel(normalizeCourseCompetencyLevel(request.level(), true));
        competency.setSortOrder(normalizeCourseSortOrder(request.sortOrder(), true));
        courseCompetencyRepository.save(competency);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse updateCourseCompetency(Long courseId, Long competencyId, CourseCompetencySaveRequest request) {
        Course course = requireCourseOwned(courseId, request.userId());
        CourseCompetency competency = courseCompetencyRepository
                .findByIdAndCourseIdAndDeletedAtIsNull(competencyId, course.getId())
                .orElseThrow(() -> new NotFoundException("Competencia no encontrada"));

        String name = trimOrNull(request.name());
        if (name == null) {
            throw new BadRequestException("name es obligatorio");
        }
        competency.setName(name);
        competency.setDescription(trimOrNull(request.description()));
        competency.setLevel(normalizeCourseCompetencyLevel(request.level(), true));
        competency.setSortOrder(normalizeCourseSortOrder(request.sortOrder(), true));
        courseCompetencyRepository.save(competency);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse deleteCourseCompetency(Long courseId, Long competencyId, Long userId) {
        Course course = requireCourseOwned(courseId, userId);
        CourseCompetency competency = courseCompetencyRepository
                .findByIdAndCourseIdAndDeletedAtIsNull(competencyId, course.getId())
                .orElseThrow(() -> new NotFoundException("Competencia no encontrada"));
        competency.setDeletedAt(LocalDateTime.now());
        courseCompetencyRepository.save(competency);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse createCourseSession(Long courseId, CourseSessionCreateRequest request) {
        Course course = requireCourseOwned(courseId, request.userId());
        String sessionName = trimOrNull(request.name());

        if (sessionName == null) {
            throw new BadRequestException("name es obligatorio");
        }

        CourseSession session = new CourseSession();
        session.setCourse(course);
        session.setName(sessionName);
        session.setWeeklyContent(trimOrNull(request.weeklyContent()));
        courseSessionRepository.save(session);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse updateCourseSession(Long courseId, Long sessionId, CourseSessionUpdateRequest request) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, request.userId());
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }
        String sessionName = trimOrNull(request.name());
        if (sessionName == null) {
            throw new BadRequestException("name es obligatorio");
        }
        session.setName(sessionName);
        session.setWeeklyContent(trimOrNull(request.weeklyContent()));
        courseSessionRepository.save(session);
        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse addCourseWeek(Long courseId, Long sessionId, CourseWeekSaveRequest request) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, request.userId());
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }

        try {
            normalizeDeletedWeekOrdersForSession(session.getId());
        } catch (DataIntegrityViolationException ignored) {
            throw new BadRequestException("No se pudo normalizar el orden de semanas eliminadas");
        }
        Integer startingOrder = normalizeWeekOrderForCreate(request.weekOrder(), session.getId());
        String requestedWeekName = trimOrNull(request.name());
        String weekDescription = trimOrNull(request.description());

        String weekName = requestedWeekName == null ? "SEMANA " + startingOrder + ": Inicio" : requestedWeekName;
        CourseWeek week = new CourseWeek();
        week.setCourseSession(session);
        week.setWeekOrder(startingOrder);
        week.setName(weekName);
        week.setDescription(weekDescription);
        try {
            courseWeekRepository.save(week);
        } catch (DataIntegrityViolationException ignored) {
            throw new BadRequestException("weekOrder ya existe en esta sesion");
        }

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse updateCourseWeek(Long courseId, Long sessionId, Long weekId, CourseWeekSaveRequest request) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, request.userId());
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }

        CourseWeek week = courseWeekRepository
                .findByIdAndCourseSessionIdAndDeletedAtIsNull(weekId, session.getId())
                .orElseThrow(() -> new NotFoundException("Semana no encontrada"));

        Integer weekOrder = request.weekOrder() == null
                ? (week.getWeekOrder() == null ? 1 : week.getWeekOrder())
                : normalizeWeekOrder(request.weekOrder(), session.getId(), week.getId());
        String weekName = request.name() == null ? trimOrNull(week.getName()) : trimOrNull(request.name());
        if (weekName == null) {
            weekName = "SEMANA " + weekOrder + ": Inicio";
        }

        week.setWeekOrder(weekOrder);
        week.setName(weekName);
        week.setDescription(trimOrNull(request.description()));
        try {
            courseWeekRepository.save(week);
        } catch (DataIntegrityViolationException ignored) {
            throw new BadRequestException("weekOrder ya existe en esta sesion");
        }

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse deleteCourseWeek(Long courseId, Long sessionId, Long weekId, Long userId) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, userId);
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }

        CourseWeek week = courseWeekRepository
                .findByIdAndCourseSessionIdAndDeletedAtIsNull(weekId, session.getId())
                .orElseThrow(() -> new NotFoundException("Semana no encontrada"));

        LocalDateTime now = LocalDateTime.now();
        week.setDeletedAt(now);
        week.setWeekOrder(resolveNextDeletedWeekOrder(session.getId(), week.getId()));
        courseWeekRepository.save(week);

        List<CourseSessionContent> contents = courseSessionContentRepository
                .findByCourseWeekIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(week.getId());
        for (CourseSessionContent content : contents) {
            content.setDeletedAt(now);
        }
        if (!contents.isEmpty()) {
            courseSessionContentRepository.saveAll(contents);
        }

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse addCourseSessionContent(
            Long courseId, Long sessionId, CourseSessionContentSaveRequest request) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, request.userId());
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }
        String type = normalizeContentType(request.type());
        String title = trimOrNull(request.title());
        if (title == null) {
            throw new BadRequestException("title es obligatorio");
        }

        CourseSessionContent content = new CourseSessionContent();
        content.setCourseSession(session);
        CourseWeek targetWeek = resolveWeekForContent(session, request.weekId());
        content.setCourseWeek(targetWeek);
        content.setContentOrder(resolveNextWeekContentOrder(targetWeek.getId()));
        content.setType(type);
        content.setTitle(title);
        applyContentData(
                content,
                type,
                request.externalLink(),
                request.fileName(),
                request.fileData(),
                request.sourceExamId(),
                request.userId());
        courseSessionContentRepository.save(content);

        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse updateCourseSessionContent(
            Long courseId, Long sessionId, Long contentId, CourseSessionContentSaveRequest request) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, request.userId());
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }
        CourseSessionContent content = courseSessionContentRepository
                .findByIdAndCourseSessionIdAndDeletedAtIsNull(contentId, session.getId())
                .orElseThrow(() -> new NotFoundException("Contenido de sesion no encontrado"));

        String type = request.type() == null ? trimOrNull(content.getType()) : normalizeContentType(request.type());
        if (type == null) {
            throw new BadRequestException("type es obligatorio");
        }

        String title = request.title() == null ? trimOrNull(content.getTitle()) : trimOrNull(request.title());
        if (title == null) {
            throw new BadRequestException("title es obligatorio");
        }

        content.setType(type);
        content.setTitle(title);
        if (request.weekId() != null || content.getCourseWeek() == null) {
            CourseWeek previousWeek = content.getCourseWeek();
            CourseWeek targetWeek = resolveWeekForContent(session, request.weekId());
            content.setCourseWeek(targetWeek);
            if (previousWeek == null || previousWeek.getId() == null || !previousWeek.getId().equals(targetWeek.getId())) {
                content.setContentOrder(resolveNextWeekContentOrder(targetWeek.getId()));
                normalizeWeekContentOrders(previousWeek == null ? null : previousWeek.getId());
            }
        }
        Long resolvedSourceExamId = request.sourceExamId() == null
                ? (content.getSourceExam() == null ? null : content.getSourceExam().getId())
                : request.sourceExamId();
        applyContentData(
                content,
                type,
                request.externalLink() == null ? content.getExternalLink() : request.externalLink(),
                request.fileName() == null ? content.getFileName() : request.fileName(),
                request.fileData() == null ? content.getFileData() : request.fileData(),
                resolvedSourceExamId,
                request.userId());
        courseSessionContentRepository.save(content);
        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse deleteCourseSession(Long courseId, Long sessionId, Long userId) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, userId);
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }
        LocalDateTime now = LocalDateTime.now();
        session.setDeletedAt(now);
        courseSessionRepository.save(session);

        List<CourseWeek> weeks =
                courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(session.getId());
        for (CourseWeek week : weeks) {
            week.setDeletedAt(now);
        }
        if (!weeks.isEmpty()) {
            courseWeekRepository.saveAll(weeks);
        }

        List<CourseSessionContent> contents =
                courseSessionContentRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(session.getId());
        for (CourseSessionContent content : contents) {
            content.setDeletedAt(now);
        }
        if (!contents.isEmpty()) {
            courseSessionContentRepository.saveAll(contents);
        }
        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse deleteCourseSessionContent(Long courseId, Long sessionId, Long contentId, Long userId) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, userId);
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }
        CourseSessionContent content = courseSessionContentRepository
                .findByIdAndCourseSessionIdAndDeletedAtIsNull(contentId, session.getId())
                .orElseThrow(() -> new NotFoundException("Contenido de sesion no encontrado"));
        Long weekId = content.getCourseWeek() == null ? null : content.getCourseWeek().getId();
        content.setDeletedAt(LocalDateTime.now());
        courseSessionContentRepository.save(content);
        normalizeWeekContentOrders(weekId);
        return toCourseResponse(course);
    }

    @Transactional
    public CourseResponse reorderWeekContents(
            Long courseId, Long sessionId, Long weekId, CourseWeekContentReorderRequest request) {
        CourseSession session = requireCourseSessionOwnedByUser(sessionId, request.userId());
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }

        CourseWeek week = courseWeekRepository
                .findByIdAndCourseSessionIdAndDeletedAtIsNull(weekId, session.getId())
                .orElseThrow(() -> new NotFoundException("Semana no encontrada"));

        List<CourseSessionContent> weekContents =
                courseSessionContentRepository.findByCourseWeekIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(week.getId());
        if (weekContents.isEmpty()) {
            return toCourseResponse(course);
        }

        List<Long> orderedIds = request.orderedContentIds().stream().filter(id -> id != null && id > 0).distinct().toList();
        if (orderedIds.size() != weekContents.size()) {
            throw new BadRequestException("orderedContentIds no coincide con la cantidad de contenidos de la semana");
        }

        Set<Long> existingIds = new LinkedHashSet<>();
        for (CourseSessionContent content : weekContents) {
            if (content.getId() != null) {
                existingIds.add(content.getId());
            }
        }
        if (!existingIds.equals(new LinkedHashSet<>(orderedIds))) {
            throw new BadRequestException("orderedContentIds debe contener exactamente los contenidos de la semana");
        }

        Map<Long, CourseSessionContent> byId = new LinkedHashMap<>();
        for (CourseSessionContent content : weekContents) {
            byId.put(content.getId(), content);
        }

        int order = 1;
        for (Long contentId : orderedIds) {
            CourseSessionContent content = byId.get(contentId);
            if (content == null) {
                throw new BadRequestException("orderedContentIds contiene contenido invalido");
            }
            content.setContentOrder(order++);
        }
        courseSessionContentRepository.saveAll(weekContents);
        return toCourseResponse(course);
    }

    @Transactional
    public CourseSessionContentPracticeStartResponse startCourseSessionContentPractice(
            Long courseId, Long sessionId, Long contentId, Long userId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, userId);
        coursePracticeWriteService.ensureParticipantAnchoredExamMembership(sourceExam, userId);

        String examName = trimOrNull(sourceExam.getName());
        if (examName == null) {
            examName = "examen";
        }
        return new CourseSessionContentPracticeStartResponse(sourceExam.getId(), examName);
    }

    @Transactional(readOnly = true)
    public ExamSummaryResponse getCourseSessionContentExamSummary(
            Long courseId, Long sessionId, Long contentId, Long userId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, userId);
        return examService.getExamSummary(sourceExam.getId(), userId);
    }

    @Transactional
    public ExamSummaryResponse renameCourseSessionContentExam(
            Long courseId, Long sessionId, Long contentId, ExamRenameRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examService.renameExam(sourceExam.getId(), request);
    }

    @Transactional
    public void deleteCourseSessionContentExam(Long courseId, Long sessionId, Long contentId, Long userId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, userId);
        examService.deleteExam(sourceExam.getId(), userId);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getCourseSessionContentExamQuestions(
            Long courseId, Long sessionId, Long contentId, Long userId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, userId);
        return examService.getManualExam(sourceExam.getId(), userId);
    }

    @Transactional
    public QuestionResponse addCourseSessionContentExamQuestion(
            Long courseId, Long sessionId, Long contentId, ManualQuestionUpsertRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examService.addManualQuestion(sourceExam.getId(), request);
    }

    @Transactional
    public QuestionResponse updateCourseSessionContentExamQuestion(
            Long courseId, Long sessionId, Long contentId, Long questionId, ManualQuestionUpsertRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examService.updateManualQuestion(sourceExam.getId(), questionId, request);
    }

    @Transactional(readOnly = true)
    public List<ExamParticipantResponse> getCourseSessionContentExamParticipants(
            Long courseId, Long sessionId, Long contentId, Long userId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, userId);
        return examService.listParticipants(sourceExam.getId(), userId);
    }

    @Transactional
    public void updateCourseSessionContentExamParticipantPermissions(
            Long courseId,
            Long sessionId,
            Long contentId,
            Long participantUserId,
            ExamParticipantPermissionUpdateRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.requesterUserId());
        examService.updateExamParticipantPermissions(sourceExam.getId(), participantUserId, request);
    }

    @Transactional
    public void removeCourseSessionContentExamParticipant(
            Long courseId, Long sessionId, Long contentId, Long participantUserId, Long requesterUserId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, requesterUserId);
        examService.removeExamParticipant(sourceExam.getId(), participantUserId, requesterUserId);
    }

    @Transactional(readOnly = true)
    public ExamPracticeSettingsResponse getCourseSessionContentIndividualPracticeSettings(
            Long courseId, Long sessionId, Long contentId, Long userId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, userId);
        return examService.getIndividualPracticeSettings(sourceExam.getId(), userId);
    }

    @Transactional
    public ExamPracticeSettingsResponse updateCourseSessionContentIndividualPracticeSettings(
            Long courseId,
            Long sessionId,
            Long contentId,
            ExamIndividualPracticeSettingsRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examService.updateIndividualPracticeSettings(sourceExam.getId(), request);
    }

    @Transactional
    public ExamSummaryResponse updateCourseSessionContentPracticeSettings(
            Long courseId, Long sessionId, Long contentId, ExamPracticeSettingsRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examService.updatePracticeSettings(sourceExam.getId(), request);
    }

    @Transactional
    public ExamPracticeStartResponse startCourseSessionContentExamPracticeAttempt(
            Long courseId, Long sessionId, Long contentId, Long userId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, userId);
        return coursePracticeWriteService.startAnchoredExamPracticeAttempt(sourceExam, userId);
    }

    @Transactional
    public ExamGroupStateResponse joinCourseSessionContentGroupPractice(
            Long courseId, Long sessionId, Long contentId, ExamGroupJoinRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return coursePracticeWriteService.joinAnchoredGroupPractice(sourceExam, request);
    }

    @Transactional
    public ExamGroupStateResponse createCourseSessionContentGroupPractice(
            Long courseId, Long sessionId, Long contentId, ExamGroupJoinRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return coursePracticeWriteService.createAnchoredGroupPractice(sourceExam, request);
    }

    @Transactional
    public ExamGroupStateResponse startCourseSessionContentGroupPractice(
            Long courseId, Long sessionId, Long contentId, ExamGroupStartRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examGroupPracticeService.start(sourceExam.getId(), request);
    }

    @Transactional(readOnly = true)
    public ExamGroupStateResponse getCourseSessionContentGroupPracticeState(
            Long courseId, Long sessionId, Long contentId, Long sessionGroupId, Long userId) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, userId);
        return examGroupPracticeService.state(sourceExam.getId(), sessionGroupId, userId);
    }

    @Transactional
    public ExamGroupStateResponse answerCourseSessionContentGroupPractice(
            Long courseId, Long sessionId, Long contentId, ExamGroupAnswerRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examGroupPracticeService.answer(sourceExam.getId(), request);
    }

    @Transactional
    public ExamGroupStateResponse nextCourseSessionContentGroupPractice(
            Long courseId, Long sessionId, Long contentId, ExamGroupAdvanceRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examGroupPracticeService.next(sourceExam.getId(), request);
    }

    @Transactional
    public ExamGroupStateResponse closeCourseSessionContentGroupPractice(
            Long courseId, Long sessionId, Long contentId, ExamGroupAdvanceRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examGroupPracticeService.close(sourceExam.getId(), request);
    }

    @Transactional
    public ExamGroupStateResponse restartCourseSessionContentGroupPractice(
            Long courseId, Long sessionId, Long contentId, ExamGroupAdvanceRequest request) {
        Exam sourceExam = requireCourseSessionContentExam(courseId, sessionId, contentId, request.userId());
        return examGroupPracticeService.restart(sourceExam.getId(), request);
    }

    @Transactional
    public CourseResponse setCourseExams(Long courseId, CourseSetExamsRequest request) {
        Course course = requireCourseOwned(courseId, request.userId());
        List<Long> requestedExamIds = normalizeExamIds(request.examIds());

        Map<Long, Exam> examsById = new LinkedHashMap<>();
        if (!requestedExamIds.isEmpty()) {
            List<Exam> exams = examRepository.findByIdInAndUserIdAndDeletedAtIsNull(requestedExamIds, request.userId());
            if (exams.size() != requestedExamIds.size()) {
                throw new BadRequestException("Uno o mas examenes no existen o no pertenecen al usuario");
            }
            examsById = exams.stream().collect(
                    LinkedHashMap::new,
                    (map, exam) -> map.put(exam.getId(), exam),
                    Map::putAll);
        }

        courseExamRepository.deleteByCourseId(course.getId());

        if (!requestedExamIds.isEmpty()) {
            List<CourseExam> associations = new ArrayList<>();
            for (Long examId : requestedExamIds) {
                Exam exam = examsById.get(examId);
                if (exam == null) {
                    throw new BadRequestException("Examen invalido para asociar al curso");
                }
                CourseExam association = new CourseExam();
                association.setCourse(course);
                association.setExam(exam);
                associations.add(association);
            }
            courseExamRepository.saveAll(associations);
        }

        return toCourseResponse(course);
    }

    @Transactional
    public CourseJoinResponse joinPublicCourse(Long courseId, Long userId) {
        User user = requireUser(userId);
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new NotFoundException("Curso no encontrado"));

        User owner = course.getUser();
        Long ownerUserId = owner == null ? null : owner.getId();
        if (ownerUserId != null && ownerUserId.equals(userId)) {
            return new CourseJoinResponse(course.getId(), "already_member", "Ya perteneces a este curso.");
        }

        if (!"public".equals(normalizeCourseVisibility(course.getVisibility(), false))) {
            throw new BadRequestException("Este curso no permite unirse directamente");
        }

        CourseMembership activeMembership = courseMembershipRepository
                .findByCourseIdAndUserIdAndDeletedAtIsNull(courseId, userId)
                .orElse(null);
        if (activeMembership != null) {
            String existingRole = normalizeCourseMembershipRole(activeMembership.getRole(), false);
            if ("pending".equals(existingRole)) {
                return new CourseJoinResponse(
                        course.getId(),
                        "already_requested",
                        "Tu solicitud ya fue enviada. Espera la aprobacion del creador.");
            }
            return new CourseJoinResponse(course.getId(), "already_member", "Ya perteneces a este curso.");
        }

        String joinMode = normalizeCourseJoinMode(course.getJoinMode(), false);
        boolean requiresApproval = "request".equals(joinMode);

        CourseMembership membership = courseMembershipRepository
                .findByCourseIdAndUserId(courseId, userId)
                .orElseGet(CourseMembership::new);
        membership.setCourse(course);
        membership.setUser(user);
        membership.setDeletedAt(null);
        membership.setRole(requiresApproval ? "pending" : "viewer");
        courseMembershipRepository.save(membership);

        if (requiresApproval) {
            return new CourseJoinResponse(
                    course.getId(),
                    "requested",
                    "Solicitud enviada. El creador debe aprobar tu ingreso.");
        }
        return new CourseJoinResponse(course.getId(), "joined", "Te uniste al curso correctamente.");
    }

    @Transactional
    public CourseJoinResponse acceptCourseJoinRequest(Long courseId, Long requesterUserId, Long ownerUserId) {
        Course course = requireCourseOwned(courseId, ownerUserId);
        CourseMembership membership = courseMembershipRepository
                .findByCourseIdAndUserIdAndDeletedAtIsNull(course.getId(), requesterUserId)
                .orElseThrow(() -> new NotFoundException("Solicitud de ingreso no encontrada"));

        String currentRole = normalizeCourseMembershipRole(membership.getRole(), false);
        if (!"pending".equals(currentRole)) {
            return new CourseJoinResponse(course.getId(), "already_processed", "La solicitud ya fue procesada.");
        }

        membership.setRole("viewer");
        membership.setDeletedAt(null);
        courseMembershipRepository.save(membership);
        return new CourseJoinResponse(course.getId(), "accepted", "Solicitud aceptada.");
    }

    @Transactional
    public CourseJoinResponse rejectCourseJoinRequest(Long courseId, Long requesterUserId, Long ownerUserId) {
        Course course = requireCourseOwned(courseId, ownerUserId);
        CourseMembership membership = courseMembershipRepository
                .findByCourseIdAndUserIdAndDeletedAtIsNull(course.getId(), requesterUserId)
                .orElseThrow(() -> new NotFoundException("Solicitud de ingreso no encontrada"));

        String currentRole = normalizeCourseMembershipRole(membership.getRole(), false);
        if (!"pending".equals(currentRole)) {
            return new CourseJoinResponse(course.getId(), "already_processed", "La solicitud ya fue procesada.");
        }

        membership.setDeletedAt(LocalDateTime.now());
        courseMembershipRepository.save(membership);
        return new CourseJoinResponse(course.getId(), "rejected", "Solicitud rechazada.");
    }

    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        Course course = requireCourseOwned(courseId, request.userId());
        String name = trimOrNull(request.name());

        if (name == null) {
            throw new BadRequestException("name es obligatorio");
        }

        course.setName(name);
        course.setDescription(trimOrNull(request.description()));
        if (request.coverImageData() != null) {
            course.setCoverImageData(trimOrNull(request.coverImageData()));
        }
        String visibility = request.visibility() == null
                ? normalizeCourseVisibility(course.getVisibility(), false)
                : normalizeCourseVisibility(request.visibility(), true);
        course.setVisibility(visibility);
        String joinMode = request.joinMode() == null
                ? normalizeCourseJoinMode(course.getJoinMode(), false)
                : normalizeCourseJoinMode(request.joinMode(), true);
        course.setJoinMode(normalizeJoinModeForVisibility(visibility, joinMode));
        String priority = request.priority() == null
                ? normalizeCoursePriority(course.getPriority(), false)
                : normalizeCoursePriority(request.priority(), true);
        course.setPriority(priority);
        Integer sortOrder = request.sortOrder() == null
                ? normalizeCourseSortOrder(course.getSortOrder(), false)
                : normalizeCourseSortOrder(request.sortOrder(), true);
        course.setSortOrder(sortOrder);
        String code = request.code() == null
                ? normalizeCourseCode(course.getCode())
                : resolveCourseCode(request.code(), name, course.getId());
        if (code == null) {
            code = resolveCourseCode(null, name, course.getId());
        }
        course.setCode(code);
        course = courseRepository.save(course);
        return toCourseResponse(course);
    }

    @Transactional
    public void deleteCourse(Long courseId, Long userId) {
        Course course = requireCourseOwned(courseId, userId);
        course.setDeletedAt(LocalDateTime.now());
        courseRepository.save(course);
        courseExamRepository.deleteByCourseId(course.getId());
    }

    private Course requireCourseOwned(Long courseId, Long userId) {
        requireUser(userId);
        return courseRepository.findByIdAndUserIdAndDeletedAtIsNull(courseId, userId)
                .orElseThrow(() -> new NotFoundException("Curso no encontrado"));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private User findUserByIdentifier(String identifier) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new NotFoundException("Usuario destino no encontrado"));
    }

    private CourseSession requireCourseSessionOwnedByUser(Long sessionId, Long userId) {
        requireUser(userId);
        return courseSessionRepository
                .findByIdAndCourseUserIdAndDeletedAtIsNull(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Sesion no encontrada"));
    }

    private boolean hasCourseAccess(Course course, Long userId) {
        User owner = course.getUser();
        if (owner != null && owner.getId() != null && owner.getId().equals(userId)) {
            return true;
        }
        return courseMembershipRepository
                .findByCourseIdAndUserIdAndDeletedAtIsNull(course.getId(), userId)
                .map(membership -> !"pending".equals(normalizeCourseMembershipRole(membership.getRole(), false)))
                .orElse(false);
    }

    private List<Long> normalizeExamIds(List<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        return examIds.stream()
                .filter(value -> value != null && value > 0)
                .map(Long::longValue)
                .distinct()
                .toList();
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCourseCode(String rawCode) {
        String value = trimOrNull(rawCode);
        if (value == null) {
            return null;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", "-");
        normalized = normalized.replaceAll("[^A-Z0-9_-]", "");
        normalized = normalized.replaceAll("[-_]{2,}", "-");
        normalized = normalized.replaceAll("^[-_]+|[-_]+$", "");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40);
        }
        return normalized;
    }

    private String normalizeCourseVisibility(String rawVisibility, boolean strict) {
        String value = trimOrNull(rawVisibility);
        if (value == null) {
            return "public";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("public") || normalized.equals("private")) {
            return normalized;
        }
        if (strict) {
            throw new BadRequestException("visibility debe ser public o private");
        }
        return "public";
    }

    private String normalizeCoursePriority(String rawPriority, boolean strict) {
        String value = trimOrNull(rawPriority);
        if (value == null) {
            return "important";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("very_important")
                || normalized.equals("important")
                || normalized.equals("low_important")
                || normalized.equals("optional")) {
            return normalized;
        }
        if (strict) {
            throw new BadRequestException("priority debe ser very_important, important, low_important u optional");
        }
        return "important";
    }

    private String normalizeCourseJoinMode(String rawJoinMode, boolean strict) {
        String value = trimOrNull(rawJoinMode);
        if (value == null) {
            return "open";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("open") || normalized.equals("request")) {
            return normalized;
        }
        if (strict) {
            throw new BadRequestException("joinMode debe ser open o request");
        }
        return "open";
    }

    private String normalizeJoinModeForVisibility(String visibility, String joinMode) {
        if ("private".equals(visibility)) {
            return "open";
        }
        return joinMode;
    }

    private Integer normalizeCourseSortOrder(Integer rawSortOrder, boolean strict) {
        if (rawSortOrder == null) {
            return 0;
        }
        if (rawSortOrder < 0) {
            if (strict) {
                throw new BadRequestException("sortOrder debe ser mayor o igual a 0");
            }
            return 0;
        }
        return rawSortOrder;
    }

    private String normalizeCourseMembershipRole(String rawRole, boolean strict) {
        String value = trimOrNull(rawRole);
        if (value == null) {
            return "viewer";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("viewer")
                || normalized.equals("editor")
                || normalized.equals("assistant")
                || normalized.equals("pending")) {
            return normalized;
        }
        if (strict) {
            throw new BadRequestException("role debe ser viewer, editor, assistant o pending");
        }
        return "viewer";
    }

    private String normalizeCourseCompetencyLevel(String rawLevel, boolean strict) {
        String value = trimOrNull(rawLevel);
        if (value == null) {
            return "basico";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("basico") || normalized.equals("intermedio") || normalized.equals("avanzado")) {
            return normalized;
        }
        if (strict) {
            throw new BadRequestException("level debe ser basico, intermedio o avanzado");
        }
        return "basico";
    }

    private String resolveCourseCode(String rawCode, String courseName, Long excludingCourseId) {
        String normalizedProvidedCode = normalizeCourseCode(rawCode);
        if (normalizedProvidedCode != null) {
            if (courseCodeExists(normalizedProvidedCode, excludingCourseId)) {
                throw new BadRequestException("El codigo del curso ya existe");
            }
            return normalizedProvidedCode;
        }

        String rawValue = trimOrNull(rawCode);
        if (rawValue != null) {
            throw new BadRequestException("El codigo del curso es invalido");
        }

        String basePrefix = normalizeCourseCode(courseName);
        if (basePrefix == null) {
            basePrefix = "CURSO";
        } else if (basePrefix.length() > 12) {
            basePrefix = basePrefix.substring(0, 12);
        }

        for (int attempt = 0; attempt < 40; attempt++) {
            long nowPart = Math.abs(System.currentTimeMillis() + attempt);
            long nanoPart = Math.abs(System.nanoTime() % 10000L);
            String candidate = basePrefix + "-" + Long.toString(nowPart, 36).toUpperCase(Locale.ROOT)
                    + String.format(Locale.ROOT, "%04d", nanoPart);
            if (candidate.length() > 40) {
                candidate = candidate.substring(0, 40);
            }
            if (!courseCodeExists(candidate, excludingCourseId)) {
                return candidate;
            }
        }

        throw new BadRequestException("No se pudo generar un codigo unico para el curso");
    }

    private boolean courseCodeExists(String code, Long excludingCourseId) {
        if (excludingCourseId == null) {
            return courseRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code);
        }
        return courseRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, excludingCourseId);
    }

    private CourseResponse toCourseResponse(Course course) {
        List<CourseSessionItemResponse> sessions = courseSessionRepository
                .findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(course.getId())
                .stream()
                .map(this::toCourseSessionItem)
                .toList();

        List<CourseExamItemResponse> exams = courseExamRepository.findByCourseIdOrderByCreatedAtAsc(course.getId()).stream()
                .map(CourseExam::getExam)
                .filter(exam -> exam != null && exam.getDeletedAt() == null)
                .map(this::toExamItem)
                .toList();

        List<CourseParticipantItemResponse> participants = buildCourseParticipants(course);
        List<CourseCompetencyItemResponse> competencies = buildCourseCompetencies(course);
        List<Long> resolvedExamIds = resolveCourseExamIds(exams, sessions);
        List<CourseGradeItemResponse> grades = buildCourseGrades(participants, resolvedExamIds);

        String normalizedName = course.getName() == null ? "" : course.getName().trim();
        if (normalizedName.isEmpty()) {
            normalizedName = "curso";
        }
        String normalizedDescription = trimOrNull(course.getDescription());
        String normalizedCoverImageData = trimOrNull(course.getCoverImageData());
        String normalizedCode = normalizeCourseCode(course.getCode());
        if (normalizedCode == null) {
            Long courseId = course.getId();
            normalizedCode = courseId == null ? "CURSO-SIN-CODIGO" : "CURSO-" + courseId;
        }
        String normalizedVisibility = normalizeCourseVisibility(course.getVisibility(), false);
        String normalizedJoinMode = normalizeJoinModeForVisibility(
            normalizedVisibility,
            normalizeCourseJoinMode(course.getJoinMode(), false));
        String normalizedPriority = normalizeCoursePriority(course.getPriority(), false);
        Integer normalizedSortOrder = normalizeCourseSortOrder(course.getSortOrder(), false);
        Long ownerUserId = course.getUser() == null ? null : course.getUser().getId();

        return new CourseResponse(
                course.getId(),
                normalizedName,
                normalizedDescription,
                normalizedCoverImageData,
                normalizedCode,
                normalizedVisibility,
                normalizedJoinMode,
                normalizedPriority,
                normalizedSortOrder,
                ownerUserId,
                sessions,
                exams,
                participants,
                grades,
                competencies,
                course.getCreatedAt());
    }

    private List<Long> resolveCourseExamIds(List<CourseExamItemResponse> exams, List<CourseSessionItemResponse> sessions) {
        Set<Long> examIds = new LinkedHashSet<>();
        for (CourseExamItemResponse exam : exams) {
            if (exam != null && exam.id() != null && exam.id() > 0) {
                examIds.add(exam.id());
            }
        }
        for (CourseSessionItemResponse session : sessions) {
            if (session == null || session.contents() == null) {
                continue;
            }
            for (CourseSessionContentItemResponse content : session.contents()) {
                if (content == null) {
                    continue;
                }
                String contentType = trimOrNull(content.type());
                if (contentType == null || !contentType.equalsIgnoreCase("exam")) {
                    continue;
                }
                Long sourceExamId = content.sourceExamId();
                if (sourceExamId != null && sourceExamId > 0) {
                    examIds.add(sourceExamId);
                }
            }
        }
        return new ArrayList<>(examIds);
    }

    private List<CourseParticipantItemResponse> buildCourseParticipants(Course course) {
        List<CourseParticipantItemResponse> participants = new ArrayList<>();

        User owner = course.getUser();
        if (owner != null && owner.getId() != null) {
            participants.add(toCourseParticipantItem(owner, null, true, course.getCreatedAt()));
        }

        List<CourseMembership> memberships =
                courseMembershipRepository.findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(course.getId());
        Long ownerUserId = owner == null ? null : owner.getId();
        for (CourseMembership membership : memberships) {
            if (membership == null) {
                continue;
            }
            User participantUser = membership.getUser();
            if (participantUser == null || participantUser.getId() == null) {
                continue;
            }
            if (ownerUserId != null && ownerUserId.equals(participantUser.getId())) {
                continue;
            }
            participants.add(toCourseParticipantItem(participantUser, membership, false, membership.getCreatedAt()));
        }

        return participants;
    }

    private CourseParticipantItemResponse toCourseParticipantItem(
            User user, CourseMembership membership, boolean owner, LocalDateTime joinedAt) {
        String name = trimOrNull(user.getName());
        String username = trimOrNull(user.getUsername());
        String email = trimOrNull(user.getEmail());
        if (username == null) {
            Long userId = user.getId();
            username = userId == null ? "user" : "user" + userId;
        }
        if (name == null) {
            name = username;
        }
        if (email == null) {
            email = "sin-correo@local";
        }
        String role = owner
                ? "owner"
                : normalizeCourseMembershipRole(membership == null ? null : membership.getRole(), false);
        Long membershipId = membership == null ? null : membership.getId();
        return new CourseParticipantItemResponse(membershipId, user.getId(), name, username, email, role, owner, joinedAt);
    }

    private List<CourseCompetencyItemResponse> buildCourseCompetencies(Course course) {
        return courseCompetencyRepository
                .findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(course.getId())
                .stream()
                .map(this::toCourseCompetencyItem)
                .toList();
    }

    private CourseCompetencyItemResponse toCourseCompetencyItem(CourseCompetency competency) {
        return new CourseCompetencyItemResponse(
                competency.getId(),
                trimOrNull(competency.getName()),
                trimOrNull(competency.getDescription()),
                normalizeCourseCompetencyLevel(competency.getLevel(), false),
                normalizeCourseSortOrder(competency.getSortOrder(), false),
                competency.getCreatedAt());
    }

    private List<CourseGradeItemResponse> buildCourseGrades(
            List<CourseParticipantItemResponse> participants, List<Long> resolvedExamIds) {
        if (participants.isEmpty()) {
            return List.of();
        }

        Map<Long, GradeAccumulator> gradeByUserId = new LinkedHashMap<>();
        for (CourseParticipantItemResponse participant : participants) {
            if (participant == null || participant.userId() == null || gradeByUserId.containsKey(participant.userId())) {
                continue;
            }
            gradeByUserId.put(participant.userId(), new GradeAccumulator(participant));
        }

        if (!gradeByUserId.isEmpty() && !resolvedExamIds.isEmpty()) {
            List<Long> validExamIds = resolvedExamIds.stream().filter(id -> id != null && id > 0).distinct().toList();
            if (!validExamIds.isEmpty()) {
                List<Long> participantUserIds = new ArrayList<>(gradeByUserId.keySet());
                List<ExamAttempt> attempts = examAttemptRepository.findByExamIdInAndUserIdIn(validExamIds, participantUserIds);
                for (ExamAttempt attempt : attempts) {
                    if (attempt == null || attempt.getUser() == null || attempt.getUser().getId() == null) {
                        continue;
                    }
                    GradeAccumulator accumulator = gradeByUserId.get(attempt.getUser().getId());
                    if (accumulator == null) {
                        continue;
                    }
                    accumulator.registerAttempt(attempt, calculateAttemptScorePercent(attempt));
                }
            }
        }

        return gradeByUserId.values().stream().map(GradeAccumulator::toResponse).toList();
    }

    private double calculateAttemptScorePercent(ExamAttempt attempt) {
        int totalPoints = attempt.getTotalPoints() == null ? 0 : attempt.getTotalPoints();
        int scoredPoints = attempt.getScoredPoints() == null ? 0 : attempt.getScoredPoints();
        double result;
        if (totalPoints > 0) {
            result = (scoredPoints * 100.0d) / totalPoints;
        } else {
            int totalQuestions = attempt.getTotalQuestions() == null ? 0 : attempt.getTotalQuestions();
            int correctCount = attempt.getCorrectCount() == null ? 0 : attempt.getCorrectCount();
            result = totalQuestions > 0 ? (correctCount * 100.0d) / totalQuestions : 0.0d;
        }
        if (result < 0.0d) {
            result = 0.0d;
        }
        if (result > 100.0d) {
            result = 100.0d;
        }
        return roundScoreValue(result);
    }

    private static double roundScoreValue(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private static Double roundNullable(Double value) {
        return value == null ? null : roundScoreValue(value);
    }

    private static LocalDateTime resolveAttemptDate(ExamAttempt attempt) {
        if (attempt.getFinishedAt() != null) {
            return attempt.getFinishedAt();
        }
        if (attempt.getUpdatedAt() != null) {
            return attempt.getUpdatedAt();
        }
        return attempt.getCreatedAt();
    }

    private static final class GradeAccumulator {
        private final CourseParticipantItemResponse participant;
        private int attemptsCount = 0;
        private double scoreSum = 0.0d;
        private Double bestScore = null;
        private Double lastScore = null;
        private LocalDateTime lastAttemptAt = null;

        private GradeAccumulator(CourseParticipantItemResponse participant) {
            this.participant = participant;
        }

        private void registerAttempt(ExamAttempt attempt, double scorePercent) {
            attemptsCount += 1;
            scoreSum += scorePercent;
            if (bestScore == null || scorePercent > bestScore) {
                bestScore = scorePercent;
            }

            LocalDateTime attemptDate = resolveAttemptDate(attempt);
            if (attemptDate == null) {
                if (lastScore == null) {
                    lastScore = scorePercent;
                }
                return;
            }
            if (lastAttemptAt == null || attemptDate.isAfter(lastAttemptAt)) {
                lastAttemptAt = attemptDate;
                lastScore = scorePercent;
            } else if (lastScore == null) {
                lastScore = scorePercent;
            }
        }

        private CourseGradeItemResponse toResponse() {
            Double averageScore = attemptsCount > 0 ? roundScoreValue(scoreSum / attemptsCount) : null;
            return new CourseGradeItemResponse(
                    participant.userId(),
                    participant.name(),
                    participant.username(),
                    participant.email(),
                    attemptsCount,
                    averageScore,
                    roundNullable(bestScore),
                    roundNullable(lastScore),
                    lastAttemptAt);
        }
    }

    private CourseSessionItemResponse toCourseSessionItem(CourseSession session) {
        String normalizedSessionName = session.getName() == null ? "" : session.getName().trim();
        if (normalizedSessionName.isEmpty()) {
            normalizedSessionName = "Sesion";
        }
        String normalizedWeeklyContent = trimOrNull(session.getWeeklyContent());
        List<CourseSessionContent> rawContents = courseSessionContentRepository
                .findByCourseSessionIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(session.getId())
                .stream()
                .toList();
        List<CourseSessionContentItemResponse> contents = rawContents
                .stream()
                .map(this::toCourseSessionContentItem)
                .toList();
        Map<Long, List<CourseSessionContentItemResponse>> contentsByWeekId = new LinkedHashMap<>();
        for (CourseSessionContentItemResponse content : contents) {
            Long weekId = content.weekId();
            if (weekId == null) {
                continue;
            }
            contentsByWeekId.computeIfAbsent(weekId, ignored -> new ArrayList<>()).add(content);
        }
        List<CourseWeekItemResponse> weeks = courseWeekRepository
                .findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(session.getId())
                .stream()
                .map(week -> new CourseWeekItemResponse(
                        week.getId(),
                        week.getWeekOrder(),
                        trimOrNull(week.getName()),
                        trimOrNull(week.getDescription()),
                        contentsByWeekId.getOrDefault(week.getId(), List.of()),
                        week.getCreatedAt()))
                .toList();
        return new CourseSessionItemResponse(
                session.getId(),
                normalizedSessionName,
                normalizedWeeklyContent,
                weeks,
                contents,
                session.getCreatedAt());
    }

    private CourseSessionContentItemResponse toCourseSessionContentItem(CourseSessionContent content) {
        Exam sourceExam = content.getSourceExam();
        CourseWeek week = content.getCourseWeek();
        return new CourseSessionContentItemResponse(
                content.getId(),
                trimOrNull(content.getType()),
                trimOrNull(content.getTitle()),
                trimOrNull(content.getExternalLink()),
                trimOrNull(content.getFileName()),
                trimOrNull(content.getFileData()),
                week == null ? null : week.getId(),
                week == null ? null : week.getWeekOrder(),
                week == null ? null : trimOrNull(week.getName()),
                sourceExam == null ? null : sourceExam.getId(),
                sourceExam == null ? null : trimOrNull(sourceExam.getName()),
                content.getContentOrder(),
                content.getCreatedAt());
    }

    private CourseExamItemResponse toExamItem(Exam exam) {
        String examName = exam.getName() == null ? "" : exam.getName().trim();
        if (examName.isEmpty()) {
            examName = "examen";
        }
        return new CourseExamItemResponse(
                exam.getId(),
                examName,
                exam.getQuestionsCount(),
                trimOrNull(exam.getCode()),
                exam.getUser() == null ? null : exam.getUser().getId(),
                "public".equalsIgnoreCase(trimOrNull(exam.getVisibility())) ? "public" : "private",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                exam.getCreatedAt());
    }

    private CourseExamItemResponse toExamItem(ExamSummaryResponse exam) {
        return new CourseExamItemResponse(
                exam.id(),
                trimOrNull(exam.name()),
                exam.questionsCount(),
                trimOrNull(exam.code()),
                exam.ownerUserId(),
                trimOrNull(exam.visibility()),
                trimOrNull(exam.accessRole()),
                exam.canEditQuestions(),
                exam.canEditSettings(),
                exam.canShare(),
                exam.canStartGroup(),
                exam.canRenameExam(),
                exam.participantsCount(),
                exam.personalPracticeCount(),
                exam.groupPracticeCount(),
                exam.attemptsCount(),
                exam.groupPracticeSessionId(),
                trimOrNull(exam.groupPracticeStatus()),
                exam.groupPracticeCreatedByUserId(),
                exam.createdAt());
    }

    private Exam requireCourseSessionContentExam(Long courseId, Long sessionId, Long contentId, Long userId) {
        requireUser(userId);

        CourseSession session = courseSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Sesion no encontrada"));
        if (session.getDeletedAt() != null) {
            throw new NotFoundException("Sesion no encontrada");
        }
        Course course = session.getCourse();
        if (course == null || course.getDeletedAt() != null || !course.getId().equals(courseId)) {
            throw new NotFoundException("Curso no encontrado");
        }
        if (!hasCourseAccess(course, userId)) {
            throw new NotFoundException("Curso no encontrado");
        }

        CourseSessionContent content = courseSessionContentRepository
                .findByIdAndCourseSessionIdAndDeletedAtIsNull(contentId, session.getId())
                .orElseThrow(() -> new NotFoundException("Contenido de sesion no encontrado"));

        String contentType = trimOrNull(content.getType());
        if (contentType == null || !contentType.equalsIgnoreCase("exam")) {
            throw new BadRequestException("Este contenido no corresponde a un examen");
        }

        Exam sourceExam = content.getSourceExam();
        if (sourceExam == null || sourceExam.getDeletedAt() != null) {
            throw new BadRequestException("Este contenido no tiene un examen asociado");
        }

        return sourceExam;
    }

    private String normalizeVideoLink(String rawValue) {
        String value = trimOrNull(rawValue);
        if (value == null) {
            return null;
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://" + value;
        }
        return value;
    }

    private String validateAndNormalizeImageData(String imageData) {
        if (imageData == null) {
            return null;
        }
        String lower = imageData.toLowerCase();
        if (!lower.startsWith("data:image/")) {
            throw new BadRequestException("La portada debe ser una imagen");
        }
        validateDocumentSize(imageData);
        return imageData;
    }

    private String validatePdfFileData(String fileData, String fileName) {
        String lower = fileData.toLowerCase();
        String normalizedName = fileName.toLowerCase();
        boolean validMime = lower.startsWith("data:application/pdf;base64,")
                || lower.startsWith("data:application/octet-stream;base64,");
        boolean validExtension = normalizedName.endsWith(".pdf");
        if (!validMime && !validExtension) {
            throw new BadRequestException("Solo se permite PDF para el campo pdf");
        }
        validateDocumentSize(fileData);
        return fileData;
    }

    private String validateWordFileData(String fileData, String fileName) {
        String lower = fileData.toLowerCase();
        String normalizedName = fileName.toLowerCase();
        boolean validMime = lower.startsWith("data:application/msword;base64,")
                || lower.startsWith("data:application/vnd.ms-word;base64,")
                || lower.startsWith(
                        "data:application/vnd.openxmlformats-officedocument.wordprocessingml.document;base64,")
                || lower.startsWith("data:application/octet-stream;base64,");
        boolean validExtension = normalizedName.endsWith(".doc") || normalizedName.endsWith(".docx");
        if (!validMime && !validExtension) {
            throw new BadRequestException("Solo se permite Word para el campo word");
        }
        validateDocumentSize(fileData);
        return fileData;
    }

    private void validateDocumentSize(String fileData) {
        if (fileData.length() > 7_000_000) {
            throw new BadRequestException("El archivo es demasiado grande");
        }
    }

    private String normalizeContentType(String rawType) {
        String type = trimOrNull(rawType);
        if (type == null) {
            throw new BadRequestException("type es obligatorio");
        }
        String normalized = type.toLowerCase(Locale.ROOT);
        if (!normalized.equals("video")
                && !normalized.equals("pdf")
                && !normalized.equals("word")
                && !normalized.equals("cover")
                && !normalized.equals("exam")) {
            throw new BadRequestException("type debe ser video, pdf, word, cover o exam");
        }
        return normalized;
    }

    private Integer normalizeWeekOrder(Integer rawWeekOrder, Long sessionId, Long excludingWeekId) {
        if (rawWeekOrder == null) {
            int nextOrder = 1;
            for (CourseWeek week : courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(sessionId)) {
                if (week == null || week.getWeekOrder() == null) {
                    continue;
                }
                nextOrder = Math.max(nextOrder, week.getWeekOrder() + 1);
            }
            return nextOrder;
        }
        if (rawWeekOrder < 1) {
            throw new BadRequestException("weekOrder debe ser mayor o igual a 1");
        }
        List<CourseWeek> weeks = courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(sessionId);
        for (CourseWeek week : weeks) {
            if (week == null || week.getId() == null || week.getWeekOrder() == null) {
                continue;
            }
            if (excludingWeekId != null && excludingWeekId.equals(week.getId())) {
                continue;
            }
            if (rawWeekOrder.equals(week.getWeekOrder())) {
                throw new BadRequestException("weekOrder ya existe en esta sesion");
            }
        }
        return rawWeekOrder;
    }

    private Integer normalizeWeekOrderForCreate(Integer rawWeekOrder, Long sessionId) {
        if (rawWeekOrder != null && rawWeekOrder < 1) {
            throw new BadRequestException("weekOrder debe ser mayor o igual a 1");
        }

        int nextOrder = 1;
        boolean requestedExists = false;

        List<CourseWeek> activeWeeks =
                courseWeekRepository.findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(sessionId);
        for (CourseWeek week : activeWeeks) {
            if (week == null || week.getWeekOrder() == null) {
                continue;
            }
            nextOrder = Math.max(nextOrder, week.getWeekOrder() + 1);
            if (rawWeekOrder != null && rawWeekOrder.equals(week.getWeekOrder())) {
                requestedExists = true;
            }
        }

        if (rawWeekOrder == null || requestedExists) {
            return nextOrder;
        }
        return rawWeekOrder;
    }

    private void normalizeDeletedWeekOrdersForSession(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        List<CourseWeek> allWeeks = courseWeekRepository.findByCourseSessionIdOrderByWeekOrderAscCreatedAtAsc(sessionId);
        if (allWeeks.isEmpty()) {
            return;
        }

        Set<Integer> usedOrders = new HashSet<>();
        int nextNegative = -1;
        for (CourseWeek week : allWeeks) {
            if (week == null || week.getWeekOrder() == null) {
                continue;
            }
            usedOrders.add(week.getWeekOrder());
            if (week.getWeekOrder() <= nextNegative) {
                nextNegative = week.getWeekOrder() - 1;
            }
        }

        List<CourseWeek> toUpdate = new ArrayList<>();
        for (CourseWeek week : allWeeks) {
            if (week == null || week.getDeletedAt() == null) {
                continue;
            }
            Integer currentOrder = week.getWeekOrder();
            if (currentOrder != null && currentOrder < 0) {
                continue;
            }

            while (usedOrders.contains(nextNegative)) {
                nextNegative -= 1;
            }
            if (currentOrder != null) {
                usedOrders.remove(currentOrder);
            }
            week.setWeekOrder(nextNegative);
            usedOrders.add(nextNegative);
            toUpdate.add(week);
            nextNegative -= 1;
        }

        if (!toUpdate.isEmpty()) {
            courseWeekRepository.saveAll(toUpdate);
        }
    }

    private Integer resolveNextDeletedWeekOrder(Long sessionId, Long excludingWeekId) {
        List<CourseWeek> allWeeks = courseWeekRepository.findByCourseSessionIdOrderByWeekOrderAscCreatedAtAsc(sessionId);
        Set<Integer> usedOrders = new HashSet<>();
        int nextNegative = -1;

        for (CourseWeek week : allWeeks) {
            if (week == null || week.getWeekOrder() == null) {
                continue;
            }
            if (excludingWeekId != null && week.getId() != null && excludingWeekId.equals(week.getId())) {
                continue;
            }
            usedOrders.add(week.getWeekOrder());
            if (week.getWeekOrder() <= nextNegative) {
                nextNegative = week.getWeekOrder() - 1;
            }
        }

        while (usedOrders.contains(nextNegative)) {
            nextNegative -= 1;
        }
        return nextNegative;
    }

    private CourseWeek resolveWeekForContent(CourseSession session, Long weekId) {
        if (session == null || session.getId() == null) {
            throw new NotFoundException("Sesion no encontrada");
        }
        if (weekId == null) {
            throw new BadRequestException("weekId es obligatorio para registrar contenido");
        }
        return courseWeekRepository
                .findByIdAndCourseSessionIdAndDeletedAtIsNull(weekId, session.getId())
                .orElseThrow(() -> new BadRequestException("weekId no pertenece a la sesion"));
    }

    private int resolveNextWeekContentOrder(Long weekId) {
        if (weekId == null) {
            return 1;
        }
        Integer maxOrder = courseSessionContentRepository.findMaxContentOrderByCourseWeekId(weekId);
        int safeMax = maxOrder == null ? 0 : Math.max(0, maxOrder);
        return safeMax + 1;
    }

    private void normalizeWeekContentOrders(Long weekId) {
        if (weekId == null) {
            return;
        }
        List<CourseSessionContent> contents =
                courseSessionContentRepository.findByCourseWeekIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(weekId);
        if (contents.isEmpty()) {
            return;
        }
        int order = 1;
        boolean changed = false;
        for (CourseSessionContent content : contents) {
            if (content.getContentOrder() == null || content.getContentOrder() != order) {
                content.setContentOrder(order);
                changed = true;
            }
            order += 1;
        }
        if (changed) {
            courseSessionContentRepository.saveAll(contents);
        }
    }

    private void applyContentData(
            CourseSessionContent content,
            String type,
            String externalLink,
            String fileName,
            String fileData,
            Long sourceExamId,
            Long ownerUserId) {
        String normalizedExternalLink = trimOrNull(externalLink);
        String normalizedFileName = trimOrNull(fileName);
        String normalizedFileData = trimOrNull(fileData);

        if (type.equals("video")) {
            String normalizedVideoLink = normalizeVideoLink(normalizedExternalLink);
            if (normalizedVideoLink == null) {
                throw new BadRequestException("externalLink es obligatorio para contenido tipo video");
            }
            content.setExternalLink(normalizedVideoLink);
            content.setFileName(null);
            content.setFileData(null);
            content.setSourceExam(null);
            return;
        }

        if (type.equals("pdf")) {
            if (normalizedFileName == null || normalizedFileData == null) {
                throw new BadRequestException("fileName y fileData son obligatorios para contenido tipo pdf");
            }
            content.setExternalLink(null);
            content.setFileName(normalizedFileName);
            content.setFileData(validatePdfFileData(normalizedFileData, normalizedFileName));
            content.setSourceExam(null);
            return;
        }

        if (type.equals("word")) {
            if (normalizedFileName == null || normalizedFileData == null) {
                throw new BadRequestException("fileName y fileData son obligatorios para contenido tipo word");
            }
            content.setExternalLink(null);
            content.setFileName(normalizedFileName);
            content.setFileData(validateWordFileData(normalizedFileData, normalizedFileName));
            content.setSourceExam(null);
            return;
        }

        if (type.equals("exam")) {
            if (sourceExamId == null) {
                throw new BadRequestException("sourceExamId es obligatorio para contenido tipo exam");
            }
            Exam sourceExam = examRepository
                    .findByIdAndUserIdAndDeletedAtIsNull(sourceExamId, ownerUserId)
                    .orElseThrow(() -> new BadRequestException("sourceExamId no pertenece al usuario"));
            content.setExternalLink(null);
            content.setFileName(null);
            content.setFileData(null);
            content.setSourceExam(sourceExam);
            return;
        }

        if (normalizedFileData == null) {
            throw new BadRequestException("fileData es obligatorio para contenido tipo cover");
        }
        content.setExternalLink(null);
        content.setFileName(normalizedFileName);
        content.setFileData(validateAndNormalizeImageData(normalizedFileData));
        content.setSourceExam(null);
    }
}
