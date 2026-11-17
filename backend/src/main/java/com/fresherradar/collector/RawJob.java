package com.fresherradar.collector;

import java.time.Instant;

/** A job exactly as a source reported it, before classification. */
public record RawJob(
        String source,
        String sourceJobId,
        String title,
        String company,
        String location,
        String description,
        String applyUrl,
        Instant postedAt) {
}
