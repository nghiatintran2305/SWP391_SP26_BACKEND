package com.example.swp391.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GithubPushEvent {
    
    @JsonProperty("ref")
    private String ref;
    
    @JsonProperty("before")
    private String before;
    
    @JsonProperty("after")
    private String after;
    
    @JsonProperty("repository")
    private Repository repository;
    
    @JsonProperty("pusher")
    private Pusher pusher;
    
    @JsonProperty("commits")
    private List<Commit> commits;
    
    @Data
    public static class Repository {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("full_name")
        private String fullName;
    }
    
    @Data
    public static class Pusher {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("email")
        private String email;
    }
    
    @Data
    public static class Commit {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("timestamp")
        private String timestamp;
        
        @JsonProperty("url")
        private String url;
        
        @JsonProperty("author")
        private Author author;
        
        @JsonProperty("added")
        private List<String> added;
        
        @JsonProperty("removed")
        private List<String> removed;
        
        @JsonProperty("modified")
        private List<String> modified;
    }
    
    @Data
    public static class Author {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("username")
        private String username;
    }
}
