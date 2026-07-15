package com.example.online_food_delivery.exception;

public class DublicateResourceFoundException extends RuntimeException{
    public DublicateResourceFoundException(String message){
        super(message);
    }
}
