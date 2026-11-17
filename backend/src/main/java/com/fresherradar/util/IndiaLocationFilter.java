
package com.fresherradar.util;

import java.util.List;
import java.util.Locale;

public final class IndiaLocationFilter {

    private IndiaLocationFilter() {
    }

    private static final List<String> INDIAN_LOCATIONS = List.of(
            "india",
            "bengaluru",
            "bangalore",
            "pune",
            "mumbai",
            "hyderabad",
            "chennai",
            "delhi",
            "new delhi",
            "ncr",
            "noida",
            "gurugram",
            "gurgaon",
            "kolkata",
            "jaipur",
            "ahmedabad",
            "kochi",
            "cochin",
            "indore",
            "bhopal",
            "lucknow",
            "chandigarh",
            "nagpur",
            "surat",
            "vadodara",
            "thiruvananthapuram",
            "trivandrum",
            "visakhapatnam",
            "vizag",
            "coimbatore",
            "mysuru",
            "mysore",
            "mangalore",
            "mangaluru",
            "bhubaneswar",
            "patna",
            "ranchi",
            "dehradun",
            "guwahati"
    );

    private static final List<String> INTERNATIONAL_LOCATIONS = List.of(
            "united states",
            "usa",
            "us only",
            "uk only",
            "united kingdom",
            "london",
            "canada",
            "australia",
            "singapore",
            "germany",
            "france",
            "ireland",
            "netherlands",
            "dubai",
            "uae",
            "new york",
            "san francisco",
            "seattle",
            "california",
            "texas"
    );

    public static boolean isIndiaBased(String location) {

        if (location == null || location.isBlank()) {
            return false;
        }

        String normalized = location
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        if (normalized.isBlank()) {
            return false;
        }

        for (String international : INTERNATIONAL_LOCATIONS) {
            if (containsPhrase(normalized, international)) {
                return false;
            }
        }

        for (String indian : INDIAN_LOCATIONS) {
            if (containsPhrase(normalized, indian)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsPhrase(String text, String phrase) {
        return (" " + text + " ").contains(" " + phrase + " ");
    }
}
