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
@Table(name = "exam_group_session_members")
public class ExamGroupSessionMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamGroupSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "connected", nullable = false)
    private Boolean connected = Boolean.TRUE;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "room_session_token", length = 128)
    private String roomSessionToken;

    @Column(name = "room_session_issued_at")
    private LocalDateTime roomSessionIssuedAt;

    @Column(name = "room_session_expires_at")
    private LocalDateTime roomSessionExpiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public ExamGroupSession getSession() {
        return session;
    }

    public void setSession(ExamGroupSession session) {
        this.session = session;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getRoomSessionToken() {
        return roomSessionToken;
    }

    public void setRoomSessionToken(String roomSessionToken) {
        this.roomSessionToken = roomSessionToken;
    }

    public LocalDateTime getRoomSessionIssuedAt() {
        return roomSessionIssuedAt;
    }

    public void setRoomSessionIssuedAt(LocalDateTime roomSessionIssuedAt) {
        this.roomSessionIssuedAt = roomSessionIssuedAt;
    }

    public LocalDateTime getRoomSessionExpiresAt() {
        return roomSessionExpiresAt;
    }

    public void setRoomSessionExpiresAt(LocalDateTime roomSessionExpiresAt) {
        this.roomSessionExpiresAt = roomSessionExpiresAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
