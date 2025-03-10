package com.ib.it.bounce.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Alert {
    @JsonProperty("alert_type")
    private String alertType;
    @JsonProperty("alert_status")
    private String alertStatus;
    @JsonProperty("alert_details")
    private String alertDetails;
    @JsonProperty("alert_timestamp")
    @Builder.Default
    private String alertTimeStamp = getCurrentTimestamp();

    private static String getCurrentTimestamp() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }
}
