package com.tripbook.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tripbook.exception.BadRequestException;
import com.tripbook.repository.HotelRepository;
import com.tripbook.repository.HotelRoomRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** See FlightServiceValidationTest for why search() itself isn't Mockito-tested beyond validation. */
@ExtendWith(MockitoExtension.class)
class HotelServiceValidationTest {

    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private HotelRoomRepository hotelRoomRepository;

    private HotelService hotelService() {
        return new HotelService(hotelRepository, hotelRoomRepository);
    }

    @Test
    void checkInInThePastIsRejected() {
        assertThatThrownBy(() -> hotelService().search(
                "Bali", LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), 2, null, 0, 20))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void checkOutNotAfterCheckInIsRejected() {
        LocalDate date = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> hotelService().search("Bali", date, date, 2, null, 0, 20))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void zeroOrFewerGuestsAreRejected() {
        assertThatThrownBy(() -> hotelService().search(
                "Bali", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 0, null, 0, 20))
                .isInstanceOf(BadRequestException.class);
    }
}
