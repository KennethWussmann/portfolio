package de.ketrwu.portfolio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationConfiguration {

    private Address address;

    private String[] domains;

    @Data
    public static class Address {

        private String name;
        private String street;
        private String zip;
        private String country;

    }
}
