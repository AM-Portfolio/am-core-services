package com.am.analysis.adapter.model.components;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lifecycle {
    private LocalDateTime startDate; // Acquisition Date
    private LocalDateTime endDate;   // Disposal Date
}
