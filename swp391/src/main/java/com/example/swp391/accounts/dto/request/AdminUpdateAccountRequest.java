package com.example.swp391.accounts.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateAccountRequest {

    @Email(message = "Email không hợp lệ")
    private String email;

    private String fullName;
    private String phone;
    private String address;
    private Boolean isActive;
}
