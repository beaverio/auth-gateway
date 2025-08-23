package com.beaver.authgateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

@Configuration
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 604800) // 7 days
public class SessionConfig {
    // Spring Session will automatically configure the WebSessionStore bean
    // No need to manually define it when using @EnableRedisWebSession
}
