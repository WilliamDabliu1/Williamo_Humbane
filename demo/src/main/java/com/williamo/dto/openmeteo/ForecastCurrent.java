package com.williamo.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastCurrent {

    private String time;
    private Integer interval;
    @JsonProperty("temperature_2m")
    private Double temperature2m;
    @JsonProperty("relative_humidity_2m")
    private Integer relativeHumidity2m;
    @JsonProperty("apparent_temperature")
    private Double apparentTemperature;
    @JsonProperty("weather_code")
    private Integer weatherCode;
    @JsonProperty("wind_speed_10m")
    private Double windSpeed10m;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Double getTemperature2m() {
        return temperature2m;
    }

    public void setTemperature2m(Double temperature2m) {
        this.temperature2m = temperature2m;
    }

    public Integer getRelativeHumidity2m() {
        return relativeHumidity2m;
    }

    public void setRelativeHumidity2m(Integer relativeHumidity2m) {
        this.relativeHumidity2m = relativeHumidity2m;
    }

    public Double getApparentTemperature() {
        return apparentTemperature;
    }

    public void setApparentTemperature(Double apparentTemperature) {
        this.apparentTemperature = apparentTemperature;
    }

    public Integer getWeatherCode() {
        return weatherCode;
    }

    public void setWeatherCode(Integer weatherCode) {
        this.weatherCode = weatherCode;
    }

    public Double getWindSpeed10m() {
        return windSpeed10m;
    }

    public void setWindSpeed10m(Double windSpeed10m) {
        this.windSpeed10m = windSpeed10m;
    }
}
