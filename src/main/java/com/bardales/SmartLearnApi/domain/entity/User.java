package com.bardales.SmartLearnApi.domain.entity;

import com.bardales.SmartLearnApi.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "username")
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "remember_token")
    private String rememberToken;

    @Column(name = "auth_provider")
    private String authProvider;

    @Column(name = "google_subject")
    private String googleSubject;

    @Column(name = "google_picture_url")
    private String googlePictureUrl;

    @Column(name = "profile_image_data", columnDefinition = "TEXT")
    private String profileImageData;

    @Column(name = "profile_image_scale")
    private Double profileImageScale;

    @Column(name = "profile_image_offset_x")
    private Double profileImageOffsetX;

    @Column(name = "profile_image_offset_y")
    private Double profileImageOffsetY;

    @Column(name = "has_local_password")
    private Boolean hasLocalPassword;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRememberToken() {
        return rememberToken;
    }

    public void setRememberToken(String rememberToken) {
        this.rememberToken = rememberToken;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public void setGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public String getGooglePictureUrl() {
        return googlePictureUrl;
    }

    public void setGooglePictureUrl(String googlePictureUrl) {
        this.googlePictureUrl = googlePictureUrl;
    }

    public String getProfileImageData() {
        return profileImageData;
    }

    public void setProfileImageData(String profileImageData) {
        this.profileImageData = profileImageData;
    }

    public Double getProfileImageScale() {
        return profileImageScale;
    }

    public void setProfileImageScale(Double profileImageScale) {
        this.profileImageScale = profileImageScale;
    }

    public Double getProfileImageOffsetX() {
        return profileImageOffsetX;
    }

    public void setProfileImageOffsetX(Double profileImageOffsetX) {
        this.profileImageOffsetX = profileImageOffsetX;
    }

    public Double getProfileImageOffsetY() {
        return profileImageOffsetY;
    }

    public void setProfileImageOffsetY(Double profileImageOffsetY) {
        this.profileImageOffsetY = profileImageOffsetY;
    }

    public Boolean getHasLocalPassword() {
        return hasLocalPassword;
    }

    public void setHasLocalPassword(Boolean hasLocalPassword) {
        this.hasLocalPassword = hasLocalPassword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(role));
    }
}
