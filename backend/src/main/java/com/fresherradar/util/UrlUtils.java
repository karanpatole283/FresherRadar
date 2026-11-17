package com.fresherradar.util;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class UrlUtils {

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "ref", "source", "gclid", "fbclid");

    private UrlUtils() {}

    /** Only absolute http/https URLs are accepted (the frontend also enforces this). */
    public static boolean isHttpUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Lower-cases the host, drops the fragment, trailing slash and tracking parameters. */
    public static String canonicalize(String url) {
        URI uri = URI.create(url.trim());
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);

        String query = "";
        if (uri.getRawQuery() != null) {
            query = Arrays.stream(uri.getRawQuery().split("&"))
                    .filter(p -> !p.isBlank())
                    .filter(p -> !TRACKING_PARAMS.contains(p.split("=", 2)[0].toLowerCase(Locale.ROOT)))
                    .sorted()
                    .collect(Collectors.joining("&"));
        }
        return host + path + (query.isEmpty() ? "" : "?" + query);
    }
}
