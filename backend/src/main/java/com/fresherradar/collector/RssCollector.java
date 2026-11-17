package com.fresherradar.collector;

import com.fresherradar.util.TextUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads RSS 2.0 / Atom feeds that the operator is permitted to consume (company career feeds,
 * job boards that publish feeds for reuse, etc.). Configure with RSS_FEEDS="Name|https://...,Name2|https://...".
 */
@Component
public class RssCollector implements JobCollector {

    private static final Logger log = LoggerFactory.getLogger(RssCollector.class);

    private final HttpClient http;
    private final boolean enabled;
    private final List<String> feeds;
    private final Duration timeout;

    public RssCollector(HttpClient http,
                        @Value("${fresherradar.sources.rss.enabled}") boolean enabled,
                        @Value("${fresherradar.sources.rss.feeds}") List<String> feeds,
                        @Value("${fresherradar.collection.http-timeout-seconds}") int timeoutSeconds) {
        this.http = http;
        this.enabled = enabled;
        this.feeds = feeds.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public String name() { return "RSS"; }

    @Override
    public boolean isEnabled() { return enabled && !feeds.isEmpty(); }

    @Override
    public List<RawJob> collect() {
        List<RawJob> jobs = new ArrayList<>();
        for (String entry : feeds) {
            String feedName = entry.contains("|") ? entry.substring(0, entry.indexOf('|')).trim() : null;
            String url = entry.contains("|") ? entry.substring(entry.indexOf('|') + 1).trim() : entry;
            try {
                jobs.addAll(readFeed(feedName, url));
            } catch (Exception e) {
                // one broken feed must not stop the others
                log.warn("RSS feed {} failed: {}", url, e.getMessage());
            }
        }
        return jobs;
    }

    private List<RawJob> readFeed(String feedName, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout)
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                .header("User-Agent", "FresherRadarBot/1.0")
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());

        Document doc = Jsoup.parse(response.body(), "", Parser.xmlParser());
        String sourceLabel = feedName != null ? feedName : hostOf(url);
        List<RawJob> jobs = new ArrayList<>();

        for (Element item : doc.select("item, entry")) {
            String title = item.selectFirst("title") != null ? item.selectFirst("title").text() : "";
            String link = linkOf(item);
            if (title.isBlank() || link.isBlank()) continue;

            String body = firstText(item, "description", "content", "summary", "content|encoded");
            String company = firstText(item, "dc|creator", "author name", "author", "company", "source");
            String location = firstText(item, "location", "job_location", "city");
            String guid = firstText(item, "guid", "id");

            jobs.add(new RawJob(sourceLabel, guid.isBlank() ? link : guid,
                    TextUtils.htmlToText(title),
                    company.isBlank() ? sourceLabel : TextUtils.htmlToText(company),
                    TextUtils.htmlToText(location),
                    TextUtils.htmlToText(body),
                    link,
                    parseDate(firstText(item, "pubDate", "published", "updated", "dc|date"))));
        }
        log.info("Feed {} returned {} entries", sourceLabel, jobs.size());
        return jobs;
    }

    private static String linkOf(Element item) {
        Element link = item.selectFirst("link");
        if (link == null) return "";
        String href = link.hasAttr("href") ? link.attr("href") : link.text();
        return href.trim();
    }

    private static String firstText(Element item, String... selectors) {
        for (String s : selectors) {
            Element e = item.selectFirst(s);
            if (e != null && !e.text().isBlank()) return e.text().trim();
        }
        return "";
    }

    private static Instant parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); } catch (Exception ignored) { }
        try { return Instant.parse(value); } catch (Exception ignored) { }
        try { return ZonedDateTime.parse(value).toInstant(); } catch (Exception ignored) { }
        return null;
    }

    private static String hostOf(String url) {
        try { return URI.create(url).getHost(); } catch (Exception e) { return "RSS"; }
    }
}
