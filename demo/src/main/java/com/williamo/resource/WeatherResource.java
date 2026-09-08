package com.williamo.resource;

import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.williamo.dto.HistoryPageResponse;
import com.williamo.dto.WeatherQueryResponse;
import com.williamo.service.WeatherService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/weather")
public class WeatherResource {

    private final WeatherService weatherService;

    public WeatherResource(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherQueryResponse getWeather(
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) {
        return weatherService.getWeather(city, latitude, longitude);
    }

    @GetMapping("/history")
    public HistoryPageResponse listHistory(
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return weatherService.listHistory(city, country, startDate, endDate, page, size);
    }

    @GetMapping("/history/{id}")
    public WeatherQueryResponse getHistory(@PathVariable Long id) {
        return weatherService.getHistory(id);
    }

    @DeleteMapping("/history/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory(@PathVariable Long id) {
        weatherService.deleteHistory(id);
    }
}
