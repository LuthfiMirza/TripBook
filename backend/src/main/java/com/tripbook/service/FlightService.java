package com.tripbook.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripbook.dto.FlightDetailResponse;
import com.tripbook.dto.FlightRequest;
import com.tripbook.dto.FlightSearchResponse;
import com.tripbook.dto.PagedResponse;
import com.tripbook.dto.SeatResponse;
import com.tripbook.entity.Flight;
import com.tripbook.entity.FlightSeat;
import com.tripbook.exception.BadRequestException;
import com.tripbook.exception.NotFoundException;
import com.tripbook.repository.FlightRepository;
import com.tripbook.repository.FlightSeatRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightSeatRepository flightSeatRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public FlightService(FlightRepository flightRepository, FlightSeatRepository flightSeatRepository) {
        this.flightRepository = flightRepository;
        this.flightSeatRepository = flightSeatRepository;
    }

    // 2 queries total for a detail call: findById, then the seats lookup below.
    // Fixed regardless of how many seats a flight has, so this isn't N+1 —
    // N+1 would be one seats query per flight in a *list*, not per request.
    @Cacheable(cacheNames = "flightDetail", key = "#id")
    public FlightDetailResponse getDetail(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flight not found: " + id));

        List<SeatResponse> seats = flightSeatRepository.findSeatMapByFlightId(id).stream()
                .map(SeatResponse::from)
                .toList();

        return new FlightDetailResponse(
                flight.getId(), flight.getFlightCode(), flight.getAirline(),
                flight.getOrigin(), flight.getDestination(),
                flight.getDepartureTime(), flight.getArrivalTime(),
                flight.getPrice(), flight.getTotalSeats(), seats);
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
    @Cacheable(cacheNames = "flightSearch", key = "#origin + ':' + #destination + ':' + #date + ':' + #passengers + ':' + (#sort ?: 'default') + ':' + #page + ':' + #size")
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

    private static final String[] SEAT_LETTERS = { "A", "B", "C", "D", "E", "F" };
    private static final int SEATS_PER_ROW = SEAT_LETTERS.length;
    private static final int BUSINESS_ROWS = 2;

    @Transactional
    @CacheEvict(cacheNames = { "flightSearch", "flightDetail" }, allEntries = true)
    public FlightDetailResponse createFlight(FlightRequest request) {
        Flight flight = Flight.builder()
                .flightCode(request.flightCode())
                .airline(request.airline())
                .origin(request.origin())
                .destination(request.destination())
                .departureTime(request.departureTime())
                .arrivalTime(request.arrivalTime())
                .price(request.price())
                .totalSeats(request.totalSeats())
                .build();
        flight = flightRepository.save(flight);

        generateSeats(flight);

        return getDetail(flight.getId());
    }

    // Rows of 6 (A-F), first 2 rows BUSINESS, rest ECONOMY, per the plan's spec.
    // A remainder under 6 becomes one partial final row so the generated seat
    // count always matches totalSeats exactly, even when it isn't a multiple of 6.
    private void generateSeats(Flight flight) {
        int totalSeats = flight.getTotalSeats();
        int fullRows = totalSeats / SEATS_PER_ROW;
        int remainder = totalSeats % SEATS_PER_ROW;

        List<FlightSeat> seats = new ArrayList<>(totalSeats);
        int row = 1;
        for (; row <= fullRows; row++) {
            String seatClass = row <= BUSINESS_ROWS ? "BUSINESS" : "ECONOMY";
            for (String letter : SEAT_LETTERS) {
                seats.add(FlightSeat.builder()
                        .flight(flight)
                        .seatNumber(row + letter)
                        .seatClass(seatClass)
                        .status("AVAILABLE")
                        .build());
            }
        }
        if (remainder > 0) {
            String seatClass = row <= BUSINESS_ROWS ? "BUSINESS" : "ECONOMY";
            for (int i = 0; i < remainder; i++) {
                seats.add(FlightSeat.builder()
                        .flight(flight)
                        .seatNumber(row + SEAT_LETTERS[i])
                        .seatClass(seatClass)
                        .status("AVAILABLE")
                        .build());
            }
        }
        flightSeatRepository.saveAll(seats);
    }

    // Updates the flight's own fields only — it deliberately does not touch
    // existing flight_seats rows. Changing totalSeats after seats (and
    // potentially bookings, from Phase 4 on) already exist is a data-migration
    // decision, not something a blind PUT should do implicitly.
    @Transactional
    @CacheEvict(cacheNames = { "flightSearch", "flightDetail" }, allEntries = true)
    public FlightDetailResponse updateFlight(Long id, FlightRequest request) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flight not found: " + id));

        flight.setFlightCode(request.flightCode());
        flight.setAirline(request.airline());
        flight.setOrigin(request.origin());
        flight.setDestination(request.destination());
        flight.setDepartureTime(request.departureTime());
        flight.setArrivalTime(request.arrivalTime());
        flight.setPrice(request.price());
        flight.setTotalSeats(request.totalSeats());
        flightRepository.save(flight);

        return getDetail(id);
    }

    @Transactional
    @CacheEvict(cacheNames = { "flightSearch", "flightDetail" }, allEntries = true)
    public void deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flight not found: " + id));
        flightSeatRepository.deleteAll(flightSeatRepository.findSeatMapByFlightId(id));
        flightRepository.delete(flight);
    }
}
