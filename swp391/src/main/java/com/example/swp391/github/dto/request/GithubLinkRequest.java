package com.example.swp391.github.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GithubLinkRequest {
    @NotBlank
    private String githubUsername;
}
