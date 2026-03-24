package com.bardales.SmartLearnApi.service;

import com.bardales.SmartLearnApi.domain.entity.AiChat;
import com.bardales.SmartLearnApi.domain.entity.AiChatMessage;
import com.bardales.SmartLearnApi.domain.entity.Exam;
import com.bardales.SmartLearnApi.domain.entity.Option;
import com.bardales.SmartLearnApi.domain.entity.Question;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.AiChatMessageRepository;
import com.bardales.SmartLearnApi.domain.repository.AiChatRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.OptionRepository;
import com.bardales.SmartLearnApi.domain.repository.QuestionRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.chat.AiModelsResponse;
import com.bardales.SmartLearnApi.dto.chat.ChatDetailResponse;
import com.bardales.SmartLearnApi.dto.chat.ChatGenerateExamResponse;
import com.bardales.SmartLearnApi.dto.chat.ChatMessageResponse;
import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiExamGenerationService {

    private static final DateTimeFormatter EXAM_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int MAX_CHAT_MESSAGE_LENGTH = 250;
    private static final int MAX_FILES = 3;
    private static final int MIN_QUESTIONS = 10;
    private static final int MAX_QUESTIONS = 100;

    private final AiChatRepository chatRepository;
    private final AiChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String ollamaBaseUrl;
    private final String ollamaModel;
    private final int maxSourceChars;

    public AiExamGenerationService(
            AiChatRepository chatRepository,
            AiChatMessageRepository messageRepository,
            UserRepository userRepository,
            ExamRepository examRepository,
            QuestionRepository questionRepository,
            OptionRepository optionRepository,
            ObjectMapper objectMapper,
            @Value("${app.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${app.ai.ollama.model:gemma3:12b}") String ollamaModel,
            @Value("${app.ai.exam.max-source-chars:45000}") int maxSourceChars) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
        this.maxSourceChars = Math.max(5000, maxSourceChars);
    }

    @Transactional
    public ChatGenerateExamResponse generateExamFromPdf(
            Long chatId,
            Long userId,
            String examName,
            String instructions,
            Integer questionsCount,
            String model,
            MultipartFile[] files) {
        User user = requireUser(userId);
        AiChat chat = chatRepository.findByIdAndUserId(chatId, userId)
                .orElseThrow(() -> new NotFoundException("Chat no encontrado"));

        List<MultipartFile> validFiles = normalizeFiles(files);
        int targetQuestions = normalizeQuestionsCount(questionsCount);
        String safeInstructions = normalizeInstructions(instructions);
        String safeExamName = normalizeExamName(examName);
        String selectedModel = normalizeModel(model);

        String sourceText = extractAndJoinDocumentText(validFiles);
        if (sourceText.isBlank()) {
            throw new BadRequestException("No se pudo extraer contenido util de los archivos adjuntos.");
        }
        if (sourceText.length() > maxSourceChars) {
            sourceText = sourceText.substring(0, maxSourceChars);
        }

        String userSummaryMessage = buildGenerationSummaryMessage(validFiles, safeInstructions, targetQuestions, safeExamName, selectedModel);
        createMessage(chat, "user", userSummaryMessage);

        List<GeneratedQuestion> generatedQuestions = requestQuestionsFromOllama(sourceText, safeInstructions, targetQuestions, selectedModel);
        Exam exam = createExamFromGeneratedQuestions(user, chat, safeExamName, generatedQuestions);

        String assistantMessage = """
                Listo. Genere tu examen "%s" con %d preguntas.

                EXAM_ID: %d
                ABRIR_WEB: /dashboard/examenes/repaso/%d
                ABRIR_APP: smartlearn://examen/%d
                """.formatted(
                exam.getName(),
                exam.getQuestionsCount(),
                exam.getId(),
                exam.getId(),
                exam.getId());
        createMessage(chat, "assistant", assistantMessage.trim());

        return new ChatGenerateExamResponse(
                exam.getId(),
                exam.getName(),
                exam.getQuestionsCount(),
                toChatDetail(chat));
    }

    @Transactional(readOnly = true)
    public AiModelsResponse listModels(Long userId) {
        requireUser(userId);

        List<String> models = fetchAvailableModelsFromOllama();
        if (models.isEmpty()) {
            models = new ArrayList<>();
            models.add(ollamaModel);
        }
        if (!models.contains(ollamaModel)) {
            List<String> merged = new ArrayList<>();
            merged.add(ollamaModel);
            merged.addAll(models);
            models = merged;
        }

        return new AiModelsResponse(ollamaModel, models);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private List<MultipartFile> normalizeFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BadRequestException("Debes adjuntar al menos 1 archivo (PDF o Word).");
        }

        List<MultipartFile> validFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            validFiles.add(file);
        }

        if (validFiles.isEmpty()) {
            throw new BadRequestException("No se recibieron archivos validos.");
        }

        if (validFiles.size() > MAX_FILES) {
            throw new BadRequestException("Solo se permiten hasta " + MAX_FILES + " archivos adjuntos.");
        }

        for (MultipartFile file : validFiles) {
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
            if (!isSupportedSourceFile(name)) {
                throw new BadRequestException("Solo se permiten archivos PDF o Word (.doc, .docx).");
            }
        }

        return validFiles;
    }

    private int normalizeQuestionsCount(Integer questionsCount) {
        int value = questionsCount == null ? MIN_QUESTIONS : questionsCount;
        if (value < MIN_QUESTIONS || value > MAX_QUESTIONS) {
            throw new BadRequestException("questionsCount debe estar entre " + MIN_QUESTIONS + " y " + MAX_QUESTIONS + ".");
        }
        return value;
    }

    private String normalizeInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            return "Genera un examen equilibrado basado solo en el contenido de los documentos adjuntos.";
        }
        return instructions.trim();
    }

    private String normalizeExamName(String examName) {
        if (examName != null && !examName.isBlank()) {
            return examName.trim();
        }
        return "examen_ia_" + LocalDateTime.now().format(EXAM_NAME_FORMAT);
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return ollamaModel;
        }
        return model.trim();
    }

    private String extractAndJoinDocumentText(List<MultipartFile> files) {
        List<String> chunks = new ArrayList<>();
        for (MultipartFile file : files) {
            String raw = extractDocumentText(file);
            if (!raw.isBlank()) {
                chunks.add(raw);
            }
        }
        return String.join("\n\n---\n\n", chunks).trim();
    }

    private String extractDocumentText(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return extractPdfText(file);
        }
        if (name.endsWith(".docx")) {
            return extractDocxText(file);
        }
        if (name.endsWith(".doc")) {
            return extractDocText(file);
        }
        throw new BadRequestException("Formato no soportado: " + safeName(file.getOriginalFilename()));
    }

    private String extractPdfText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String raw = stripper.getText(document);
            return raw == null ? "" : raw.replace("\u0000", "").trim();
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo leer el PDF: " + safeName(file.getOriginalFilename()));
        }
    }

    private String extractDocxText(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getBytes()));
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String raw = extractor.getText();
            return raw == null ? "" : raw.replace("\u0000", "").trim();
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo leer el archivo Word (.docx): " + safeName(file.getOriginalFilename()));
        }
    }

    private String extractDocText(MultipartFile file) {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(file.getBytes()));
                WordExtractor extractor = new WordExtractor(document)) {
            String raw = extractor.getText();
            return raw == null ? "" : raw.replace("\u0000", "").trim();
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo leer el archivo Word (.doc): " + safeName(file.getOriginalFilename()));
        }
    }

    private List<GeneratedQuestion> requestQuestionsFromOllama(String sourceText, String instructions, int questionsCount, String modelName) {
        String prompt = buildPrompt(sourceText, instructions, questionsCount);
        String payload = buildOllamaPayload(prompt, modelName);
        String modelResponse = callOllama(payload);
        return parseGeneratedQuestions(modelResponse, questionsCount);
    }

    private String buildPrompt(String sourceText, String instructions, int questionsCount) {
        return """
                Eres un generador de examenes academicos.
                Usa EXCLUSIVAMENTE el contenido adjunto (PDF/Word) para crear EXACTAMENTE %d preguntas.

                Debes respetar el modelo de SmartLearn, equivalente al Excel con columnas:
                pregunta, tipo, opcion_a, opcion_b, opcion_c, opcion_d, respuesta_correcta, explicacion, puntaje, temporizador_segundos.

                INSTRUCCIONES DEL USUARIO:
                %s

                FORMATO DE SALIDA OBLIGATORIO (solo JSON valido, sin markdown):
                {
                  "questions": [
                    {
                      "questionText": "texto",
                      "questionType": "multiple_choice|written",
                      "options": ["opcion A", "opcion B", "opcion C", "opcion D"],
                      "correctOption": "a|b|c|d",
                      "correctAnswer": "respuesta escrita",
                      "explanation": "explicacion breve",
                      "points": 1,
                      "temporizadorSegundos": 30
                    }
                  ]
                }

                REGLAS:
                - Para "multiple_choice": incluir entre 2 y 4 opciones no vacias y "correctOption" valido.
                - Para "written": dejar "options" vacio y completar "correctAnswer".
                - "points" entre 1 y 20.
                - "temporizadorSegundos" entre 15 y 300.
                - "multiple_choice" equivale a tipo=seleccion del Excel.
                - "written" equivale a tipo=escrita del Excel.
                - Para "multiple_choice", "correctAnswer" puede ir vacio.
                - Para "written", deja "correctOption" en "a".
                - Usa espanol neutro.
                - Devuelve SOLO JSON.

                CONTENIDO FUENTE:
                %s
                """
                .formatted(questionsCount, instructions, sourceText);
    }

    private String buildOllamaPayload(String prompt, String modelName) {
        try {
            JsonNode payload = objectMapper.createObjectNode()
                    .put("model", modelName)
                    .put("prompt", prompt)
                    .put("stream", false)
                    .put("format", "json");
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo preparar la solicitud a Ollama.");
        }
    }

    private List<String> fetchAvailableModelsFromOllama() {
        try {
            String endpoint = normalizeBaseUrl(ollamaBaseUrl) + "/api/tags";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of(ollamaModel);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode modelsNode = root.path("models");
            if (!modelsNode.isArray()) {
                return List.of(ollamaModel);
            }

            Set<String> names = new LinkedHashSet<>();
            for (JsonNode modelNode : modelsNode) {
                String modelName = modelNode.path("name").asText("").trim();
                if (!modelName.isBlank()) {
                    names.add(modelName);
                }
            }

            if (names.isEmpty()) {
                names.add(ollamaModel);
            }
            return new ArrayList<>(names);
        } catch (Exception exception) {
            return List.of(ollamaModel);
        }
    }

    private String callOllama(String payload) {
        try {
            String endpoint = normalizeBaseUrl(ollamaBaseUrl) + "/api/generate";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException("Ollama respondio con error: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String modelResponse = root.path("response").asText("");
            if (modelResponse.isBlank()) {
                throw new BadRequestException("Ollama no devolvio contenido para generar el examen.");
            }
            return modelResponse;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("No se pudo conectar con Ollama local.");
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo conectar con Ollama local.");
        }
    }

    private List<GeneratedQuestion> parseGeneratedQuestions(String modelResponse, int requestedCount) {
        try {
            JsonNode root = parseJsonLenient(modelResponse);
            JsonNode questionsNode = root.path("questions");
            if (!questionsNode.isArray()) {
                throw new BadRequestException("El modelo no devolvio un arreglo de preguntas valido.");
            }

            List<GeneratedQuestion> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                GeneratedQuestion question = toGeneratedQuestion(node);
                if (question.questionText().isBlank()) {
                    continue;
                }
                questions.add(question);
                if (questions.size() >= requestedCount) {
                    break;
                }
            }

            if (questions.size() < MIN_QUESTIONS) {
                throw new BadRequestException("El modelo devolvio muy pocas preguntas validas. Intenta nuevamente.");
            }

            return questions;
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo interpretar la respuesta del modelo local.");
        }
    }

    private JsonNode parseJsonLenient(String rawResponse) throws IOException {
        try {
            return objectMapper.readTree(rawResponse);
        } catch (IOException exception) {
            int start = rawResponse.indexOf('{');
            int end = rawResponse.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String maybeJson = rawResponse.substring(start, end + 1);
                return objectMapper.readTree(maybeJson);
            }
            throw exception;
        }
    }

    private GeneratedQuestion toGeneratedQuestion(JsonNode node) {
        String questionText = node.path("questionText").asText("").trim();
        String questionType = node.path("questionType").asText("written").trim().toLowerCase(Locale.ROOT);
        String explanation = node.path("explanation").asText("").trim();
        int points = clampInt(node.path("points").asInt(1), 1, 20);
        int timer = clampInt(node.path("temporizadorSegundos").asInt(30), 15, 300);

        List<String> options = new ArrayList<>();
        if (node.path("options").isArray()) {
            Set<String> seen = new HashSet<>();
            for (JsonNode optionNode : node.path("options")) {
                String option = optionNode.asText("").trim();
                if (!option.isBlank() && seen.add(option.toLowerCase(Locale.ROOT))) {
                    options.add(option);
                }
                if (options.size() >= 4) {
                    break;
                }
            }
        }

        String correctOption = node.path("correctOption").asText("a").trim().toLowerCase(Locale.ROOT);
        String correctAnswer = node.path("correctAnswer").asText("").trim();

        if (!questionType.equals("multiple_choice")) {
            questionType = "written";
        }

        return new GeneratedQuestion(questionText, questionType, options, correctOption, correctAnswer, explanation, points, timer);
    }

    private Exam createExamFromGeneratedQuestions(User user, AiChat chat, String examName, List<GeneratedQuestion> generatedQuestions) {
        Exam exam = new Exam();
        exam.setUser(user);
        exam.setName(examName);
        exam.setSourceFilePath("ai://" + chat.getId() + "/generated.xlsx");
        exam.setQuestionsCount(0);
        exam = examRepository.save(exam);
        exam = ensureExamCode(exam);

        int saved = 0;
        for (GeneratedQuestion generated : generatedQuestions) {
            Question question = new Question();
            question.setExam(exam);
            question.setQuestionText(generated.questionText());
            question.setExplanation(generated.explanation().isBlank() ? null : generated.explanation());
            question.setPoints(generated.points());
            question.setTimeLimit(generated.temporizadorSegundos());
            question.setTemporizadorSegundos(generated.temporizadorSegundos());
            question.setReviewSeconds(10);
            question.setTimerEnabled(Boolean.TRUE);

            if (generated.questionType().equals("multiple_choice") && generated.options().size() >= 2) {
                question.setQuestionType("multiple_choice");
                int correctIndex = resolveCorrectIndex(generated.correctOption(), generated.options().size());
                String correctAnswer = generated.options().get(correctIndex);
                question.setCorrectAnswer(correctAnswer);
                question = questionRepository.save(question);

                for (int index = 0; index < generated.options().size(); index++) {
                    Option option = new Option();
                    option.setQuestion(question);
                    option.setOptionText(generated.options().get(index));
                    option.setIsCorrect(index == correctIndex);
                    optionRepository.save(option);
                }
            } else {
                question.setQuestionType("written");
                String correctAnswer = generated.correctAnswer().isBlank()
                        ? "Respuesta esperada no especificada."
                        : generated.correctAnswer();
                question.setCorrectAnswer(correctAnswer);
                questionRepository.save(question);
            }
            saved++;
        }

        if (saved <= 0) {
            throw new BadRequestException("No se pudieron generar preguntas validas para el examen.");
        }

        exam.setQuestionsCount(saved);
        return examRepository.save(exam);
    }

    private Exam ensureExamCode(Exam exam) {
        if (exam == null || exam.getId() == null) {
            return exam;
        }
        String currentCode = exam.getCode() == null ? null : exam.getCode().trim();
        if (currentCode != null && !currentCode.isEmpty()) {
            return exam;
        }
        exam.setCode(String.format(Locale.ROOT, "EXM-%06d", Math.max(0L, exam.getId())));
        return examRepository.save(exam);
    }

    private int resolveCorrectIndex(String correctOption, int optionsSize) {
        return switch (correctOption) {
            case "b" -> optionsSize > 1 ? 1 : 0;
            case "c" -> optionsSize > 2 ? 2 : 0;
            case "d" -> optionsSize > 3 ? 3 : 0;
            default -> 0;
        };
    }

    private String buildGenerationSummaryMessage(
            List<MultipartFile> files,
            String instructions,
            int questionsCount,
            String examName,
            String modelName) {
        String attachments = files.stream()
                .map(file -> safeName(file.getOriginalFilename()))
                .filter(name -> !name.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse("PDF/Word");

        return """
                Generar examen desde documentos.
                Nombre objetivo: %s
                Modelo: %s
                Preguntas: %d
                Instrucciones: %s
                Archivos: %s
                """.formatted(examName, modelName, questionsCount, instructions, attachments).trim();
    }

    private void createMessage(AiChat chat, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setChat(chat);
        message.setRole(role);
        message.setContent(normalizeChatMessage(content));
        messageRepository.save(message);
    }

    private String normalizeChatMessage(String content) {
        String safe = content == null ? "" : content.trim();
        if (safe.length() <= MAX_CHAT_MESSAGE_LENGTH) {
            return safe;
        }
        return safe.substring(0, MAX_CHAT_MESSAGE_LENGTH - 3).trim() + "...";
    }

    private ChatDetailResponse toChatDetail(AiChat chat) {
        List<ChatMessageResponse> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId()).stream()
                .map(message -> new ChatMessageResponse(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
        return new ChatDetailResponse(chat.getId(), chat.getName(), messages);
    }

    private String safeName(String value) {
        return value == null ? "" : value.trim();
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isSupportedSourceFile(String lowerName) {
        return lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx");
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:11434";
        }
        String value = baseUrl.trim();
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record GeneratedQuestion(
            String questionText,
            String questionType,
            List<String> options,
            String correctOption,
            String correctAnswer,
            String explanation,
            Integer points,
            Integer temporizadorSegundos) {
        private GeneratedQuestion {
            Objects.requireNonNull(questionText);
            Objects.requireNonNull(questionType);
            Objects.requireNonNull(options);
            Objects.requireNonNull(correctOption);
            Objects.requireNonNull(correctAnswer);
            Objects.requireNonNull(explanation);
            Objects.requireNonNull(points);
            Objects.requireNonNull(temporizadorSegundos);
        }
    }
}

