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
@Table(name = "course_sessions")
public class CourseSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "weekly_content")
    private String weeklyContent;

    @Column(name = "video_title")
    private String videoTitle;

    @Column(name = "video_link")
    private String videoLink;

    @Column(name = "cover_title")
    private String coverTitle;

    @Column(name = "cover_image_data", columnDefinition = "TEXT")
    private String coverImageData;

    @Column(name = "pdf_title")
    private String pdfTitle;

    @Column(name = "pdf_file_name")
    private String pdfFileName;

    @Column(name = "pdf_file_data", columnDefinition = "TEXT")
    private String pdfFileData;

    @Column(name = "word_title")
    private String wordTitle;

    @Column(name = "word_file_name")
    private String wordFileName;

    @Column(name = "word_file_data", columnDefinition = "TEXT")
    private String wordFileData;

    @Column(name = "links_text", columnDefinition = "TEXT")
    private String linksText;

    @Column(name = "material_name")
    private String materialName;

    @Column(name = "material_file_name")
    private String materialFileName;

    @Column(name = "material_file_data", columnDefinition = "TEXT")
    private String materialFileData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloned_exam_id")
    private Exam clonedExam;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWeeklyContent() {
        return weeklyContent;
    }

    public void setWeeklyContent(String weeklyContent) {
        this.weeklyContent = weeklyContent;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public void setVideoLink(String videoLink) {
        this.videoLink = videoLink;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getCoverImageData() {
        return coverImageData;
    }

    public void setCoverImageData(String coverImageData) {
        this.coverImageData = coverImageData;
    }

    public String getCoverTitle() {
        return coverTitle;
    }

    public void setCoverTitle(String coverTitle) {
        this.coverTitle = coverTitle;
    }

    public String getPdfFileName() {
        return pdfFileName;
    }

    public void setPdfFileName(String pdfFileName) {
        this.pdfFileName = pdfFileName;
    }

    public String getPdfTitle() {
        return pdfTitle;
    }

    public void setPdfTitle(String pdfTitle) {
        this.pdfTitle = pdfTitle;
    }

    public String getPdfFileData() {
        return pdfFileData;
    }

    public void setPdfFileData(String pdfFileData) {
        this.pdfFileData = pdfFileData;
    }

    public String getWordFileName() {
        return wordFileName;
    }

    public void setWordFileName(String wordFileName) {
        this.wordFileName = wordFileName;
    }

    public String getWordTitle() {
        return wordTitle;
    }

    public void setWordTitle(String wordTitle) {
        this.wordTitle = wordTitle;
    }

    public String getWordFileData() {
        return wordFileData;
    }

    public void setWordFileData(String wordFileData) {
        this.wordFileData = wordFileData;
    }

    public String getLinksText() {
        return linksText;
    }

    public void setLinksText(String linksText) {
        this.linksText = linksText;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialFileName() {
        return materialFileName;
    }

    public void setMaterialFileName(String materialFileName) {
        this.materialFileName = materialFileName;
    }

    public String getMaterialFileData() {
        return materialFileData;
    }

    public void setMaterialFileData(String materialFileData) {
        this.materialFileData = materialFileData;
    }

    public Exam getClonedExam() {
        return clonedExam;
    }

    public void setClonedExam(Exam clonedExam) {
        this.clonedExam = clonedExam;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
