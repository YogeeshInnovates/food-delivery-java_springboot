package com.example.online_food_delivery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Checkingworking {


    @GetMapping("/wellcome")
    public  String  check(){
        return "Hi wellcome to spring it is working";
    }
}
