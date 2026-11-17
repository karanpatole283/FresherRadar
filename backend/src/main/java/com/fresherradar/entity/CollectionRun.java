package com.fresherradar.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "collection_runs")
public class CollectionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int fetched;

    @Column(nullable = false)
    private int inserted;

    @Column(nullable = false)
    private int duplicates;

    @Column(name = "location_rejected", nullable = false)
    private int locationRejected;

    @Column(name = "stale_or_invalid", nullable = false)
    private int staleOrInvalid;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public Long getId() { return id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFetched() { return fetched; }
    public void setFetched(int fetched) { this.fetched = fetched; }

    public int getInserted() { return inserted; }
    public void setInserted(int inserted) { this.inserted = inserted; }

    public int getDuplicates() { return duplicates; }
    public void setDuplicates(int duplicates) { this.duplicates = duplicates; }

    public int getLocationRejected() { return locationRejected; }
    public void setLocationRejected(int locationRejected) {
        this.locationRejected = locationRejected;
    }

    public int getStaleOrInvalid() { return staleOrInvalid; }
    public void setStaleOrInvalid(int staleOrInvalid) {
        this.staleOrInvalid = staleOrInvalid;
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}