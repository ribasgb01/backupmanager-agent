package com.microservice.backupagent.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AgentConfig {

    private final String serverUrl;
    private final String apiKey;
    private final String dbHost;
    private final int dbPort;
    private final String dbPath;
    private final String dbUser;
    private final String dbPassword;
    private final String tempDir;

    public AgentConfig(String configPath){

        Properties properties = new Properties();

        File configFile;

        if(configPath != null && !configPath.isBlank()){
            File path = new File(configPath);

            configFile = path.isDirectory() ? new File(path, "agent.properties") : path;
        } else {
            configFile = new File("agent.properties");
        }

        if (!configFile.exists()) {
            throw new IllegalStateException("Arquivo de configuração não encontrado em: " + configFile.getAbsolutePath());
        }

        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);

        } catch (IOException e){
            throw new IllegalStateException("Erro ao ler o arquivo agent.properties", e);
        }

        this.serverUrl = properties.getProperty("server.url");
        this.apiKey = properties.getProperty("server.api-key");
        this.dbHost = properties.getProperty("db.host", "localhost");

        String portStr = properties.getProperty("db.port", "3050");
        dbPort = Integer.parseInt(portStr.trim());

        this.dbPath = properties.getProperty("db.path");
        this.dbUser = properties.getProperty("db.user", "SYSDBA");
        this.dbPassword = properties.getProperty("db.password", "masterkey");
        this.tempDir = properties.getProperty("backup.temp-dir", "/tmp/backups");

        if (serverUrl == null || serverUrl.isBlank() || apiKey == null || apiKey.isBlank() || dbPath == null || dbPath.isBlank()){
            throw new IllegalStateException("Erro: server.url, server.api-key e db.path são obrigatórios no agent.properties");
        }
    }

    public AgentConfig(){
        this(null);
    }
}