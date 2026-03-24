package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bardales.SmartLearnApi.domain.repository.ExamAttemptRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamGroupSessionRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamPracticePreferenceRepository;
import com.bardales.SmartLearnApi.domain.repository.ExamRepository;
import com.bardales.SmartLearnApi.domain.repository.OptionRepository;
import com.bardales.SmartLearnApi.domain.repository.QuestionRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamServiceFormatTemplateTest {

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamMembershipRepository examMembershipRepository;

    @Mock
    private ExamGroupSessionRepository examGroupSessionRepository;

        @Mock
        private ExamPracticePreferenceRepository examPracticePreferenceRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void downloadExamFormatTemplateContainsReviewSecondsHeader() throws Exception {
        ExamService examService = new ExamService(
                examAttemptRepository,
                examRepository,
                examMembershipRepository,
                examPracticePreferenceRepository,
                examGroupSessionRepository,
                questionRepository,
                optionRepository,
                userRepository);

        byte[] file = examService.downloadExamFormatTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            DataFormatter formatter = new DataFormatter();

            List<String> headers = List.of(
                    formatter.formatCellValue(headerRow.getCell(0)),
                    formatter.formatCellValue(headerRow.getCell(1)),
                    formatter.formatCellValue(headerRow.getCell(2)),
                    formatter.formatCellValue(headerRow.getCell(3)),
                    formatter.formatCellValue(headerRow.getCell(4)),
                    formatter.formatCellValue(headerRow.getCell(5)),
                    formatter.formatCellValue(headerRow.getCell(6)),
                    formatter.formatCellValue(headerRow.getCell(7)),
                    formatter.formatCellValue(headerRow.getCell(8)),
                    formatter.formatCellValue(headerRow.getCell(9)),
                    formatter.formatCellValue(headerRow.getCell(10)),
                    formatter.formatCellValue(headerRow.getCell(11)),
                    formatter.formatCellValue(headerRow.getCell(12)));

            assertEquals(
                    List.of(
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
                            "temporizador"),
                    headers);
        }
    }
}
