package com.taskmanager.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "db")
public class DatabaseConfig {

    private String host;
    private String port;
    private String user;
    private String password;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUrl() {
        return String.format(
                "jdbc:mysql://%s:%s/defaultdb?ssl-mode=REQUIRED",
                this.host,
                this.port
        );
    }
}