package com.tripbook.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tripbook.exception.BadRequestException;
import com.tripbook.repository.FlightRepository;
import com.tripbook.repository.FlightSeatRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FlightService.search() builds and executes native SQL via EntityManager
 * directly (see the class-level comment there for why). Mockito-mocking that
 * chain would only prove we called the right EntityManager methods, not that
 * sorting or availableSeats are actually correct against real data — that's
 * already verified against a real Postgres instance in the Phase 3 audit
 * (curl-tested: sort=price_asc returns ascending prices, availableSeats
 * matches actual flight_seats counts). What's worth a real unit test is the
 * validation that runs *before* any DB call — these tests only cover that.
 */
@ExtendWith(MockitoExtension.class)
class FlightServiceValidationTest {

    @Mock
    private FlightRepository flightRepository;
    @Mock
    private FlightSeatRepository flightSeatRepository;

    private FlightService flightService() {
        return new FlightService(flightRepository, flightSeatRepository);
    }

    @Test
    void pastDatesAreRejected() {
        assertThatThrownBy(() -> flightService().search(
                "CGK", "DPS", LocalDate.now().minusDays(1), 1, null, 0, 20))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void zeroOrFewerPassengersAreRejected() {
        assertThatThrownBy(() -> flightService().search(
                "CGK", "DPS", LocalDate.now().plusDays(1), 0, null, 0, 20))
                .isInstanceOf(BadRequestException.class);
    }
}
