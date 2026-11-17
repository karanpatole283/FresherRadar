
package com.fresherradar.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 200)
    private String company;

    @Column(length = 120)
    private String location;

    @Column(length = 80)
    private String category;

    @Column(name = "employment_type", length = 30)
    private String employmentType;

    @Column(length = 20)
    private String experience;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(name = "source_job_id", length = 200)
    private String sourceJobId;

    @Column(name = "apply_url", nullable = false, length = 2000)
    private String applyUrl;

    @Column(name = "apply_url_key", nullable = false, length = 64)
    private String applyUrlKey;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> skills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> eligibility = new ArrayList<>();

    @Column(name = "search_text", columnDefinition = "text")
    private String searchText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "classification_confidence")
    private Double classificationConfidence;

    @Column(name = "classification_reason", length = 500)
    private String classificationReason;

    @Column(name = "classified_at")
    private Instant classifiedAt;

    @Column(name = "classification_attempts", nullable = false)
    private int classificationAttempts;

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }

    public Instant getCollectedAt() { return collectedAt; }
    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceJobId() { return sourceJobId; }
    public void setSourceJobId(String sourceJobId) {
        this.sourceJobId = sourceJobId;
    }

    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }

    public String getApplyUrlKey() { return applyUrlKey; }
    public void setApplyUrlKey(String applyUrlKey) {
        this.applyUrlKey = applyUrlKey;
    }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getEligibility() { return eligibility; }
    public void setEligibility(List<String> eligibility) {
        this.eligibility = eligibility;
    }

    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public Double getClassificationConfidence() {
        return classificationConfidence;
    }

    public void setClassificationConfidence(Double v) {
        this.classificationConfidence = v;
    }

    public String getClassificationReason() {
        return classificationReason;
    }

    public void setClassificationReason(String v) {
        this.classificationReason = v;
    }

    public Instant getClassifiedAt() { return classifiedAt; }
    public void setClassifiedAt(Instant classifiedAt) {
        this.classifiedAt = classifiedAt;
    }

    public int getClassificationAttempts() {
        return classificationAttempts;
    }

    public void setClassificationAttempts(int n) {
        this.classificationAttempts = n;
    }
}
