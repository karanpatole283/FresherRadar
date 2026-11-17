package com.fresherradar.service;

import com.fresherradar.classifier.ClassificationResult;
import com.fresherradar.classifier.JobClassifier;
import com.fresherradar.dto.EvaluationResult;
import com.fresherradar.exception.LlmException;
import com.fresherradar.util.CsvUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Measures classification accuracy against a HUMAN-labelled CSV. No labels are shipped with the project:
 * accuracy can only be reported after you label real collected jobs (see docs/EVALUATION.md).
 * Columns: title,company,location,description,expected_fresher_eligible,expected_category,expected_employment_type
 */
@Service
public class EvaluationService {

    private final JobClassifier classifier;
    private final CostCalculator calculator;
    private final Path file;

    public EvaluationService(JobClassifier classifier, CostCalculator calculator,
                             @Value("${fresherradar.evaluation.file}") String file) {
        this.classifier = classifier;
        this.calculator = calculator;
        this.file = Path.of(file);
    }

    public EvaluationResult run() {
        if (!classifier.isAvailable()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "LLM_API_KEY is not configured.");
        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Labelled file not found at " + file + ". See docs/EVALUATION.md.");
        }
        List<List<String>> rows;
        try {
            rows = CsvUtils.parse(Files.readString(file));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the labelled file.");
        }
        if (rows.size() < 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The labelled file has no data rows.");

        List<String> header = rows.get(0).stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
        int iTitle = col(header, "title"), iCompany = col(header, "company"), iLoc = col(header, "location"),
                iDesc = col(header, "description"), iElig = col(header, "expected_fresher_eligible"),
                iCat = col(header, "expected_category"), iType = col(header, "expected_employment_type");

        int classified = 0, failed = 0, eligOk = 0, catOk = 0, catTotal = 0, typeOk = 0, typeTotal = 0;
        long in = 0, out = 0;
        List<String> mismatches = new ArrayList<>();

        for (List<String> row : rows.subList(1, rows.size())) {
            try {
                JobClassifier.Outcome o = classifier.classify(get(row, iTitle), get(row, iCompany), get(row, iLoc), get(row, iDesc));
                in += o.usage().inputTokens();
                out += o.usage().outputTokens();
                if (!o.success()) { failed++; continue; }
                classified++;
                ClassificationResult r = o.result();
                boolean expectedElig = Boolean.parseBoolean(get(row, iElig).trim());
                if (r.fresherEligible() == expectedElig) eligOk++;
                else mismatches.add("eligible: '" + get(row, iTitle) + "' expected " + expectedElig + " got " + r.fresherEligible());

                if (expectedElig && !get(row, iCat).isBlank()) {
                    catTotal++;
                    if (r.category().equalsIgnoreCase(get(row, iCat).trim())) catOk++;
                    else mismatches.add("category: '" + get(row, iTitle) + "' expected " + get(row, iCat) + " got " + r.category());
                }
                if (expectedElig && !get(row, iType).isBlank()) {
                    typeTotal++;
                    if (r.employmentType().equalsIgnoreCase(get(row, iType).trim())) typeOk++;
                }
            } catch (LlmException e) {
                failed++;
            }
        }
        int samples = rows.size() - 1;
        BigDecimal cost = calculator.cost((int) Math.min(in, Integer.MAX_VALUE), (int) Math.min(out, Integer.MAX_VALUE));
        BigDecimal per1000 = samples == 0 ? BigDecimal.ZERO
                : cost.multiply(BigDecimal.valueOf(1000)).divide(BigDecimal.valueOf(samples), 4, RoundingMode.HALF_UP);
        return new EvaluationResult(samples, classified, failed,
                ratio(eligOk, classified), ratio(catOk, catTotal), ratio(typeOk, typeTotal), cost, per1000,
                mismatches.size() > 50 ? mismatches.subList(0, 50) : mismatches);
    }

    private static double ratio(int ok, int total) { return total == 0 ? 0 : Math.round(10000.0 * ok / total) / 10000.0; }

    private static int col(List<String> header, String name) {
        int i = header.indexOf(name);
        if (i < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Labelled file is missing column '" + name + "'.");
        return i;
    }

    private static String get(List<String> row, int i) { return i < row.size() ? row.get(i) : ""; }
}
