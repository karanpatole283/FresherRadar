package com.fresherradar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fresherradar.classifier.ClassificationResult;
import com.fresherradar.classifier.JobClassifier;
import com.fresherradar.classifier.LlmClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobClassifierTest {

    private JobClassifier classifierReturning(String text) {
        LlmClient fake = new LlmClient() {
            public boolean isConfigured() { return true; }
            public String modelName() { return "fake"; }
            public LlmResponse complete(String s, String u) { return new LlmResponse(text, 100, 50, 5); }
        };
        return new JobClassifier(fake, new ObjectMapper());
    }

    @Test
    void parsesValidJsonAndNormalisesVocabulary() {
        String json = """
                Here you go: {"fresher_eligible": true, "employment_type": "internship", "experience": "0 years",
                "category": "ai / ml", "city": "Pune", "company": "Acme", "skills": ["Python", "SQL"],
                "eligibility": ["B.Tech"], "confidence": 0.9, "reason": "Intern role"}""";
        JobClassifier.Outcome o = classifierReturning(json).classify("ML Intern", "Acme", "Pune", "desc");
        assertTrue(o.success());
        ClassificationResult r = o.result();
        assertTrue(r.fresherEligible());
        assertEquals("Internship", r.employmentType());
        assertEquals("AI / ML", r.category());
        assertEquals(100, o.usage().inputTokens());
    }

    @Test
    void unknownCategoryFallsBackToOther() {
        String json = "{\"fresher_eligible\":true,\"employment_type\":\"Full-time\",\"experience\":\"0-1 years\",\"category\":\"Astrology\",\"confidence\":0.8}";
        assertEquals("Other", classifierReturning(json).classify("t", "c", "l", "d").result().category());
    }

    @Test
    void garbageResponseIsReportedAsFailureButKeepsUsage() {
        JobClassifier.Outcome o = classifierReturning("sorry, I cannot help").classify("t", "c", "l", "d");
        assertFalse(o.success());
        assertNotNull(o.error());
        assertEquals(50, o.usage().outputTokens());
    }
}
