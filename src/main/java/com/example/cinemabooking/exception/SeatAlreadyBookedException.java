package com.example.cinemabooking.exception;

public class SeatAlreadyBookedException extends RuntimeException {
    public SeatAlreadyBookedException(int seatNumber) {
        super("صندلی شماره " + seatNumber + " قبلاً فروخته شده است");
    }
}
