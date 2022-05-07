package org.apache.fineract.infrastructure.core.utils;

import java.util.Arrays;
import org.springframework.core.env.Environment;

public class ProfileUtils {

    public static final String BASICAUTH_PROFILE_NAME = "basicauth";
    public static final String TWOFACT_AUTH_PROFILE_NAME = "twofactor";
    public static final String SPRING_UPGRADEDB_PROFILE_NAME = "upgradeDB";

    private final Environment environment;

    public ProfileUtils(final Environment environment) {
        this.environment = environment;
    }

    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }

    public boolean isActiveProfile(String profile) {
        String[] activeProfiles = getActiveProfiles();
        return Arrays.asList(activeProfiles).contains(profile);
    }
}
