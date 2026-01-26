package com.example.swp391.accounts.dto.request;

import com.example.swp391.accounts.enums.LoginType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String username;
    private String password;
    private LoginType loginType;
}



