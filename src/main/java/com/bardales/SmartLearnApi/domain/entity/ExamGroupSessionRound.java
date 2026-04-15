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
@Table(name = "exam_group_session_rounds")
public class ExamGroupSessionRound extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamGroupSession session;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "phase", nullable = false)
    private String phase = "open";

    @Column(name = "open_started_at")
    private LocalDateTime openStartedAt;

    @Column(name = "open_ends_at")
    private LocalDateTime openEndsAt;

    @Column(name = "review_started_at")
    private LocalDateTime reviewStartedAt;

    @Column(name = "review_ends_at")
    private LocalDateTime reviewEndsAt;

    @Column(name = "close_reason")
    private String closeReason;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public ExamGroupSession getSession() {
        return session;
    }

    public void setSession(ExamGroupSession session) {
        this.session = session;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public LocalDateTime getOpenStartedAt() {
        return openStartedAt;
    }

    public void setOpenStartedAt(LocalDateTime openStartedAt) {
        this.openStartedAt = openStartedAt;
    }

    public LocalDateTime getOpenEndsAt() {
        return openEndsAt;
    }

    public void setOpenEndsAt(LocalDateTime openEndsAt) {
        this.openEndsAt = openEndsAt;
    }

    public LocalDateTime getReviewStartedAt() {
        return reviewStartedAt;
    }

    public void setReviewStartedAt(LocalDateTime reviewStartedAt) {
        this.reviewStartedAt = reviewStartedAt;
    }

    public LocalDateTime getReviewEndsAt() {
        return reviewEndsAt;
    }

    public void setReviewEndsAt(LocalDateTime reviewEndsAt) {
        this.reviewEndsAt = reviewEndsAt;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}

