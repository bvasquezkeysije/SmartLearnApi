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
@Table(name = "exams")
public class Exam extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code")
    private String code;

    @Column(name = "source_file_path", nullable = false)
    private String sourceFilePath;

    @Column(name = "questions_count", nullable = false)
    private Integer questionsCount = 0;

    @Column(name = "practice_feedback_enabled", nullable = false)
    private Boolean practiceFeedbackEnabled = Boolean.TRUE;

    @Column(name = "practice_order_mode", nullable = false)
    private String practiceOrderMode = "ordered";

    @Column(name = "practice_repeat_until_correct", nullable = false)
    private Boolean practiceRepeatUntilCorrect = Boolean.FALSE;

    @Column(name = "visibility")
    private String visibility = "private";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public Integer getQuestionsCount() {
        return questionsCount;
    }

    public void setQuestionsCount(Integer questionsCount) {
        this.questionsCount = questionsCount;
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

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
