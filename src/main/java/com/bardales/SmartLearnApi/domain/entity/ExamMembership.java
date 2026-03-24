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
@Table(name = "exam_memberships")
public class ExamMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role", nullable = false)
    private String role = "viewer";

    @Column(name = "can_share", nullable = false)
    private Boolean canShare = Boolean.FALSE;

    @Column(name = "can_start_group", nullable = false)
    private Boolean canStartGroup = Boolean.FALSE;

    @Column(name = "can_rename_exam", nullable = false)
    private Boolean canRenameExam = Boolean.FALSE;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getCanShare() {
        return canShare;
    }

    public void setCanShare(Boolean canShare) {
        this.canShare = canShare;
    }

    public Boolean getCanStartGroup() {
        return canStartGroup;
    }

    public void setCanStartGroup(Boolean canStartGroup) {
        this.canStartGroup = canStartGroup;
    }

    public Boolean getCanRenameExam() {
        return canRenameExam;
    }

    public void setCanRenameExam(Boolean canRenameExam) {
        this.canRenameExam = canRenameExam;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
