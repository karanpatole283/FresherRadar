
package com.fresherradar.dto;

import java.time.Instant;

public record CollectionRunDto(
        Long id,
        String source,
        Instant startedAt,
        Instant finishedAt,
        String status,
        int fetched,
        int inserted,
        int duplicates,
        int locationRejected,
        int staleOrInvalid,
        String errorMessage
) {
}
