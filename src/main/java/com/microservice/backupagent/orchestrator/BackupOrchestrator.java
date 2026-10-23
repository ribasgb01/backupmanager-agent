package com.microservice.backupagent.orchestrator;

import com.microservice.backupagent.client.BackupServerClient;
import com.microservice.backupagent.compressor.FileCompressor;
import com.microservice.backupagent.dto.BackupUploadResponse;
import com.microservice.backupagent.dumper.FirebirdDumper;
import com.microservice.backupagent.hasher.HashCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class BackupOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BackupOrchestrator.class);
    private final FirebirdDumper dumper;
    private final FileCompressor compressor;
    private final HashCalculator hasher;
    private final BackupServerClient client;

    public BackupOrchestrator(
            FirebirdDumper dumper,
            FileCompressor compressor,
            HashCalculator hasher,
            BackupServerClient client
    ){

        this.dumper = dumper;
        this.compressor = compressor;
        this.hasher = hasher;
        this.client = client;
    }

    public void run(){

        UUID backupId = null;
        Path dumpFile = null;
        Path compressedFile = null;

        try{
            dumpFile = dumper.dump();
            compressedFile = compressor.compress(dumpFile);
            String hashMd5 = hasher.hash(compressedFile);

            long fileSizeBytes = Files.size(compressedFile);
            String fileName = compressedFile.getFileName().toString();

            BackupUploadResponse uploadResponse = client.requestUpload(fileName, fileSizeBytes);
            backupId = uploadResponse.backupId();

            client.uploadToStorage(uploadResponse.uploadUrl(), compressedFile);
            client.completeBackup(backupId, "SUCCESS",hashMd5, null);
            log.info("Processo de backup concluído com sucesso!");

        } catch (Exception e){
            log.error("Falha crítica durante a execução do backup", e);
            if(backupId != null){
                try{
                    client.completeBackup(backupId, "FAILED", null, e.getMessage());
                } catch (Exception ex) {
                    log.warn("Não foi possível notificar a API Central sobre a falha.: {}", ex.getMessage());
                }
            }
            throw new RuntimeException("O processo de backup falhou.", e);
        } finally {
            safeDelete(dumpFile);
            safeDelete(compressedFile);
        }
    }

    private void safeDelete(Path file){
        if(file != null){
            try{
                Files.deleteIfExists(file);
                log.warn("Arquivo temporário removido com sucesso: {}", file);
            } catch (IOException e){
                log.warn("Falha ao remover arquivo temporário: {}", file, e);
            }
        }
    }
}
