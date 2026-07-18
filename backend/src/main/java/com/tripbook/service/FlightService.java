package com.tripbook.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tripbook.dto.FlightSearchResponse;
import com.tripbook.dto.PagedResponse;
import com.tripbook.exception.BadRequestException;
import com.tripbook.repository.FlightRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    /**
     * Native SQL, not JPQL: this query aggregates available seats per flight,
     * filters on the aggregate (HAVING), sorts, and paginates in one round
     * trip. JPQL constructor-expression projections don't support parameter-
     * driven ORDER BY against an aggregated column cleanly, and a derived-table
     * count query (needed for accurate pagination totals) isn't portable JPQL
     * either — native SQL is the honest choice here, not a workaround.
     *
     * Exactly 2 SQL statements execute per call: this data query and the count
     * query below. Neither runs once per row, so there is no N+1.
     */
    @SuppressWarnings("unchecked")
    public PagedResponse<FlightSearchResponse> search(
            String origin, String destination, LocalDate date, int passengers,
            String sort, int page, int size) {

        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("date must not be in the past");
        }
        if (passengers < 1) {
            throw new BadRequestException("passengers must be at least 1");
        }

        String orderBy = switch (sort == null ? "" : sort) {
            case "price_desc" -> "f.price DESC";
            case "departure_asc" -> "f.departure_time ASC";
            default -> "f.price ASC"; // "price_asc" and any unrecognized value
        };

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        String dataSql = """
                SELECT f.id, f.flight_code, f.airline, f.origin, f.destination,
                       f.departure_time, f.arrival_time, f.price,
                       COUNT(fs.id) FILTER (WHERE fs.status = 'AVAILABLE') AS available_seats
                FROM flights f
                JOIN flight_seats fs ON fs.flight_id = f.id
                WHERE f.origin = :origin AND f.destination = :destination
                  AND f.departure_time >= :dayStart AND f.departure_time < :dayEnd
                GROUP BY f.id
                HAVING COUNT(fs.id) FILTER (WHERE fs.status = 'AVAILABLE') >= :passengers
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(orderBy);

        Query dataQuery = entityManager.createNativeQuery(dataSql)
                .setParameter("origin", origin)
                .setParameter("destination", destination)
                .setParameter("dayStart", dayStart)
                .setParameter("dayEnd", dayEnd)
                .setParameter("passengers", passengers)
                .setParameter("limit", size)
                .setParameter("offset", page * size);

        List<Object[]> rows = dataQuery.getResultList();
        List<FlightSearchResponse> content = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            content.add(new FlightSearchResponse(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    ((Timestamp) row[5]).toLocalDateTime(),
                    ((Timestamp) row[6]).toLocalDateTime(),
                    (BigDecimal) row[7],
                    ((Number) row[8]).longValue()));
        }

        String countSql = """
                SELECT COUNT(*) FROM (
                    SELECT f.id
                    FROM flights f
                    JOIN flight_seats fs ON fs.flight_id = f.id
                    WHERE f.origin = :origin AND f.destination = :destination
                      AND f.departure_time >= :dayStart AND f.departure_time < :dayEnd
                    GROUP BY f.id
                    HAVING COUNT(fs.id) FILTER (WHERE fs.status = 'AVAILABLE') >= :passengers
                ) matching_flights
                """;

        long total = ((Number) entityManager.createNativeQuery(countSql)
                .setParameter("origin", origin)
                .setParameter("destination", destination)
                .setParameter("dayStart", dayStart)
                .setParameter("dayEnd", dayEnd)
                .setParameter("passengers", passengers)
                .getSingleResult()).longValue();

        return new PagedResponse<>(content, page, size, total);
    }
}
