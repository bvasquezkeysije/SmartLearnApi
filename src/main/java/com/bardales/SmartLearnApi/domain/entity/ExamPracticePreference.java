package com.bardales.SmartLearnApi.domain.entity;

import com.bardales.SmartLearnApi.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_practice_preferences")
public class ExamPracticePreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "practice_feedback_enabled", nullable = false)
    private Boolean practiceFeedbackEnabled = Boolean.TRUE;

    @Column(name = "practice_order_mode", nullable = false)
    private String practiceOrderMode = "ordered";

    @Column(name = "practice_repeat_until_correct", nullable = false)
    private Boolean practiceRepeatUntilCorrect = Boolean.TRUE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    public Boolean getPracticeFeedbackEnabled() {
        return practiceFeedbackEnabled;
    }

    public void setPracticeFeedbackEnabled(Boolean practiceFeedbackEnabled) {
        this.practiceFeedbackEnabled = practiceFeedbackEnabled;
    }

    public String getPracticeOrderMode() {
        return practiceOrderMode;
    }

    public void setPracticeOrderMode(String practiceOrderMode) {
        this.practiceOrderMode = practiceOrderMode;
    }

    public Boolean getPracticeRepeatUntilCorrect() {
        return practiceRepeatUntilCorrect;
    }

    public void setPracticeRepeatUntilCorrect(Boolean practiceRepeatUntilCorrect) {
        this.practiceRepeatUntilCorrect = practiceRepeatUntilCorrect;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
