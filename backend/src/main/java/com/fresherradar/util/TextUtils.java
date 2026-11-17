package com.fresherradar.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextUtils {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern COMPANY_SUFFIX =
            Pattern.compile("\\b(pvt|private|ltd|limited|llp|inc|llc|corp|corporation|technologies|technology|solutions)\\b");

    private TextUtils() {}

    /** Strips all HTML tags and collapses whitespace. */
    public static String htmlToText(String html) {
        if (html == null || html.isBlank()) return "";
        String text = Jsoup.clean(html, "", Safelist.none(), new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false));
        text = Jsoup.parse(text).text();           // decode entities like &amp;
        return text.replaceAll("\\s+", " ").trim();
    }

    public static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Lower-case, accent-free, alphanumeric-only form used for duplicate comparison. */
    public static String normalizeForKey(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        return NON_ALNUM.matcher(s.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    public static String normalizeCompany(String company) {
        String s = normalizeForKey(company);
        return COMPANY_SUFFIX.matcher(s).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    /** Same job posted twice (even on different sites) should produce the same fingerprint. */
    public static String fingerprint(String title, String company, String city) {
        String basis = normalizeForKey(title) + "|" + normalizeCompany(company) + "|" + normalizeForKey(city);
        return sha256(basis);
    }

    public static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
