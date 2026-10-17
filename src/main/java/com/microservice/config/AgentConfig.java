package com.microservice.config;

import java.io.FileInputStream;
import java.util.Properties;

public class AgentConfig {

    Properties properties = new Properties();

   FileInputStream inputStream =
           AgentConfig.class
                   .getClassLoader()
                   .getResourceAsStream("agent.properties");
}