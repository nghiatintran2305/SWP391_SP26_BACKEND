package com.example.swp391.accounts.service;

import com.example.swp391.accounts.dto.response.LecturerResponse;

import java.util.List;

public interface IAccountService {
    List<LecturerResponse> getLecturers(String keyword);
}
