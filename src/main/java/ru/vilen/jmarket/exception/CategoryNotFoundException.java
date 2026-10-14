package ru.vilen.jmarket.exception;

public class CategoryNotFoundException extends RuntimeException{

    public CategoryNotFoundException(Long categoryId) {
        super("Category with ID " + categoryId + " not found");
    }
}
