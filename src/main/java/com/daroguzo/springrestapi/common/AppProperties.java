package com.daroguzo.springrestapi.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "my-app")
@Getter @Setter
public class AppProperties {

    @NotNull
    private String adminUsername;

    @NotNull
    private String adminPassword;

    @NotNull
    private String userUsername;

    @NotNull
    private String userPassword;

    @NotNull
    private String clientId;

    @NotNull
    private String clientSecret;
}
