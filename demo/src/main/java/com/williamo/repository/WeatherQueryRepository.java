package com.williamo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.williamo.entity.WeatherQuery;

import java.time.LocalDateTime;

public interface WeatherQueryRepository extends JpaRepository<WeatherQuery, Long> {

    @Query("SELECT w FROM WeatherQuery w WHERE "
            + "(:city IS NULL OR LOWER(w.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND "
            + "(:country IS NULL OR LOWER(w.country) LIKE LOWER(CONCAT('%', :country, '%'))) AND "
            + "(:startDate IS NULL OR w.consultedAt >= :startDate) AND "
            + "(:endDate IS NULL OR w.consultedAt < :endDate) "
            + "ORDER BY w.consultedAt DESC")
    Page<WeatherQuery> searchHistory(
            @Param("city") String city,
            @Param("country") String country,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
