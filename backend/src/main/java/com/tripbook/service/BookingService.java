package com.tripbook.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripbook.dto.BookingResponse;
import com.tripbook.dto.FlightBookingRequest;
import com.tripbook.dto.HotelBookingRequest;
import com.tripbook.dto.PagedResponse;
import com.tripbook.entity.Booking;
import com.tripbook.entity.FlightSeat;
import com.tripbook.entity.HotelRoom;
import com.tripbook.entity.User;
import com.tripbook.exception.BadRequestException;
import com.tripbook.exception.NotFoundException;
import com.tripbook.exception.SeatUnavailableException;
import com.tripbook.repository.BookingRepository;
import com.tripbook.repository.FlightSeatRepository;
import com.tripbook.repository.HotelRoomRepository;
import com.tripbook.repository.UserRepository;

@Service
public class BookingService {

    private static final String AVAILABLE = "AVAILABLE";
    private static final String BOOKED = "BOOKED";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String CANCELLED = "CANCELLED";
    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final BookingRepository bookingRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final HotelRoomRepository hotelRoomRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public BookingService(
            BookingRepository bookingRepository,
            FlightSeatRepository flightSeatRepository,
            HotelRoomRepository hotelRoomRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.flightSeatRepository = flightSeatRepository;
        this.hotelRoomRepository = hotelRoomRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = { "flightSearch", "flightDetail" }, allEntries = true)
    public BookingResponse bookFlight(String email, FlightBookingRequest request) {
        User user = currentUser(email);
        FlightSeat seat = flightSeatRepository
                .findByFlightIdAndSeatNumberForUpdate(request.flightId(), request.seatNumber())
                .orElseThrow(() -> new NotFoundException("Seat not found: " + request.seatNumber()));

        if (!AVAILABLE.equals(seat.getStatus())) {
            throw new SeatUnavailableException("Seat is not available: " + request.seatNumber());
        }

        seat.setStatus(BOOKED);
        Booking booking = Booking.builder()
                .bookingReference(generateUniqueReference())
                .user(user)
                .bookingType("FLIGHT")
                .flightSeat(seat)
                .totalPrice(seat.getFlight().getPrice())
                .status(CONFIRMED)
                .build();

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    @CacheEvict(cacheNames = { "hotelSearch", "hotelDetail" }, allEntries = true)
    public BookingResponse bookHotel(String email, HotelBookingRequest request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new BadRequestException("checkOut must be after checkIn");
        }

        User user = currentUser(email);
        HotelRoom room = hotelRoomRepository
                .findByHotelIdAndRoomNumberForUpdate(request.hotelId(), request.roomNumber())
                .orElseThrow(() -> new NotFoundException("Room not found: " + request.roomNumber()));

        if (!AVAILABLE.equals(room.getStatus())) {
            throw new SeatUnavailableException("Room is not available: " + request.roomNumber());
        }

        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        BigDecimal totalPrice = room.getHotel().getPricePerNight().multiply(BigDecimal.valueOf(nights));
        room.setStatus(BOOKED);

        Booking booking = Booking.builder()
                .bookingReference(generateUniqueReference())
                .user(user)
                .bookingType("HOTEL")
                .hotelRoom(room)
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .totalPrice(totalPrice)
                .status(CONFIRMED)
                .build();

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getMyBookings(String email, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        Pageable pageable = PageRequest.of(page, size);
        var bookings = bookingRepository.findByUser_EmailOrderByCreatedAtDesc(email, pageable);
        return new PagedResponse<>(bookings.getContent().stream().map(BookingResponse::from).toList(),
                page, size, bookings.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BookingResponse getMyBooking(String email, String reference) {
        return BookingResponse.from(findOwnedBooking(email, reference));
    }

    @Transactional
    @CacheEvict(cacheNames = { "flightSearch", "flightDetail", "hotelSearch", "hotelDetail" }, allEntries = true)
    public BookingResponse cancel(String email, String reference) {
        Booking booking = findOwnedBooking(email, reference);
        if (CANCELLED.equals(booking.getStatus())) {
            return BookingResponse.from(booking);
        }

        if ("FLIGHT".equals(booking.getBookingType())) {
            FlightSeat seat = booking.getFlightSeat();
            flightSeatRepository.findByFlightIdAndSeatNumberForUpdate(
                    seat.getFlight().getId(), seat.getSeatNumber())
                    .orElseThrow(() -> new NotFoundException("Seat not found: " + seat.getSeatNumber()))
                    .setStatus(AVAILABLE);
        } else if ("HOTEL".equals(booking.getBookingType())) {
            HotelRoom room = booking.getHotelRoom();
            hotelRoomRepository.findByHotelIdAndRoomNumberForUpdate(
                    room.getHotel().getId(), room.getRoomNumber())
                    .orElseThrow(() -> new NotFoundException("Room not found: " + room.getRoomNumber()))
                    .setStatus(AVAILABLE);
        }

        booking.setStatus(CANCELLED);
        return BookingResponse.from(booking);
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
    }

    private Booking findOwnedBooking(String email, String reference) {
        return bookingRepository.findByBookingReferenceAndUser_Email(reference, email)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + reference));
    }

    private String generateUniqueReference() {
        String reference;
        do {
            reference = "TB-" + randomReferenceBody();
        } while (bookingRepository.existsByBookingReference(reference));
        return reference;
    }

    private String randomReferenceBody() {
        StringBuilder value = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            value.append(REFERENCE_ALPHABET.charAt(random.nextInt(REFERENCE_ALPHABET.length())));
        }
        return value.toString();
    }
}
