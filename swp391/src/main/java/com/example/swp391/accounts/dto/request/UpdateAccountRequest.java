package com.example.swp391.accounts.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    @Email(message = "Invalid email format")
    @Pattern(regexp = "^$|^.+@.+$", message = "Email must contain @")
    private String email;

    @Pattern(
            regexp = "^$|^[\\p{L}\\s'.-]+$",
            message = "Full name must not contain digits or special symbols"
    )
    private String fullName;

    @Pattern(
            regexp = "^$|^\\d{10}$",
            message = "Phone number must contain exactly 10 digits"
    )
    private String phone;

    private String address;
    private LocalDate dateOfBirth;
}
