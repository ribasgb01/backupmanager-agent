package com.microservice.backupagent.compressor;

import com.microservice.backupagent.dumper.FirebirdDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public class FileCompressor {

    private static final Logger log = LoggerFactory.getLogger(FileCompressor.class);

    public Path compress(Path backupFilePath){

        Path compressedPath = Path.of(backupFilePath + ".gz");

        try (InputStream input = Files.newInputStream(backupFilePath); GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(compressedPath))){
            log.info("Compactando arquivo de backup: {}", backupFilePath);
            input.transferTo(output);
        } catch (IOException e){
            log.error("Erro ao compactar banco de dados: {}", backupFilePath, e);
            throw new IllegalStateException("Erro ao compactar o banco de dados.", e);
        }

        log.info("Arquivo compactado com sucesso em: {}", compressedPath);
        return compressedPath;
    }
}
