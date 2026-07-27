package com.manacommunity.auth.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycStatsResponse {
    private long total;
    private long pending;
    private long approved;
    private long rejected;
}
