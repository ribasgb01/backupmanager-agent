package com.microservice.backupagent.dto;

public record CreateUploadRequest(
        String fileName,
        Long fileSizeBytes
){}
