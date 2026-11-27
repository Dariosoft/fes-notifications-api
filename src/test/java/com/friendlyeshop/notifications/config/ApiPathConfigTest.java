package com.friendlyeshop.notifications.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

class ApiPathConfigTest {

    @Test
    void prefixesControllersWithNotificationsPath() {
        PathMatchConfigurer configurer = mock(PathMatchConfigurer.class);

        new ApiPathConfig().configurePathMatch(configurer);

        verify(configurer).addPathPrefix(eq("/notifications"), any());
    }
}
