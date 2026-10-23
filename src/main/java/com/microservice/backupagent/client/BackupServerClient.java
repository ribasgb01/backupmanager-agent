package com.microservice.backupagent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.backupagent.config.AgentConfig;
import com.microservice.backupagent.dto.BackupUploadResponse;
import com.microservice.backupagent.dto.CompleteBackupRequest;
import com.microservice.backupagent.dto.CreateUploadRequest;
import com.microservice.backupagent.orchestrator.BackupOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

public class BackupServerClient {

    private static final Logger log = LoggerFactory.getLogger(BackupServerClient.class);

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
            log.info("Solicitando autorização de upload para a API Central (Arquivo: {}, Tamanho: {} bytes)...", filename, fileSizeBytes);

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
                BackupUploadResponse uploadResponse = objectMapper.readValue(response.body(), BackupUploadResponse.class);
                log.info("Autorização concedida com sucesso pela API Central. Backup ID gerado: {}", uploadResponse.backupId());
                return uploadResponse;
            }
            log.error("API Central recusou o pedido de upload. Status HTTP: {} - Detalhe: {}", response.statusCode(), response.body());
            throw new IllegalStateException("Servidor recusou a solicitação de upload. Status: " + response.statusCode() + " - Detalhe: " + response.body());

        } catch (IOException | InterruptedException e){
            log.error("Falha ao processar a requisição de upload", e);
            throw new IllegalStateException("Falha ao processar a requisição de upload.", e);
        }
    }

    public void uploadToStorage(String presignedUrl, Path filePath){

        try{
            log.info("Iniciando envio direto do arquivo para o Storage S3/Silo: {}", filePath.getFileName());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(presignedUrl))
                    .timeout(Duration.ofMinutes(30))
                    .PUT(HttpRequest.BodyPublishers.ofFile(filePath))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200){
                log.error("Storage S3 rejeitou o arquivo. Status HTTP: {} - Detalhe: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Falha ao enviar arquivo para o servidor. Status: " + response.statusCode() + " - Detalhe: " + response.body());
            }

            log.info("Upload para o Storage S3 concluído com sucesso!");
        } catch (IOException | InterruptedException e){
            log.error("Não foi possível enviar o arquivo ao servidor.", e);
            throw new IllegalStateException("Não foi possível enviar o arquivo ao servidor.", e);
        }
    }

    public void completeBackup(UUID backupId, String status, String hashMd5, String errorMessage){

        try{
            log.info("Notificando API Central sobre o status final do backup: [{}]...", status);
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
                log.error("Falha ao registrar conclusão na API Central. Status HTTP: {} - Detalhe: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Falha ao completar o backup. Status: " + response.statusCode() + " - Detalhe: " + response.body());
            }

            log.info("Status de conclusão registrado com sucesso na API Central.");

        } catch (IOException | InterruptedException e){
            log.error("Não foi possível completar o backup.", e);

            throw new IllegalStateException("Não foi possível completar o backup.", e);
        }
    }

}
