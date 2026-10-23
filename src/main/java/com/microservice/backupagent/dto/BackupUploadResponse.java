package com.microservice.backupagent.dto;

import java.util.UUID;

public record BackupUploadResponse(
        String uploadUrl,
        UUID backupId
){}
