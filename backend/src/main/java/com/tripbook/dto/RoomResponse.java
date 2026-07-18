package com.tripbook.dto;

import com.tripbook.entity.HotelRoom;

public record RoomResponse(Long id, String roomNumber, String roomType, String status) {

    public static RoomResponse from(HotelRoom room) {
        return new RoomResponse(room.getId(), room.getRoomNumber(), room.getRoomType(), room.getStatus());
    }
}
