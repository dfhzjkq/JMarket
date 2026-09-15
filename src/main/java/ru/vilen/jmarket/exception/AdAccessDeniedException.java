package ru.vilen.jmarket.exception;

public class AdAccessDeniedException extends RuntimeException{

    public AdAccessDeniedException(String message) {
        super(message);
    }
}
