package ru.vilen.jmarket.exception;

public class AdNotFoundException extends RuntimeException {

    public AdNotFoundException(Long adId) {
        super("Ad with ID " + adId + " not found");
    }
}
