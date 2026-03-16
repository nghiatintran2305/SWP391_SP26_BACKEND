package com.example.swp391.github.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GithubRepoResponse{
    private String name;
    @JsonProperty("html_url")
    private String html_url;
}
