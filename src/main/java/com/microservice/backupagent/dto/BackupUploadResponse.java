package com.microservice.backupagent.dto;

import java.util.UUID;

public record BackupUploadRequest (
        String uploadUrl,
        UUID backupId
){}
