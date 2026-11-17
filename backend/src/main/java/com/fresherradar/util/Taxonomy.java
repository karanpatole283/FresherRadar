package com.fresherradar.util;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The fixed vocabularies the frontend filters on. The LLM must choose from these. */
public final class Taxonomy {

    public static final List<String> CATEGORIES = List.of(
            "Software Engineering", "AI / ML", "Data & Analytics", "DevOps & Cloud", "Quality Assurance",
            "Cybersecurity", "Design", "Product & Project Management", "Marketing", "Sales & Business Development",
            "Finance & Accounting", "Operations & Supply Chain", "Customer Support", "HR & Recruiting",
            "Content & Writing", "Other");

    public static final List<String> EMPLOYMENT_TYPES = List.of("Full-time", "Internship");

    public static final List<String> EXPERIENCE_LEVELS = List.of("0 years", "0-1 years", "0-2 years");

    private Taxonomy() {}

    public static Optional<String> matchCategory(String value) { return match(CATEGORIES, value); }
    public static Optional<String> matchEmploymentType(String value) { return match(EMPLOYMENT_TYPES, value); }
    public static Optional<String> matchExperience(String value) {
        if (value == null) return Optional.empty();
        return match(EXPERIENCE_LEVELS, value.replace('–', '-').replace(" - ", "-"));
    }

    private static Optional<String> match(List<String> allowed, String value) {
        if (value == null) return Optional.empty();
        String v = value.trim().toLowerCase(Locale.ROOT);
        return allowed.stream().filter(a -> a.toLowerCase(Locale.ROOT).equals(v)).findFirst();
    }
}
