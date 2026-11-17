package com.fresherradar.entity;

public enum JobStatus {
    /** Collected, waiting for LLM classification. Never shown to users. */
    PENDING,
    /** Classified as a genuine fresher/internship role. Shown to users. */
    ACTIVE,
    /** Classified as not suitable for freshers. */
    REJECTED,
    /** Same job already exists (found after LLM normalisation). */
    DUPLICATE,
    /** LLM classification failed repeatedly. */
    FAILED
}
