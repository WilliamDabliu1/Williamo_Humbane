package com.williamo.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastResponse {

    private Double latitude;
    private Double longitude;
    private String timezone;
    private Double elevation;
    @JsonProperty("current_units")
    private ForecastCurrentUnits currentUnits;
    private ForecastCurrent current;
    private Boolean error;
    private String reason;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public ForecastCurrentUnits getCurrentUnits() {
        return currentUnits;
    }

    public void setCurrentUnits(ForecastCurrentUnits currentUnits) {
        this.currentUnits = currentUnits;
    }

    public ForecastCurrent getCurrent() {
        return current;
    }

    public void setCurrent(ForecastCurrent current) {
        this.current = current;
    }

    public Boolean getError() {
        return error;
    }

    public void setError(Boolean error) {
        this.error = error;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
