package com.fresherradar.collector;

import java.util.List;

public interface JobCollector {

    /** Human-readable source name stored on every job (shown in the UI). */
    String name();

    /** False when the source is switched off or its credentials are missing. */
    boolean isEnabled();

    List<RawJob> collect() throws Exception;
}
