package com.decoaromas.decoaromaspos.dto.other.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailabilityResponse {
    private boolean isAvailable;
    private String message;
}
