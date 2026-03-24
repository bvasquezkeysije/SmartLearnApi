package com.bardales.SmartLearnApi.domain.entity;

import com.bardales.SmartLearnApi.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "questions")
public class Question extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "time_limit", nullable = false)
    private Integer timeLimit;

    @Column(name = "temporizador_segundos")
    private Integer temporizadorSegundos;

    @Column(name = "timer_enabled", nullable = false)
    private Boolean timerEnabled = Boolean.TRUE;

    @Column(name = "review_seconds", nullable = false)
    private Integer reviewSeconds = 10;

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Integer getTemporizadorSegundos() {
        return temporizadorSegundos;
    }

    public void setTemporizadorSegundos(Integer temporizadorSegundos) {
        this.temporizadorSegundos = temporizadorSegundos;
    }

    public Boolean getTimerEnabled() {
        return timerEnabled;
    }

    public void setTimerEnabled(Boolean timerEnabled) {
        this.timerEnabled = timerEnabled;
    }

    public Integer getReviewSeconds() {
        return reviewSeconds;
    }

    public void setReviewSeconds(Integer reviewSeconds) {
        this.reviewSeconds = reviewSeconds;
    }
}
