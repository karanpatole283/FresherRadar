package com.fresherradar;

import com.fresherradar.util.CsvUtils;
import com.fresherradar.util.TextUtils;
import com.fresherradar.util.UrlUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void canonicalUrlIgnoresTrackingParamsCaseAndTrailingSlash() {
        String a = UrlUtils.canonicalize("https://Example.com/jobs/123/?utm_source=x&id=9#apply");
        String b = UrlUtils.canonicalize("https://example.com/jobs/123?id=9");
        assertEquals(a, b);
    }

    @Test
    void onlyHttpUrlsAreAccepted() {
        assertTrue(UrlUtils.isHttpUrl("https://example.com/apply"));
        assertFalse(UrlUtils.isHttpUrl("javascript:alert(1)"));
        assertFalse(UrlUtils.isHttpUrl("ftp://example.com/x"));
        assertFalse(UrlUtils.isHttpUrl(""));
        assertFalse(UrlUtils.isHttpUrl(null));
    }

    @Test
    void fingerprintMatchesAcrossFormattingDifferences() {
        String a = TextUtils.fingerprint("Software Engineer - Fresher", "Acme Technologies Pvt. Ltd.", "Pune");
        String b = TextUtils.fingerprint("software engineer fresher", "ACME", "pune");
        assertEquals(a, b);
        assertNotEquals(a, TextUtils.fingerprint("Software Engineer - Fresher", "Acme", "Mumbai"));
    }

    @Test
    void htmlIsStrippedToPlainText() {
        assertEquals("Hello & welcome", TextUtils.htmlToText("<p>Hello &amp; <b>welcome</b></p>"));
        assertEquals("", TextUtils.htmlToText(null));
    }

    @Test
    void csvParserHandlesQuotesAndEmbeddedNewlines() {
        List<List<String>> rows = CsvUtils.parse("a,b\n\"x, y\",\"line1\nline2 \"\"q\"\"\"\n");
        assertEquals(2, rows.size());
        assertEquals("x, y", rows.get(1).get(0));
        assertEquals("line1\nline2 \"q\"", rows.get(1).get(1));
    }
}
