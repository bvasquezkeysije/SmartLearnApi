package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.ExamGroupSession;
import com.bardales.SmartLearnApi.domain.entity.ExamMembership;
import com.bardales.SmartLearnApi.domain.entity.ExamPracticePreference;
import com.bardales.SmartLearnApi.domain.entity.ExamAttempt;
import com.bardales.SmartLearnApi.domain.entity.Option;
import com.bardales.SmartLearnApi.domain.entity.Question;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.ExamMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamPracticePreferenceRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamAttemptRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.OptionRepository;
import com.bardales.SmartLearnApi.domain.repository.QuestionRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.exam.ExamParticipantPermissionUpdateRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamParticipantResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamRenameRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamIndividualPracticeSettingsRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamListVisibilityResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeStartResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeSettingsResponse;
import com.bardales.SmartLearnApi.dto.exam.ExamPracticeSettingsRequest;
import com.bardales.SmartLearnApi.dto.exam.ExamSummaryResponse;
import com.bardales.SmartLearnApi.dto.exam.ManualExamCreateRequest;
import com.bardales.SmartLearnApi.dto.exam.ManualQuestionUpsertRequest;
import com.bardales.SmartLearnApi.dto.exam.QuestionResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExamService {
    private static final int DEFAULT_REVIEW_SECONDS = 10;
    private static final Pattern LEGACY_SHARE_CLONE_SOURCE_PATTERN =
            Pattern.compile("^share-link://[^/]+/source/(\\d+)$");

    private static final List<String> REQUIRED_EXCEL_HEADERS = List.of(
            "pregunta",
            "tipo",
            "opcion_a",
            "opcion_b",
            "opcion_c",
            "opcion_d",
            "respuesta_correcta",
            "explicacion",
            "puntaje",
            "temporizador_segundos",
            "tiempo_revision_segundos",
            "cronometro_segundos",
            "temporizador");

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamRepository examRepository;
    private final ExamMembershipRepository examMembershipRepository;
    private final ExamPracticePreferenceRepository examPracticePreferenceRepository;
    private final ExamGroupSessionRepository examGroupSessionRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final UserRepository userRepository;

    public ExamService(
            ExamAttemptRepository examAttemptRepository,
            ExamRepository examRepository,
            ExamMembershipRepository examMembershipRepository,
            ExamPracticePreferenceRepository examPracticePreferenceRepository,
            ExamGroupSessionRepository examGroupSessionRepository,
            QuestionRepository questionRepository,
            OptionRepository optionRepository,
            UserRepository userRepository) {
        this.examAttemptRepository = examAttemptRepository;
        this.examRepository = examRepository;
        this.examMembershipRepository = examMembershipRepository;
        this.examPracticePreferenceRepository = examPracticePreferenceRepository;
        this.examGroupSessionRepository = examGroupSessionRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ExamSummaryResponse> listExams(Long userId) {
        User requester = requireUser(userId);
        Map<Long, Exam> examsById = new LinkedHashMap<>();

        for (Exam exam : examRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(requester.getId())) {
            if (exam.getId() != null) {
                examsById.putIfAbsent(exam.getId(), exam);
            }
        }

        for (ExamMembership membership : examMembershipRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(requester.getId())) {
            if (!Boolean.TRUE.equals(membership.getVisibleInExamList())) {
                continue;
            }
            Exam exam = membership.getExam();
            if (exam == null || exam.getDeletedAt() != null || exam.getId() == null) {
                continue;
            }
            examsById.putIfAbsent(exam.getId(), exam);
        }

        for (Exam exam : examRepository.findByVisibilityIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc("public")) {
            if (exam.getId() == null) {
                continue;
            }
            examsById.putIfAbsent(exam.getId(), exam);
        }

        return examsById.values().stream()
                .filter(exam -> !shouldHideLegacyShareClone(exam, requester.getId()))
                .sorted(Comparator
                        .comparing(
                                (Exam exam) -> exam.getCreatedAt() != null ? exam.getCreatedAt() : exam.getUpdatedAt(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .map(exam -> toExamSummary(exam, requester.getId()))
                .toList();
    }

    @Transactional
    public ExamSummaryResponse createManualExam(ManualExamCreateRequest request) {
        User user = requireUser(request.userId());

        Exam exam = new Exam();
        exam.setUser(user);
        exam.setName(request.manualExamName().trim());
        exam.setSourceFilePath("manual://" + UUID.randomUUID());
        exam.setQuestionsCount(0);
        exam.setVisibility("private");

        exam = examRepository.save(exam);
        exam = ensureExamCode(exam);
        return toExamSummary(exam, user.getId());
    }

    @Transactional
    public ExamSummaryResponse importExam(Long userId, String examName, MultipartFile examFile) {
        User user = requireUser(userId);
        String normalizedExamName = trimOrNull(examName);

        if (normalizedExamName == null) {
            throw new BadRequestException("examName es obligatorio");
        }

        if (examFile == null || examFile.isEmpty()) {
            throw new BadRequestException("examFile es obligatorio");
        }

        String originalFileName = examFile.getOriginalFilename() == null
                ? "exam.xlsx"
                : examFile.getOriginalFilename();

        String lowerFileName = originalFileName.toLowerCase(Locale.ROOT);
        if (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
            throw new BadRequestException("Solo se permite archivo Excel (.xlsx o .xls)");
        }

        Exam exam = new Exam();
        exam.setUser(user);
        exam.setName(normalizedExamName);
        exam.setSourceFilePath("upload://" + originalFileName);
        exam.setQuestionsCount(0);
        exam.setVisibility("private");
        exam = examRepository.save(exam);
        exam = ensureExamCode(exam);

        int importedCount = 0;

        try (InputStream inputStream = examFile.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("El archivo Excel no contiene hojas");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new BadRequestException("El archivo Excel no contiene encabezados");
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> headerMap = buildHeaderMap(headerRow, formatter);

            int firstDataRow = headerRow.getRowNum() + 1;
            for (int rowIndex = firstDataRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                int excelRow = rowIndex + 1;
                String questionText = readCell(row, headerMap, "pregunta", formatter);
                if (questionText.isBlank()) {
                    continue;
                }

                String questionType = mapQuestionType(readCell(row, headerMap, "tipo", formatter));
                if (questionType == null) {
                    throw new BadRequestException("Fila " + excelRow + ": el campo tipo debe ser seleccion o escrita");
                }

                String correctAnswerRaw = readCell(row, headerMap, "respuesta_correcta", formatter);
                String explanation = trimOrNull(readCell(row, headerMap, "explicacion", formatter));

                int points = parsePositiveInt(readCell(row, headerMap, "puntaje", formatter), "puntaje", excelRow);
                int timeLimit = extractTemporizadorSeconds(row, headerMap, formatter, excelRow);
                int reviewSeconds = extractReviewSeconds(row, headerMap, formatter, excelRow);
                boolean timerEnabled = extractTimerEnabled(row, headerMap, formatter, excelRow);

                Question question = new Question();
                question.setExam(exam);
                question.setQuestionText(questionText);
                question.setQuestionType(questionType);
                question.setExplanation(explanation);
                question.setPoints(points);
                question.setTimeLimit(timeLimit);
                question.setTemporizadorSegundos(timeLimit);
                question.setReviewSeconds(reviewSeconds);
                question.setTimerEnabled(timerEnabled);

                if ("multiple_choice".equals(questionType)) {
                    List<String> options = extractMultipleChoiceOptions(row, headerMap, formatter);
                    if (options.size() < 2) {
                        throw new BadRequestException(
                                "Fila " + excelRow + ": se requieren al menos 2 opciones para tipo seleccion");
                    }

                    String correctAnswer =
                            resolveCorrectAnswerForMultipleChoice(correctAnswerRaw, options, excelRow);
                    question.setCorrectAnswer(correctAnswer);
                    question = questionRepository.save(question);

                    boolean hasCorrectOption = false;
                    for (String optionText : options) {
                        boolean isCorrect = isCorrectAnswer(optionText, correctAnswer);
                        hasCorrectOption = hasCorrectOption || isCorrect;

                        Option option = new Option();
                        option.setQuestion(question);
                        option.setOptionText(optionText);
                        option.setIsCorrect(isCorrect);
                        optionRepository.save(option);
                    }

                    if (!hasCorrectOption) {
                        throw new BadRequestException(
                                "Fila " + excelRow + ": respuesta_correcta no coincide con opcion_a/b/c/d");
                    }
                } else {
                    String writtenAnswer = trimOrNull(correctAnswerRaw);
                    if (writtenAnswer == null) {
                        throw new BadRequestException("Fila " + excelRow + ": respuesta_correcta es obligatoria");
                    }

                    question.setCorrectAnswer(writtenAnswer);
                    questionRepository.save(question);
                }

                importedCount++;
            }
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo leer el archivo Excel");
        }

        if (importedCount <= 0) {
            throw new BadRequestException("No se encontraron preguntas validas para importar");
        }

        exam.setQuestionsCount(importedCount);
        exam = examRepository.save(exam);

        return toExamSummary(exam, user.getId());
    }

    @Transactional(readOnly = true)
    public byte[] downloadExamFormatTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("formato_examen");

            String[] headers = new String[] {
                "pregunta",
                "tipo",
                "opcion_a",
                "opcion_b",
                "opcion_c",
                "opcion_d",
                "respuesta_correcta",
                "explicacion",
                "puntaje",
                "temporizador_segundos",
                "tiempo_revision_segundos",
                "cronometro_segundos",
                "temporizador"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            Row sampleOne = sheet.createRow(1);
            sampleOne.createCell(0).setCellValue("Capital de Peru?");
            sampleOne.createCell(1).setCellValue("seleccion");
            sampleOne.createCell(2).setCellValue("Lima");
            sampleOne.createCell(3).setCellValue("Cusco");
            sampleOne.createCell(4).setCellValue("Piura");
            sampleOne.createCell(5).setCellValue("Arequipa");
            sampleOne.createCell(6).setCellValue("Lima");
            sampleOne.createCell(7).setCellValue("Lima es la capital del Peru");
            sampleOne.createCell(8).setCellValue(5);
            sampleOne.createCell(9).setCellValue(30);
            sampleOne.createCell(10).setCellValue(10);
            sampleOne.createCell(11).setCellValue(0);
            sampleOne.createCell(12).setCellValue("si");

            Row sampleTwo = sheet.createRow(2);
            sampleTwo.createCell(0).setCellValue("Define algoritmo");
            sampleTwo.createCell(1).setCellValue("escrita");
            sampleTwo.createCell(2).setCellValue("");
            sampleTwo.createCell(3).setCellValue("");
            sampleTwo.createCell(4).setCellValue("");
            sampleTwo.createCell(5).setCellValue("");
            sampleTwo.createCell(6).setCellValue("Conjunto de pasos para resolver un problema");
            sampleTwo.createCell(7).setCellValue("Un algoritmo es una serie de pasos ordenados");
            sampleTwo.createCell(8).setCellValue(10);
            sampleTwo.createCell(9).setCellValue(120);
            sampleTwo.createCell(10).setCellValue(15);
            sampleTwo.createCell(11).setCellValue(0);
            sampleTwo.createCell(12).setCellValue("si");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo generar el formato de examen");
        }
    }

    @Transactional
    public ExamSummaryResponse renameExam(Long examId, ExamRenameRequest request) {
        Exam exam = requireExamCanRename(examId, request.userId());
        exam.setName(request.examName().trim());
        return toExamSummary(examRepository.save(exam), request.userId());
    }

    @Transactional
    public ExamSummaryResponse updatePracticeSettings(Long examId, ExamPracticeSettingsRequest request) {
        Exam exam = requireExamCanEdit(examId, request.userId());
        PracticeSettings settings = parsePracticeSettings(
                request.practiceFeedbackMode(),
                request.practiceOrderMode(),
                request.practiceProgressMode());

        exam.setPracticeFeedbackEnabled(settings.feedbackEnabled());
        exam.setPracticeOrderMode(settings.orderMode());
        exam.setPracticeRepeatUntilCorrect(settings.repeatUntilCorrect());

        String requestedVisibility = trimOrNull(request.visibility());
        if (requestedVisibility != null) {
            Long ownerId = exam.getUser() == null ? null : exam.getUser().getId();
            if (ownerId == null || !ownerId.equals(request.userId())) {
                throw new BadRequestException("Solo el propietario puede cambiar la visibilidad.");
            }
            exam.setVisibility(normalizeExamVisibility(requestedVisibility));
        }

        return toExamSummary(examRepository.save(exam), request.userId());
    }

    @Transactional(readOnly = true)
    public ExamPracticeSettingsResponse getIndividualPracticeSettings(Long examId, Long userId) {
        Exam exam = requireExamCanPractice(examId, userId);
        PracticeSettings settings = resolvePracticeSettingsForUser(exam, userId);
        return toPracticeSettingsResponse(settings);
    }

    @Transactional
    public ExamPracticeSettingsResponse updateIndividualPracticeSettings(Long examId, ExamIndividualPracticeSettingsRequest request) {
        Exam exam = requireExamCanPractice(examId, request.userId());
        User user = requireUser(request.userId());
        PracticeSettings settings = parsePracticeSettings(
                request.practiceFeedbackMode(),
                request.practiceOrderMode(),
                request.practiceProgressMode());

        ExamPracticePreference preference = examPracticePreferenceRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(examId, user.getId())
                .orElseGet(ExamPracticePreference::new);
        preference.setExam(exam);
        preference.setUser(user);
        preference.setPracticeFeedbackEnabled(settings.feedbackEnabled());
        preference.setPracticeOrderMode(settings.orderMode());
        preference.setPracticeRepeatUntilCorrect(settings.repeatUntilCorrect());
        preference.setDeletedAt(null);
        preference = examPracticePreferenceRepository.save(preference);

        return toPracticeSettingsResponse(resolvePracticeSettingsForUser(exam, user.getId()));
    }

    @Transactional
    public ExamPracticeStartResponse startPracticeAttempt(Long examId, Long userId) {
        User user = requireUser(userId);
        Exam exam = requireExamCanPractice(examId, userId);
        PracticeSettings settings = resolvePracticeSettingsForUser(exam, userId);

        Long ownerUserId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerUserId == null || !ownerUserId.equals(userId)) {
            upsertExamMembership(exam, user, "viewer", Boolean.FALSE);
        }

        List<Question> questions = questionRepository.findByExamIdOrderByIdAsc(examId);
        if (questions.isEmpty()) {
            throw new BadRequestException("Este examen no tiene preguntas para iniciar repaso.");
        }

        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        if ("random".equalsIgnoreCase(settings.orderMode())) {
            questionIds = new ArrayList<>(questionIds);
            Collections.shuffle(questionIds);
        }

        int totalPoints = questions.stream().mapToInt(q -> q.getPoints() == null ? 0 : q.getPoints()).sum();

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExam(exam);
        attempt.setUser(user);
        attempt.setQuestionIds(questionIds.toString());
        attempt.setQuestionsOrderMode(settings.orderMode());
        attempt.setFeedbackEnabled(settings.feedbackEnabled());
        attempt.setRepeatUntilCorrect(settings.repeatUntilCorrect());
        attempt.setTotalQuestions(questionIds.size());
        attempt.setAnsweredCount(0);
        attempt.setUnansweredCount(0);
        attempt.setCorrectCount(0);
        attempt.setTotalPoints(totalPoints);
        attempt.setScoredPoints(0);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setFinishedAt(null);

        attempt = examAttemptRepository.save(attempt);
        return new ExamPracticeStartResponse(
                attempt.getId(),
                attempt.getTotalQuestions(),
                attempt.getQuestionsOrderMode(),
                attempt.getFeedbackEnabled(),
                attempt.getRepeatUntilCorrect(),
                attempt.getStartedAt());
    }

    @Transactional
    public Exam ensureExamAvailableForUser(Long sourceExamId, Long userId, String sourceFilePath) {
        User user = requireUser(userId);
        Exam sourceExam = examRepository.findById(sourceExamId)
                .orElseThrow(() -> new NotFoundException("Examen no encontrado"));

        if (sourceExam.getDeletedAt() != null) {
            throw new NotFoundException("Examen no encontrado");
        }

        Long sourceOwnerId = sourceExam.getUser() != null ? sourceExam.getUser().getId() : null;
        if (sourceOwnerId != null && sourceOwnerId.equals(userId)) {
            return sourceExam;
        }

        String normalizedSourcePath = trimOrNull(sourceFilePath);
        if (normalizedSourcePath == null) {
            normalizedSourcePath = "course-content://source/" + sourceExamId;
        }

        Exam existing = examRepository
                .findByUserIdAndSourceFilePathAndDeletedAtIsNull(userId, normalizedSourcePath)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        Exam clonedExam = new Exam();
        clonedExam.setUser(user);
        clonedExam.setName(sourceExam.getName());
        clonedExam.setSourceFilePath(normalizedSourcePath);
        clonedExam.setQuestionsCount(sourceExam.getQuestionsCount() == null ? 0 : sourceExam.getQuestionsCount());
        clonedExam.setPracticeFeedbackEnabled(
                sourceExam.getPracticeFeedbackEnabled() == null ? Boolean.TRUE : sourceExam.getPracticeFeedbackEnabled());
        clonedExam.setPracticeOrderMode(
                trimOrNull(sourceExam.getPracticeOrderMode()) == null ? "ordered" : sourceExam.getPracticeOrderMode());
        clonedExam.setPracticeRepeatUntilCorrect(
                sourceExam.getPracticeRepeatUntilCorrect() != null && sourceExam.getPracticeRepeatUntilCorrect());
        clonedExam = examRepository.save(clonedExam);
        clonedExam = ensureExamCode(clonedExam);

        List<Question> sourceQuestions = questionRepository.findByExamIdOrderByIdAsc(sourceExam.getId());
        int copiedQuestions = 0;
        for (Question sourceQuestion : sourceQuestions) {
            Question clonedQuestion = new Question();
            clonedQuestion.setExam(clonedExam);
            clonedQuestion.setQuestionText(sourceQuestion.getQuestionText());
            clonedQuestion.setQuestionType(sourceQuestion.getQuestionType());
            clonedQuestion.setCorrectAnswer(sourceQuestion.getCorrectAnswer());
            clonedQuestion.setExplanation(sourceQuestion.getExplanation());
            clonedQuestion.setPoints(sourceQuestion.getPoints() == null ? 1 : sourceQuestion.getPoints());
            Integer normalizedTimeLimit = sourceQuestion.getTemporizadorSegundos() != null
                    ? sourceQuestion.getTemporizadorSegundos()
                    : sourceQuestion.getTimeLimit();
            if (normalizedTimeLimit == null || normalizedTimeLimit <= 0) {
                normalizedTimeLimit = 30;
            }
            clonedQuestion.setTimeLimit(normalizedTimeLimit);
            clonedQuestion.setTemporizadorSegundos(normalizedTimeLimit);
            clonedQuestion.setReviewSeconds(resolveReviewSeconds(sourceQuestion.getReviewSeconds()));
            clonedQuestion.setTimerEnabled(sourceQuestion.getTimerEnabled() == null ? Boolean.TRUE : sourceQuestion.getTimerEnabled());
            clonedQuestion = questionRepository.save(clonedQuestion);
            copiedQuestions++;

            List<Option> sourceOptions = optionRepository.findByQuestionIdOrderByIdAsc(sourceQuestion.getId());
            for (Option sourceOption : sourceOptions) {
                Option clonedOption = new Option();
                clonedOption.setQuestion(clonedQuestion);
                clonedOption.setOptionText(sourceOption.getOptionText());
                clonedOption.setIsCorrect(Boolean.TRUE.equals(sourceOption.getIsCorrect()));
                optionRepository.save(clonedOption);
            }
        }

        clonedExam.setQuestionsCount(copiedQuestions);
        clonedExam = examRepository.save(clonedExam);
        return clonedExam;
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getManualExam(Long examId, Long userId) {
        requireExamCanPractice(examId, userId);
        return questionRepository.findByExamIdOrderByIdAsc(examId).stream()
                .map(this::toQuestionResponse)
                .toList();
    }

    @Transactional
    public QuestionResponse addManualQuestion(Long examId, ManualQuestionUpsertRequest request) {
        Exam exam = requireExamCanEdit(examId, request.userId());
        ValidatedQuestion validated = validateQuestionPayload(request);

        Question question = new Question();
        question.setExam(exam);
        question.setQuestionText(request.questionText().trim());
        question.setQuestionType(validated.questionType());
        question.setCorrectAnswer(validated.correctAnswer());
        question.setExplanation(trimOrNull(request.explanation()));
        question.setPoints(request.points());
        question.setTimeLimit(request.temporizadorSegundos());
        question.setTemporizadorSegundos(request.temporizadorSegundos());
        question.setReviewSeconds(resolveReviewSeconds(request.reviewSeconds()));
        question.setTimerEnabled(Boolean.TRUE.equals(request.timerEnabled()));
        question = questionRepository.save(question);

        persistOptions(question, validated.optionsByLetter(), validated.correctOption());
        refreshQuestionsCount(exam);

        return toQuestionResponse(question);
    }

    @Transactional
    public QuestionResponse updateManualQuestion(Long examId, Long questionId, ManualQuestionUpsertRequest request) {
        Exam exam = requireExamCanEdit(examId, request.userId());
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Pregunta no encontrada"));

        if (question.getExam() == null || !question.getExam().getId().equals(exam.getId())) {
            throw new NotFoundException("Pregunta no pertenece al examen");
        }

        ValidatedQuestion validated = validateQuestionPayload(request);

        question.setQuestionText(request.questionText().trim());
        question.setQuestionType(validated.questionType());
        question.setCorrectAnswer(validated.correctAnswer());
        question.setExplanation(trimOrNull(request.explanation()));
        question.setPoints(request.points());
        question.setTimeLimit(request.temporizadorSegundos());
        question.setTemporizadorSegundos(request.temporizadorSegundos());
        question.setReviewSeconds(resolveReviewSeconds(request.reviewSeconds()));
        question.setTimerEnabled(Boolean.TRUE.equals(request.timerEnabled()));
        question = questionRepository.save(question);

        optionRepository.deleteByQuestionId(question.getId());
        persistOptions(question, validated.optionsByLetter(), validated.correctOption());
        refreshQuestionsCount(exam);

        return toQuestionResponse(question);
    }

    @Transactional
    public void deleteExam(Long examId, Long userId) {
        Exam exam = requireExamOwner(examId, userId);
        exam.setDeletedAt(LocalDateTime.now());
        examRepository.save(exam);
    }

    @Transactional(readOnly = true)
    public List<ExamParticipantResponse> listParticipants(Long examId, Long requesterUserId) {
        Exam exam = requireExamCanPractice(examId, requesterUserId);
        List<ExamParticipantResponse> participants = new ArrayList<>();

        User owner = exam.getUser();
        Long ownerId = owner == null ? null : owner.getId();
        if (owner != null && ownerId != null) {
            participants.add(new ExamParticipantResponse(
                    ownerId,
                    trimOrNull(owner.getName()) == null ? "Usuario" : trimOrNull(owner.getName()),
                    trimOrNull(owner.getUsername()) == null ? "" : trimOrNull(owner.getUsername()),
                    trimOrNull(owner.getEmail()) == null ? "" : trimOrNull(owner.getEmail()),
                    resolveUserProfileImage(owner),
                    "owner",
                    Boolean.TRUE,
                    Boolean.TRUE,
                    Boolean.TRUE,
                    Boolean.TRUE,
                    exam.getCreatedAt()));
        }

        for (ExamMembership membership : examMembershipRepository.findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(examId)) {
            User participant = membership.getUser();
            if (participant == null || participant.getId() == null) {
                continue;
            }
            if (ownerId != null && ownerId.equals(participant.getId())) {
                continue;
            }
            participants.add(new ExamParticipantResponse(
                    participant.getId(),
                    trimOrNull(participant.getName()) == null ? "Usuario" : trimOrNull(participant.getName()),
                    trimOrNull(participant.getUsername()) == null ? "" : trimOrNull(participant.getUsername()),
                    trimOrNull(participant.getEmail()) == null ? "" : trimOrNull(participant.getEmail()),
                    resolveUserProfileImage(participant),
                    normalizeExamRole(membership.getRole()),
                    Boolean.TRUE.equals(membership.getCanShare()),
                    Boolean.TRUE.equals(membership.getCanStartGroup()),
                    Boolean.TRUE.equals(membership.getCanRenameExam()),
                    Boolean.FALSE,
                    membership.getCreatedAt()));
        }

        return participants;
    }

    @Transactional
    public void updateExamParticipantPermissions(
            Long examId, Long participantUserId, ExamParticipantPermissionUpdateRequest request) {
        Exam exam = requireExamOwner(examId, request.requesterUserId());
        Long ownerId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerId != null && ownerId.equals(participantUserId)) {
            throw new BadRequestException("No puedes cambiar los permisos del propietario.");
        }

        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(examId, participantUserId)
                .orElseThrow(() -> new NotFoundException("Participante no encontrado en este examen."));
        membership.setRole(normalizeExamRole(request.role()));
        membership.setCanShare(Boolean.TRUE.equals(request.canShare()));
        membership.setCanStartGroup(Boolean.TRUE.equals(request.canStartGroup()));
        membership.setCanRenameExam(Boolean.TRUE.equals(request.canRenameExam()));
        examMembershipRepository.save(membership);
    }

    @Transactional
    public void removeExamParticipant(Long examId, Long participantUserId, Long requesterUserId) {
        Exam exam = requireExamOwner(examId, requesterUserId);
        Long ownerId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerId != null && ownerId.equals(participantUserId)) {
            throw new BadRequestException("No puedes eliminar al propietario del examen.");
        }

        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(examId, participantUserId)
                .orElseThrow(() -> new NotFoundException("Participante no encontrado en este examen."));

        LocalDateTime now = LocalDateTime.now();
        membership.setRole("viewer");
        membership.setCanShare(Boolean.FALSE);
        membership.setCanStartGroup(Boolean.FALSE);
        membership.setCanRenameExam(Boolean.FALSE);
        membership.setDeletedAt(now);
        examMembershipRepository.save(membership);

        ExamPracticePreference preference = examPracticePreferenceRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(examId, participantUserId)
                .orElse(null);
        if (preference != null) {
            preference.setDeletedAt(now);
            examPracticePreferenceRepository.save(preference);
        }
    }

    @Transactional(readOnly = true)
    public Exam requireExamCanShare(Long examId, Long userId) {
        ExamAccess access = resolveExamAccess(examId, userId);
        if (!access.canShare()) {
            throw new BadRequestException("No tienes permisos para compartir este examen.");
        }
        return access.exam();
    }

    @Transactional
    public void upsertExamMembership(Exam exam, User participant, String role, Boolean canShare) {
        upsertExamMembership(exam, participant, role, canShare, Boolean.FALSE, Boolean.TRUE);
    }

    @Transactional
    public void upsertExamMembership(Exam exam, User participant, String role, Boolean canShare, Boolean canStartGroup) {
        upsertExamMembership(exam, participant, role, canShare, canStartGroup, Boolean.TRUE);
    }

    @Transactional
    public void upsertExamMembership(
            Exam exam,
            User participant,
            String role,
            Boolean canShare,
            Boolean canStartGroup,
            Boolean visibleInExamList) {
        if (exam == null || exam.getId() == null || participant == null || participant.getId() == null) {
            throw new BadRequestException("No se pudo registrar el participante del examen.");
        }

        Long ownerId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerId != null && ownerId.equals(participant.getId())) {
            return;
        }

        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(exam.getId(), participant.getId())
                .orElse(null);
        if (membership == null) {
            membership = new ExamMembership();
            membership.setExam(exam);
            membership.setUser(participant);
            membership.setCanRenameExam(Boolean.FALSE);
        }
        membership.setRole(normalizeExamRole(role));
        membership.setCanShare(Boolean.TRUE.equals(canShare));
        membership.setCanStartGroup(Boolean.TRUE.equals(canStartGroup));
        membership.setVisibleInExamList(Boolean.TRUE.equals(visibleInExamList));
        membership.setDeletedAt(null);
        examMembershipRepository.save(membership);
    }

    @Transactional
    public void setExamListVisibility(Long examId, Long userId, Boolean visible) {
        Exam exam = requireExamCanPractice(examId, userId);
        Long ownerId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerId != null && ownerId.equals(userId)) {
            return;
        }

        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(examId, userId)
                .orElseThrow(() -> new NotFoundException("No perteneces a este examen."));
        membership.setVisibleInExamList(Boolean.TRUE.equals(visible));
        examMembershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public ExamListVisibilityResponse getExamListVisibility(Long examId, Long userId) {
        Exam exam = requireExamCanPractice(examId, userId);
        Long ownerId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerId != null && ownerId.equals(userId)) {
            return new ExamListVisibilityResponse(examId, userId, true);
        }
        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(examId, userId)
                .orElse(null);
        boolean visible = membership != null && Boolean.TRUE.equals(membership.getVisibleInExamList());
        return new ExamListVisibilityResponse(examId, userId, visible);
    }

    private void refreshQuestionsCount(Exam exam) {
        int count = questionRepository.findByExamIdOrderByIdAsc(exam.getId()).size();
        exam.setQuestionsCount(count);
        examRepository.save(exam);
    }

    private ValidatedQuestion validateQuestionPayload(ManualQuestionUpsertRequest request) {
        String type = request.questionType() == null ? "" : request.questionType().trim().toLowerCase();
        if (!type.equals("multiple_choice") && !type.equals("written")) {
            throw new BadRequestException("questionType debe ser multiple_choice o written");
        }

        Map<String, String> options = new LinkedHashMap<>();
        putOption(options, "a", request.optionA());
        putOption(options, "b", request.optionB());
        putOption(options, "c", request.optionC());
        putOption(options, "d", request.optionD());

        String correctOption = request.correctOption() == null ? null : request.correctOption().trim().toLowerCase();
        String correctAnswer = trimOrNull(request.correctAnswer());

        if (type.equals("multiple_choice")) {
            if (options.size() < 2) {
                throw new BadRequestException("Debes ingresar al menos 2 opciones para seleccion multiple");
            }
            if (correctOption == null || !options.containsKey(correctOption)) {
                throw new BadRequestException("correctOption invalida para las opciones registradas");
            }
            correctAnswer = options.get(correctOption);
        } else {
            if (correctAnswer == null) {
                throw new BadRequestException("correctAnswer es obligatorio para preguntas escritas");
            }
            correctOption = null;
        }

        return new ValidatedQuestion(type, correctAnswer, options, correctOption);
    }

    private void persistOptions(Question question, Map<String, String> optionsByLetter, String correctOption) {
        if (!"multiple_choice".equals(question.getQuestionType())) {
            return;
        }

        for (Map.Entry<String, String> entry : optionsByLetter.entrySet()) {
            Option option = new Option();
            option.setQuestion(question);
            option.setOptionText(entry.getValue());
            option.setIsCorrect(entry.getKey().equals(correctOption));
            optionRepository.save(option);
        }
    }

    private void putOption(Map<String, String> options, String key, String value) {
        String normalized = trimOrNull(value);
        if (normalized != null) {
            options.put(key, normalized);
        }
    }

    private Map<String, Integer> buildHeaderMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headerMap = new HashMap<>();

        short lastCell = headerRow.getLastCellNum();
        for (int i = 0; i < lastCell; i++) {
            String normalized = normalizeHeader(formatter.formatCellValue(headerRow.getCell(i)));
            if (!normalized.isBlank() && !headerMap.containsKey(normalized)) {
                headerMap.put(normalized, i);
            }
        }

        Set<String> missing = new HashSet<>(REQUIRED_EXCEL_HEADERS);
        missing.removeAll(headerMap.keySet());
        if (!missing.isEmpty()) {
            throw new BadRequestException("Faltan columnas en el Excel: " + String.join(", ", missing));
        }

        return headerMap;
    }

    private String readCell(Row row, Map<String, Integer> headerMap, String key, DataFormatter formatter) {
        Integer index = headerMap.get(key);
        if (index == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private int parsePositiveInt(String rawValue, String fieldName, int excelRow) {
        String normalized = rawValue == null ? "" : rawValue.trim();
        if (normalized.isBlank()) {
            throw new BadRequestException("Fila " + excelRow + ": " + fieldName + " es obligatorio");
        }

        try {
            int parsed = Integer.parseInt(normalized);
            if (parsed <= 0) {
                throw new NumberFormatException("must be > 0");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Fila " + excelRow + ": " + fieldName + " debe ser mayor a 0");
        }
    }

    private int extractTemporizadorSeconds(Row row, Map<String, Integer> headerMap, DataFormatter formatter, int excelRow) {
        String key = headerMap.containsKey("temporizador_segundos") ? "temporizador_segundos" : "tiempo_segundos";
        return parsePositiveInt(readCell(row, headerMap, key, formatter), key, excelRow);
    }

    private int extractReviewSeconds(Row row, Map<String, Integer> headerMap, DataFormatter formatter, int excelRow) {
        String key = null;
        if (headerMap.containsKey("tiempo_revision_segundos")) {
            key = "tiempo_revision_segundos";
        } else if (headerMap.containsKey("revision_segundos")) {
            key = "revision_segundos";
        } else if (headerMap.containsKey("tiempo_revision")) {
            key = "tiempo_revision";
        }

        if (key == null) {
            return DEFAULT_REVIEW_SECONDS;
        }

        String rawValue = readCell(row, headerMap, key, formatter);
        if (rawValue.isBlank()) {
            return DEFAULT_REVIEW_SECONDS;
        }

        return parsePositiveInt(rawValue, key, excelRow);
    }

    private int resolveReviewSeconds(Integer reviewSeconds) {
        if (reviewSeconds == null || reviewSeconds <= 0) {
            return DEFAULT_REVIEW_SECONDS;
        }
        return reviewSeconds;
    }

    private boolean extractTimerEnabled(Row row, Map<String, Integer> headerMap, DataFormatter formatter, int excelRow) {
        if (!headerMap.containsKey("temporizador")) {
            return true;
        }

        String raw = readCell(row, headerMap, "temporizador", formatter);
        if (raw.isBlank()) {
            return true;
        }

        String normalized = normalizeHeader(raw);
        if (List.of("1", "si", "s", "yes", "true", "on", "activo", "activado").contains(normalized)) {
            return true;
        }

        if (List.of("0", "no", "n", "false", "off", "inactivo", "desactivado").contains(normalized)) {
            return false;
        }

        throw new BadRequestException("Fila " + excelRow + ": temporizador debe ser si/no, true/false o 1/0");
    }

    private String mapQuestionType(String rawValue) {
        String normalized = normalizeHeader(rawValue);

        if (List.of("seleccion", "multiple", "multiple_choice", "multiplechoice", "opcion_multiple", "opcionmultiple")
                .contains(normalized)) {
            return "multiple_choice";
        }

        if (List.of("escrita", "written", "abierta", "texto").contains(normalized)) {
            return "written";
        }

        return null;
    }

    private List<String> extractMultipleChoiceOptions(Row row, Map<String, Integer> headerMap, DataFormatter formatter) {
        List<String> options = new ArrayList<>();
        for (String key : List.of("opcion_a", "opcion_b", "opcion_c", "opcion_d")) {
            String value = readCell(row, headerMap, key, formatter);
            if (!value.isBlank()) {
                options.add(value);
            }
        }
        return options;
    }

    private String resolveCorrectAnswerForMultipleChoice(String rawAnswer, List<String> options, int excelRow) {
        String answer = rawAnswer == null ? "" : rawAnswer.trim();
        if (answer.isBlank()) {
            throw new BadRequestException("Fila " + excelRow + ": respuesta_correcta es obligatoria");
        }

        String normalized = normalizeHeader(answer);
        Map<String, Integer> letterMap = Map.of(
                "a", 0,
                "b", 1,
                "c", 2,
                "d", 3,
                "1", 0,
                "2", 1,
                "3", 2,
                "4", 3);

        if (letterMap.containsKey(normalized)) {
            int index = letterMap.get(normalized);
            if (index >= 0 && index < options.size()) {
                return options.get(index);
            }
            throw new BadRequestException("Fila " + excelRow + ": respuesta_correcta no tiene una opcion valida asociada");
        }

        for (String option : options) {
            if (isCorrectAnswer(answer, option)) {
                return option;
            }
        }

        throw new BadRequestException("Fila " + excelRow + ": respuesta_correcta no coincide con opcion_a/b/c/d");
    }

    private boolean isCorrectAnswer(String givenAnswer, String correctAnswer) {
        return normalizeAnswerForComparison(givenAnswer).equals(normalizeAnswerForComparison(correctAnswer));
    }

    private String normalizeHeader(String value) {
        String safe = value == null ? "" : value;
        String noAccents = Normalizer.normalize(safe, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }

    private String normalizeAnswerForComparison(String value) {
        String safe = value == null ? "" : value;
        String noAccents = Normalizer.normalize(safe, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "")
                .trim();
    }

    private Exam requireExamCanPractice(Long examId, Long userId) {
        return resolveExamAccess(examId, userId).exam();
    }

    private PracticeSettings parsePracticeSettings(String feedbackModeRaw, String orderModeRaw, String progressModeRaw) {
        String feedbackMode = trimOrNull(feedbackModeRaw);
        feedbackMode = feedbackMode == null ? "with_feedback" : feedbackMode.toLowerCase(Locale.ROOT);
        if (!feedbackMode.equals("with_feedback") && !feedbackMode.equals("without_feedback")) {
            throw new BadRequestException("practiceFeedbackMode invalido");
        }

        String orderMode = trimOrNull(orderModeRaw);
        orderMode = orderMode == null ? "ordered" : orderMode.toLowerCase(Locale.ROOT);
        if (!orderMode.equals("ordered") && !orderMode.equals("random")) {
            throw new BadRequestException("practiceOrderMode invalido");
        }

        String progressMode = trimOrNull(progressModeRaw);
        progressMode = progressMode == null ? "repeat_until_correct" : progressMode.toLowerCase(Locale.ROOT);
        if (!progressMode.equals("repeat_until_correct") && !progressMode.equals("allow_incorrect_pass")) {
            throw new BadRequestException("practiceProgressMode invalido");
        }

        return new PracticeSettings(
                "with_feedback".equals(feedbackMode),
                orderMode,
                "repeat_until_correct".equals(progressMode));
    }

    private PracticeSettings resolvePracticeSettingsForUser(Exam exam, Long userId) {
        ExamPracticePreference preference = examPracticePreferenceRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(exam.getId(), userId)
                .orElse(null);
        if (preference != null) {
            return new PracticeSettings(
                    Boolean.TRUE.equals(preference.getPracticeFeedbackEnabled()),
                    "random".equalsIgnoreCase(preference.getPracticeOrderMode()) ? "random" : "ordered",
                    Boolean.TRUE.equals(preference.getPracticeRepeatUntilCorrect()));
        }
        return new PracticeSettings(
                exam.getPracticeFeedbackEnabled() == null || exam.getPracticeFeedbackEnabled(),
                "random".equalsIgnoreCase(exam.getPracticeOrderMode()) ? "random" : "ordered",
                exam.getPracticeRepeatUntilCorrect() == null || exam.getPracticeRepeatUntilCorrect());
    }

    private ExamPracticeSettingsResponse toPracticeSettingsResponse(PracticeSettings settings) {
        return new ExamPracticeSettingsResponse(
                settings.feedbackEnabled() ? "with_feedback" : "without_feedback",
                settings.orderMode(),
                settings.repeatUntilCorrect() ? "repeat_until_correct" : "allow_incorrect_pass");
    }

    private Exam requireExamCanEdit(Long examId, Long userId) {
        ExamAccess access = resolveExamAccess(examId, userId);
        if (!access.canEdit()) {
            throw new BadRequestException("No tienes permisos para editar este examen.");
        }
        return access.exam();
    }

    private Exam requireExamCanRename(Long examId, Long userId) {
        ExamAccess access = resolveExamAccess(examId, userId);
        if (!access.canRenameExam()) {
            throw new BadRequestException("No tienes permisos para renombrar este examen.");
        }
        return access.exam();
    }

    private Exam requireExamOwner(Long examId, Long userId) {
        ExamAccess access = resolveExamAccess(examId, userId);
        if (!access.owner()) {
            throw new BadRequestException("Solo el propietario puede realizar esta accion.");
        }
        return access.exam();
    }

    private ExamAccess resolveExamAccess(Long examId, Long userId) {
        requireUser(userId);
        Exam exam = examRepository.findByIdAndDeletedAtIsNull(examId)
                .orElseThrow(() -> new NotFoundException("Examen no encontrado"));
        Long ownerUserId = exam.getUser() == null ? null : exam.getUser().getId();
        if (ownerUserId != null && ownerUserId.equals(userId)) {
            return new ExamAccess(exam, true, "owner", true, true, true, true);
        }

        ExamMembership membership = examMembershipRepository
                .findByExamIdAndUserIdAndDeletedAtIsNull(examId, userId)
                .orElse(null);
        if (membership == null) {
            if ("public".equals(normalizeExamVisibility(exam.getVisibility()))) {
                return new ExamAccess(exam, false, "viewer", false, false, false, false);
            }
            throw new NotFoundException("Examen no encontrado");
        }
        String role = normalizeExamRole(membership.getRole());
        boolean canEdit = "editor".equals(role);
        boolean canShare = Boolean.TRUE.equals(membership.getCanShare());
        boolean canStartGroup = Boolean.TRUE.equals(membership.getCanStartGroup());
        boolean canRenameExam = Boolean.TRUE.equals(membership.getCanRenameExam());
        return new ExamAccess(exam, false, role, canEdit, canShare, canStartGroup, canRenameExam);
    }

    private String normalizeExamRole(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "viewer";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.equals("editor")) {
            return "editor";
        }
        return "viewer";
    }

    private String normalizeExamVisibility(String value) {
        String normalized = trimOrNull(value);
        if (normalized == null) {
            return "private";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (normalized.equals("public")) {
            return "public";
        }
        return "private";
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveUserProfileImage(User user) {
        if (user == null) {
            return "";
        }
        String custom = trimOrNull(user.getProfileImageData());
        if (custom != null) {
            return custom;
        }
        String google = trimOrNull(user.getGooglePictureUrl());
        return google == null ? "" : google;
    }

    private boolean shouldHideLegacyShareClone(Exam exam, Long requesterUserId) {
        if (exam == null || exam.getId() == null) {
            return false;
        }

        Long sourceExamId = parseLegacyShareSourceExamId(exam.getSourceFilePath());
        if (sourceExamId == null || sourceExamId.equals(exam.getId())) {
            return false;
        }

        Exam sourceExam = examRepository.findByIdAndDeletedAtIsNull(sourceExamId).orElse(null);
        if (sourceExam == null) {
            return false;
        }

        try {
            resolveExamAccess(sourceExamId, requesterUserId);
            return true;
        } catch (NotFoundException exception) {
            return false;
        }
    }

    private Long parseLegacyShareSourceExamId(String sourceFilePath) {
        String normalizedPath = trimOrNull(sourceFilePath);
        if (normalizedPath == null) {
            return null;
        }
        Matcher matcher = LEGACY_SHARE_CLONE_SOURCE_PATTERN.matcher(normalizedPath);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Exam ensureExamCode(Exam exam) {
        if (exam == null || exam.getId() == null) {
            return exam;
        }
        if (trimOrNull(exam.getCode()) != null) {
            return exam;
        }
        exam.setCode(buildDefaultExamCode(exam.getId()));
        return examRepository.save(exam);
    }

    private String resolveExamCode(Exam exam) {
        String storedCode = trimOrNull(exam.getCode());
        if (storedCode != null) {
            return storedCode.toUpperCase(Locale.ROOT);
        }
        return buildDefaultExamCode(exam.getId());
    }

    private String buildDefaultExamCode(Long examId) {
        long safeId = examId == null ? 0L : Math.max(examId, 0L);
        return String.format(Locale.ROOT, "EXM-%06d", safeId);
    }

    private ExamSummaryResponse toExamSummary(Exam exam, Long userId) {
        var createdAt = exam.getCreatedAt() != null ? exam.getCreatedAt() : exam.getUpdatedAt();
        long personalPracticeCount = examAttemptRepository.countByExamIdAndUserId(exam.getId(), userId);
        long groupPracticeCount = examGroupSessionRepository.countPracticedByExamId(exam.getId());
        ExamAccess access = resolveExamAccess(exam.getId(), userId);
        Long ownerUserId = exam.getUser() == null ? null : exam.getUser().getId();
        long participantsCount = 1 + examMembershipRepository.countByExamIdAndDeletedAtIsNull(exam.getId());
        ExamGroupSession groupSession = examGroupSessionRepository
                .findTopByExamIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(exam.getId(), List.of("waiting", "active"))
                .orElse(null);
        Long groupSessionId = groupSession == null ? null : groupSession.getId();
        String groupSessionStatus = groupSession == null ? null : groupSession.getStatus();
        Long groupSessionCreatedByUserId = groupSession == null || groupSession.getCreatedByUser() == null
                ? null
                : groupSession.getCreatedByUser().getId();
        return new ExamSummaryResponse(
                exam.getId(),
                exam.getName(),
                resolveExamCode(exam),
                exam.getSourceFilePath(),
                exam.getQuestionsCount(),
                personalPracticeCount,
                groupPracticeCount,
                personalPracticeCount,
                exam.getPracticeFeedbackEnabled(),
                exam.getPracticeOrderMode(),
                exam.getPracticeRepeatUntilCorrect(),
                ownerUserId,
                normalizeExamVisibility(exam.getVisibility()),
                access.role(),
                access.canEdit(),
                access.canEdit(),
                access.canShare(),
                access.canStartGroup(),
                access.canRenameExam(),
                participantsCount,
                groupSessionId,
                groupSessionStatus,
                groupSessionCreatedByUserId,
                createdAt);
    }

    private QuestionResponse toQuestionResponse(Question question) {
        List<Option> options = optionRepository.findByQuestionIdOrderByIdAsc(question.getId());

        String optionA = options.size() > 0 ? options.get(0).getOptionText() : null;
        String optionB = options.size() > 1 ? options.get(1).getOptionText() : null;
        String optionC = options.size() > 2 ? options.get(2).getOptionText() : null;
        String optionD = options.size() > 3 ? options.get(3).getOptionText() : null;

        String correctOption = null;
        for (int i = 0; i < options.size(); i++) {
            if (Boolean.TRUE.equals(options.get(i).getIsCorrect())) {
                correctOption = switch (i) {
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
                question.getExam() != null ? question.getExam().getId() : null,
                question.getQuestionText(),
                question.getQuestionType(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getPoints(),
                question.getTemporizadorSegundos() != null ? question.getTemporizadorSegundos() : question.getTimeLimit(),
                resolveReviewSeconds(question.getReviewSeconds()),
                question.getTimerEnabled(),
                optionA,
                optionB,
                optionC,
                optionD,
                correctOption);
    }

    private record ValidatedQuestion(
            String questionType,
            String correctAnswer,
            Map<String, String> optionsByLetter,
            String correctOption) {
    }

    private record ExamAccess(
            Exam exam,
            boolean owner,
            String role,
            boolean canEdit,
            boolean canShare,
            boolean canStartGroup,
            boolean canRenameExam) {
    }

    private record PracticeSettings(boolean feedbackEnabled, String orderMode, boolean repeatUntilCorrect) {
    }
}
