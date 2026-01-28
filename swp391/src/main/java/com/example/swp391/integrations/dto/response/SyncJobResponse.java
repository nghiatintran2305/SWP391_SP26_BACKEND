package com.example.swp391.integrations.dto.response;

import com.example.swp391.integrations.enums.SyncJobStatus;
import com.example.swp391.integrations.enums.SyncJobType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SyncJobResponse {

    private String id;
    private String groupId;
    private SyncJobType type;
    private SyncJobStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String message;
}
