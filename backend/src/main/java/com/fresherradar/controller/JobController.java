package com.fresherradar.controller;

import com.fresherradar.dto.JobDto;
import com.fresherradar.dto.JobListResponse;
import com.fresherradar.dto.JobSearchCriteria;
import com.fresherradar.dto.MetaResponse;
import com.fresherradar.service.JobService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @GetMapping("/jobs")
    public JobListResponse list(
            @RequestParam(required = false) @Size(max = 100, message = "search must be at most 100 characters") String search,
            @RequestParam(required = false) @Size(max = 120, message = "location is too long") String location,
            @RequestParam(required = false) @Size(max = 80, message = "category is too long") String category,
            @RequestParam(required = false) @Size(max = 30, message = "employmentType is too long") String employmentType,
            @RequestParam(required = false) @Size(max = 20, message = "experience is too long") String experience,
            @RequestParam(required = false) @Size(max = 200, message = "company is too long") String company,
            @RequestParam(required = false) @Min(value = 1, message = "postedWithinHours must be >= 1")
            @Max(value = 8760, message = "postedWithinHours must be <= 8760") Integer postedWithinHours,
            @RequestParam(defaultValue = "newest") @Pattern(regexp = "newest|oldest", message = "sort must be 'newest' or 'oldest'") String sort,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") int page,
            @RequestParam(defaultValue = "50") @Min(value = 1, message = "size must be >= 1")
            @Max(value = 1000, message = "size must be <= 1000") int size) {
        return service.search(new JobSearchCriteria(search, location, category, employmentType, experience,
                company, postedWithinHours, sort), page, size);
    }

    @GetMapping("/jobs/{id}")
    public JobDto get(@PathVariable Long id) {
        return service.get(id);
    }

    /** Values for the filter dropdowns. */
    @GetMapping("/meta")
    public MetaResponse meta() {
        return service.meta();
    }
}
