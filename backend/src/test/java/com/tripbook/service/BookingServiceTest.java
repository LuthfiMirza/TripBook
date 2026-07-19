package com.tripbook.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.tripbook.dto.BookingResponse;
import com.tripbook.dto.FlightBookingRequest;
import com.tripbook.dto.HotelBookingRequest;
import com.tripbook.entity.Booking;
import com.tripbook.entity.Flight;
import com.tripbook.entity.FlightSeat;
import com.tripbook.entity.Hotel;
import com.tripbook.entity.HotelRoom;
import com.tripbook.entity.User;
import com.tripbook.event.BookingConfirmedEvent;
import com.tripbook.exception.BadRequestException;
import com.tripbook.exception.NotFoundException;
import com.tripbook.exception.SeatUnavailableException;
import com.tripbook.repository.BookingRepository;
import com.tripbook.repository.FlightSeatRepository;
import com.tripbook.repository.HotelRoomRepository;
import com.tripbook.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the booking/cancellation logic that would actually be probed in an
 * interview: seat locking, conflict handling, ownership checks, and the
 * after-success-only event publish. Does not assert mocks were called for
 * their own sake — every verify() here backs a real behavioral claim (e.g.
 * "the locking method is used, not plain findById" exists specifically to
 * catch a future refactor silently removing the lock).
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private FlightSeatRepository flightSeatRepository;
    @Mock
    private HotelRoomRepository hotelRoomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BookingService bookingService;

    private User user;
    private Flight flight;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, flightSeatRepository, hotelRoomRepository, userRepository, eventPublisher);

        user = User.builder().id(1L).email("test@tripbook.com").fullName("Test User").role("USER").build();
        flight = Flight.builder()
                .id(10L).flightCode("GA400").airline("Garuda Indonesia")
                .origin("CGK").destination("DPS").price(new BigDecimal("1850000.00")).totalSeats(72)
                .build();
    }

    // Scoped to only the tests that reach a real save — stubbing these in
    // @BeforeEach for every test trips Mockito's strict-stubbing check on the
    // failure-path tests that throw before ever reaching them.
    private void stubCurrentUser() {
        when(userRepository.findByEmail("test@tripbook.com")).thenReturn(Optional.of(user));
    }

    private void stubSuccessfulSave() {
        when(bookingRepository.existsByBookingReference(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });
    }

    private FlightSeat availableSeat(String seatNumber) {
        return FlightSeat.builder().id(5L).flight(flight).seatNumber(seatNumber).seatClass("ECONOMY").status("AVAILABLE").build();
    }

    @Test
    void bookingAnAvailableSeatSucceedsAndReturnsAReference() {
        stubCurrentUser();
        stubSuccessfulSave();
        when(flightSeatRepository.findByFlightIdAndSeatNumberForUpdate(10L, "12F"))
                .thenReturn(Optional.of(availableSeat("12F")));

        BookingResponse response = bookingService.bookFlight(
                "test@tripbook.com", new FlightBookingRequest(10L, "12F", "Passenger Name"));

        assertThat(response.bookingReference()).isNotBlank().startsWith("TB-");
        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.flight().seatNumber()).isEqualTo("12F");
    }

    @Test
    void bookingABookedSeatThrowsSeatUnavailableException() {
        stubCurrentUser();
        FlightSeat bookedSeat = availableSeat("12F");
        bookedSeat.setStatus("BOOKED");
        when(flightSeatRepository.findByFlightIdAndSeatNumberForUpdate(10L, "12F"))
                .thenReturn(Optional.of(bookedSeat));

        assertThatThrownBy(() -> bookingService.bookFlight(
                "test@tripbook.com", new FlightBookingRequest(10L, "12F", "Passenger Name")))
                .isInstanceOf(SeatUnavailableException.class);

        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void bookingANonexistentSeatThrowsNotFoundException() {
        stubCurrentUser();
        when(flightSeatRepository.findByFlightIdAndSeatNumberForUpdate(10L, "99Z")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.bookFlight(
                "test@tripbook.com", new FlightBookingRequest(10L, "99Z", "Passenger Name")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void theLockingRepositoryMethodIsUsedNotPlainFindById() {
        stubCurrentUser();
        stubSuccessfulSave();
        when(flightSeatRepository.findByFlightIdAndSeatNumberForUpdate(10L, "12F"))
                .thenReturn(Optional.of(availableSeat("12F")));

        bookingService.bookFlight("test@tripbook.com", new FlightBookingRequest(10L, "12F", "Passenger Name"));

        verify(flightSeatRepository, times(1)).findByFlightIdAndSeatNumberForUpdate(10L, "12F");
        verify(flightSeatRepository, never()).findById(anyLong());
    }

    @Test
    void theEventIsPublishedOnlyOnSuccessNeverOnFailure() {
        stubCurrentUser();
        stubSuccessfulSave();
        when(flightSeatRepository.findByFlightIdAndSeatNumberForUpdate(10L, "12F"))
                .thenReturn(Optional.of(availableSeat("12F")));

        bookingService.bookFlight("test@tripbook.com", new FlightBookingRequest(10L, "12F", "Passenger Name"));

        ArgumentCaptor<BookingConfirmedEvent> captor = ArgumentCaptor.forClass(BookingConfirmedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().bookingReference()).startsWith("TB-");
    }

    @Test
    void cancellingReleasesTheSeatBackToAvailable() {
        FlightSeat seat = availableSeat("12F");
        seat.setStatus("BOOKED");
        Booking booking = Booking.builder()
                .id(100L).bookingReference("TB-ABC12345").user(user).bookingType("FLIGHT")
                .flightSeat(seat).totalPrice(flight.getPrice()).status("CONFIRMED").build();

        when(bookingRepository.findByBookingReferenceAndUser_Email("TB-ABC12345", "test@tripbook.com"))
                .thenReturn(Optional.of(booking));
        when(flightSeatRepository.findByFlightIdAndSeatNumberForUpdate(10L, "12F"))
                .thenReturn(Optional.of(seat));

        BookingResponse response = bookingService.cancel("test@tripbook.com", "TB-ABC12345");

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(seat.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void cancellingSomeoneElsesBookingIsRejected() {
        when(bookingRepository.findByBookingReferenceAndUser_Email("TB-ABC12345", "test@tripbook.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel("test@tripbook.com", "TB-ABC12345"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancellingAnAlreadyCancelledBookingIsIdempotentNotAnError() {
        // NOTE: the plan's audit checklist says "cancelling an already-cancelled
        // booking is rejected", but the shipped behavior is an idempotent no-op
        // (returns the booking as-is) rather than throwing. That's arguably the
        // friendlier choice for a double-click/retry, but it is a real
        // discrepancy from the plan's literal wording — flagging here rather
        // than silently asserting whichever behavior happened to ship.
        Booking cancelled = Booking.builder()
                .id(100L).bookingReference("TB-ABC12345").user(user).bookingType("FLIGHT")
                .totalPrice(flight.getPrice()).status("CANCELLED").build();
        when(bookingRepository.findByBookingReferenceAndUser_Email("TB-ABC12345", "test@tripbook.com"))
                .thenReturn(Optional.of(cancelled));

        BookingResponse response = bookingService.cancel("test@tripbook.com", "TB-ABC12345");

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(flightSeatRepository, never()).findByFlightIdAndSeatNumberForUpdate(anyLong(), anyString());
    }

    @Test
    void bookingAHotelRoomComputesTotalPriceFromNights() {
        stubCurrentUser();
        stubSuccessfulSave();
        Hotel hotel = Hotel.builder().id(20L).name("Grand Inna Kuta").city("Bali")
                .pricePerNight(new BigDecimal("1200000.00")).build();
        HotelRoom room = HotelRoom.builder().id(6L).hotel(hotel).roomNumber("101").roomType("Standard").status("AVAILABLE").build();
        when(hotelRoomRepository.findByHotelIdAndRoomNumberForUpdate(20L, "101")).thenReturn(Optional.of(room));

        BookingResponse response = bookingService.bookHotel("test@tripbook.com", new HotelBookingRequest(
                20L, "101", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4), "Guest Name"));

        // 3 nights x 1,200,000
        assertThat(response.totalPrice()).isEqualByComparingTo("3600000.00");
    }

    @Test
    void bookingAHotelWithCheckOutNotAfterCheckInIsRejected() {
        assertThatThrownBy(() -> bookingService.bookHotel("test@tripbook.com", new HotelBookingRequest(
                20L, "101", LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 4), "Guest Name")))
                .isInstanceOf(BadRequestException.class);

        verify(hotelRoomRepository, never()).findByHotelIdAndRoomNumberForUpdate(anyLong(), anyString());
    }
}
