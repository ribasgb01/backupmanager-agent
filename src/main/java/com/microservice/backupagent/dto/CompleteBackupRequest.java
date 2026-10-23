package com.microservice.backupagent.dto;

import java.util.UUID;

public record CompleteBackupRequest(
        String status,
        String fileHashMd5,
        String errorMessage
){}
