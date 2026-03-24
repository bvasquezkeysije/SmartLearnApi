package com.bardales.SmartLearnApi.domain.entity;

import com.bardales.SmartLearnApi.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "exam_attempts")
public class ExamAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "question_ids", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String questionIds;

    @Column(name = "questions_order_mode", nullable = false)
    private String questionsOrderMode = "ordered";

    @Column(name = "feedback_enabled", nullable = false)
    private Boolean feedbackEnabled = Boolean.TRUE;

    @Column(name = "repeat_until_correct", nullable = false)
    private Boolean repeatUntilCorrect = Boolean.FALSE;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 0;

    @Column(name = "answered_count", nullable = false)
    private Integer answeredCount = 0;

    @Column(name = "unanswered_count", nullable = false)
    private Integer unansweredCount = 0;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount = 0;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "scored_points", nullable = false)
    private Integer scoredPoints = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(String questionIds) {
        this.questionIds = questionIds;
    }

    public String getQuestionsOrderMode() {
        return questionsOrderMode;
    }

    public void setQuestionsOrderMode(String questionsOrderMode) {
        this.questionsOrderMode = questionsOrderMode;
    }

    public Boolean getFeedbackEnabled() {
        return feedbackEnabled;
    }

    public void setFeedbackEnabled(Boolean feedbackEnabled) {
        this.feedbackEnabled = feedbackEnabled;
    }

    public Boolean getRepeatUntilCorrect() {
        return repeatUntilCorrect;
    }

    public void setRepeatUntilCorrect(Boolean repeatUntilCorrect) {
        this.repeatUntilCorrect = repeatUntilCorrect;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(Integer answeredCount) {
        this.answeredCount = answeredCount;
    }

    public Integer getUnansweredCount() {
        return unansweredCount;
    }

    public void setUnansweredCount(Integer unansweredCount) {
        this.unansweredCount = unansweredCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getScoredPoints() {
        return scoredPoints;
    }

    public void setScoredPoints(Integer scoredPoints) {
        this.scoredPoints = scoredPoints;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
