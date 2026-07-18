package com.czetsuyatech.nerv.exception.autoconfigure;

import com.czetsuyatech.nerv.exception.api.origin.NervOrigin;
import com.czetsuyatech.nerv.exception.api.origin.NervOriginResolver;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

public class DefaultNervOriginResolver implements NervOriginResolver {

    private final Environment environment;
    private final BuildProperties buildProperties;

    public DefaultNervOriginResolver(Environment environment, BuildProperties buildProperties) {
        this.environment = environment;
        this.buildProperties = buildProperties;
    }

    @Override
    public NervOrigin resolve() {
        return NervOrigin.builder()
                .service(resolveService())
                .instance(resolveInstance())
                .version(resolveVersion())
                .environment(resolveEnvironment())
                .build();
    }

    private String resolveService() {
        return environment.getProperty("spring.application.name", "unknown-service");
    }

    private String resolveInstance() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "unknown-instance";
        }
    }

    private String resolveVersion() {
        return Optional.ofNullable(buildProperties)
                .map(BuildProperties::getVersion)
                .orElse("unknown-version");
    }

    private String resolveEnvironment() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 ? "default" : String.join(",", profiles);
    }

}
