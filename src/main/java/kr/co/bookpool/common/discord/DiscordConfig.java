package kr.co.bookpool.common.discord;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DiscordProperties.class)
public class DiscordConfig {
}
