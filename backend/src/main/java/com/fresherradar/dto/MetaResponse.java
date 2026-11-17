package com.fresherradar.dto;

import java.util.List;

public record MetaResponse(List<String> locations, List<String> categories,
                           List<String> employmentTypes, List<String> experienceLevels) {
}
