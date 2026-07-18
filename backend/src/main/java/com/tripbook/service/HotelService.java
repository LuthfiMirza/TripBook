package com.tripbook.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tripbook.dto.HotelSearchResponse;
import com.tripbook.dto.PagedResponse;
import com.tripbook.exception.BadRequestException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class HotelService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * The schema (Phase 1) tracks room status as a single AVAILABLE/BOOKED
     * flag, not a per-date calendar — modeling per-night inventory would need
     * a separate hotel_room_availability table keyed by date, which is out of
     * scope for this project's timeline (the concurrency/locking mechanics
     * this project demonstrates don't need it). checkIn/checkOut are therefore
     * validated (not in the past, checkOut after checkIn) but do not filter
     * results — availableRooms reflects current status, not date-range
     * availability. Same simplification as flights' seat availability.
     */
    @SuppressWarnings("unchecked")
    public PagedResponse<HotelSearchResponse> search(
            String city, LocalDate checkIn, LocalDate checkOut, int guests,
            String sort, int page, int size) {

        if (checkIn.isBefore(LocalDate.now())) {
            throw new BadRequestException("checkIn must not be in the past");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("checkOut must be after checkIn");
        }
        if (guests < 1) {
            throw new BadRequestException("guests must be at least 1");
        }

        String orderBy = "price_desc".equals(sort) ? "h.price_per_night DESC" : "h.price_per_night ASC";

        String dataSql = """
                SELECT h.id, h.name, h.city, h.address, h.price_per_night, h.star_rating,
                       COUNT(hr.id) FILTER (WHERE hr.status = 'AVAILABLE') AS available_rooms
                FROM hotels h
                JOIN hotel_rooms hr ON hr.hotel_id = h.id
                WHERE h.city = :city
                GROUP BY h.id
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(orderBy);

        Query dataQuery = entityManager.createNativeQuery(dataSql)
                .setParameter("city", city)
                .setParameter("limit", size)
                .setParameter("offset", page * size);

        List<Object[]> rows = dataQuery.getResultList();
        List<HotelSearchResponse> content = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            content.add(new HotelSearchResponse(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (BigDecimal) row[4],
                    row[5] == null ? null : ((Number) row[5]).intValue(),
                    ((Number) row[6]).longValue()));
        }

        long total = ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM hotels WHERE city = :city")
                .setParameter("city", city)
                .getSingleResult()).longValue();

        return new PagedResponse<>(content, page, size, total);
    }
}
