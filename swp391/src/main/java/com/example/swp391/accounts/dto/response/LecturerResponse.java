package com.example.swp391.accounts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LecturerResponse {
    private String id;
    private String username;
    private String email;
    private String fullName;
}
