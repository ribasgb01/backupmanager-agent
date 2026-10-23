package com.microservice.backupagent.dumper;

import com.microservice.backupagent.config.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FirebirdDumper {

    private final AgentConfig config;
    private static final Logger log = LoggerFactory.getLogger(FirebirdDumper.class);

    public FirebirdDumper(AgentConfig config){
        this.config = config;
    }

    public Path dump(){

        try {
            Files.createDirectories(Path.of(config.getTempDir()));

            String sourceDb = config.getDbHost() + "/" + config.getDbPort() + ":" + config.getDbPath();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "backup" + "_" + timestamp + ".fbk";
            Path destinationPath = Path.of(config.getTempDir(), fileName);

            ProcessBuilder pb = new ProcessBuilder(
                    "gbak",
                    "-b",
                    "-user", config.getDbUser(),
                    "-password", config.getDbPassword(),
                    sourceDb,
                    destinationPath.toString()
            )
                    .redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder terminalOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null){
                    terminalOutput.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();

            if(exitCode == 0){
                log.info("Dump concluído com sucesso. Arquivo gerado em {}", destinationPath);
                return destinationPath;
            } else {
                log.error("O utilitário gbak falhou. Detalhes da saída: \n{}", terminalOutput);
                throw new IllegalStateException("O utilitário gbak falhou ao gerar o backup local.");
            }


        } catch (IOException e){
            log.error("Erro ao executar o gbak.", e);
            throw new IllegalStateException("Erro ao executar o gbak.", e);

        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            log.error("Execução do gbak foi interrompida.", e);
            throw new IllegalStateException("Execução do gbak foi interrompida.", e);
        }
    }
}
