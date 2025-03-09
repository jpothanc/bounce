package com.ib.it.bounce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "email")  // ✅ Binds "email" properties
public class EmailConfig {

    public static final String TEAM_DEVELOPMENT = "development";
    public static final String TEAM_DATABASE = "database";


    private boolean enabled;
    private String host;
    private int port;
    private String developmentTeamEmail;
    private String databaseTeamEmail;

    public String getEmailRecipient(String team) {
        return switch (team) {
            case TEAM_DEVELOPMENT-> developmentTeamEmail;
            case TEAM_DATABASE -> databaseTeamEmail;
            default -> throw new IllegalArgumentException("Invalid team: " + team);
        };
    }
}
