package ru.otus.highload.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class User {

    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String gender;
    private String interests;
    private String city;
    private String username;
    private String password;
}
