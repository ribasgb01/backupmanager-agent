package com.microservice.backupagent.hasher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashCalculator {

    private static final Logger log = LoggerFactory.getLogger(HashCalculator.class);

    public String hash(Path filePath){

        MessageDigest md;

        try (InputStream inputStream = Files.newInputStream(filePath)){
            md = MessageDigest.getInstance("MD5");
            log.info("Calculando hash MD5 do arquivo: {}", filePath);

            byte[] buffer = new byte[8192];
            int readBytes;

            while((readBytes = inputStream.read(buffer)) != -1){
                md.update(buffer, 0, readBytes);
            }

        } catch (NoSuchAlgorithmException e){
            log.error("Erro ao declarar algorítmo para hash.", e);
            throw new IllegalStateException("Erro ao declarar algorítmo para hash.", e);

        } catch (IOException e){
            log.error("Erro ao calcular Hash MD5.", e);
            throw new IllegalStateException("Erro ao calcular Hash MD5.", e);
        }

        byte[] hashByte = md.digest();
        String hashMd5 = HexFormat.of().formatHex(hashByte);

        log.info("Hash MD5 calculado com sucesso: {}", hashMd5);

        return hashMd5;
    }
}
