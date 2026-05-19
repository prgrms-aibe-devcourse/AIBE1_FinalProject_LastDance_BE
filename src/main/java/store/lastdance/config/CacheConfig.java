package store.lastdance.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache("userProfile",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build()
        );

        manager.registerCustomCache("nickname",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build()
        );

        manager.registerCustomCache("expenseTrend",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build()
        );

        manager.registerCustomCache("groupExpenseTrend",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build()
        );

        return manager;
    }
}
