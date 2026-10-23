package com.microservice.backupagent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.backupagent.config.AgentConfig;
import com.microservice.backupagent.dto.BackupUploadResponse;
import com.microservice.backupagent.dto.CompleteBackupRequest;
import com.microservice.backupagent.dto.CreateUploadRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

public class BackupServerClient {

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BackupServerClient(AgentConfig config){

        this.config = config;
        httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        objectMapper = new ObjectMapper();
    }


    public BackupUploadResponse requestUpload(String filename, long fileSizeBytes){

        try {
            CreateUploadRequest requestBody = new CreateUploadRequest(filename, fileSizeBytes);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getServerUrl() + "/api/v1/backups/request-upload"))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", config.getApiKey())
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();


            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() == 200 || response.statusCode() == 201){
                return objectMapper.readValue(response.body(), BackupUploadResponse.class);
            }

            throw new IllegalStateException("Servidor recusou a solicitação de upload. Status: " + response.statusCode() + " - Detalhe: " + response.body());

        } catch (IOException | InterruptedException e){
            throw new IllegalStateException("Falha ao processar a requisição de upload.", e);
        }
    }

    public void uploadToStorage(String presignedUrl, Path filePath){

        try{
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(presignedUrl))
                    .timeout(Duration.ofMinutes(30))
                    .PUT(HttpRequest.BodyPublishers.ofFile(filePath))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200){
                throw new IllegalStateException("Falha ao enviar arquivo para o servidor. Status: " + response.statusCode() + " - Detalhe: " + response.body());
            }

        } catch (IOException | InterruptedException e){
            throw new IllegalStateException("Não foi possível enviar o arquivo ao servidor.", e);
        }
    }

    public void completeBackup(UUID backupId, String status, String hashMd5, String errorMessage){

        try{
            CompleteBackupRequest requestBody = new CompleteBackupRequest(status, hashMd5, errorMessage);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getServerUrl() + "/api/v1/backups/" + backupId + "/complete"))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", config.getApiKey())
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 204 && response.statusCode() != 200){
                throw new IllegalStateException("Falha ao completar o backup. Status: " + response.statusCode() + " - Detalhe: " + response.body());
            }

        } catch (IOException | InterruptedException e){
            throw new IllegalStateException("Não foi possível completar o backup.", e);
        }
    }

}
