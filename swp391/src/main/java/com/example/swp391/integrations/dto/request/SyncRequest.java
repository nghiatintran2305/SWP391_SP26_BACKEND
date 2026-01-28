package com.example.swp391.integrations.dto.request;

import com.example.swp391.integrations.enums.SyncJobType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SyncRequest {

    @NotNull
    private SyncJobType type;
}
