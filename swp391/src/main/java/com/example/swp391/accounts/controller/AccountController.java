package com.example.swp391.accounts.controller;

import com.example.swp391.accounts.dto.response.LecturerResponse;
import com.example.swp391.accounts.service.IAccountService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@AllArgsConstructor
public class AccountController {
    private final IAccountService accountService;

    @GetMapping("/lecturers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LecturerResponse>> getLecturers(
            @RequestParam(required = false) String search
    ) {
        List<LecturerResponse> lecturers =
                accountService.getLecturers(search);

        return ResponseEntity.ok(lecturers);
    }
}
