package net.bitnp.guildofpioneers.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimezoneConfig {

    @Bean
    public TimeZone applicationTimeZone(@Value("${app.timezone:UTC+8}") String timezone) {
        TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of(timezone));
        TimeZone.setDefault(timeZone);
        return timeZone;
    }
}
