package com.microservice.backupagent;

import com.microservice.backupagent.client.BackupServerClient;
import com.microservice.backupagent.compressor.FileCompressor;
import com.microservice.backupagent.config.AgentConfig;
import com.microservice.backupagent.dumper.FirebirdDumper;
import com.microservice.backupagent.hasher.HashCalculator;
import com.microservice.backupagent.orchestrator.BackupOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class App {

    static {
        try {
            File jarPath = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File jarDir = jarPath.getParentFile();

            System.setProperty("LOG_DIR", new File(jarDir, "logs").getAbsolutePath());
        } catch (Exception e) {
            System.setProperty("LOG_DIR", "logs");
        }
    }

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        try {
            String filePath = args.length > 0 ? args[0] : null;
            AgentConfig config = new AgentConfig(filePath);

            FirebirdDumper dumper = new FirebirdDumper(config);
            FileCompressor compressor = new FileCompressor();
            HashCalculator hasher = new HashCalculator();
            BackupServerClient client = new BackupServerClient(config);

            BackupOrchestrator orchestrator = new BackupOrchestrator(dumper, compressor, hasher, client);
            orchestrator.run();

            System.exit(0);

        } catch (Exception e){
            log.error("Execução do Agente abortada por falha fatal: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
