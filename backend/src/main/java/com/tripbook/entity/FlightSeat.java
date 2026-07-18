package com.tripbook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * `version` is a plain column, not a JPA @Version field — Phase 4 locks seats
 * with PESSIMISTIC_WRITE (SELECT ... FOR UPDATE), not optimistic locking, so an
 * automatic Hibernate version check would just be redundant machinery here.
 */
@Entity
@Table(name = "flight_seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(name = "seat_class", nullable = false)
    private String seatClass;

    @Column(nullable = false)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
