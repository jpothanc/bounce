package com.ib.it.bounce.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ib.it.bounce.config.MonitoringConfig;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class MemoryCacheImpl<K, V> implements MemoryCache<K, V> {
    private final Cache<K, V> cache;

    public MemoryCacheImpl(MonitoringConfig monitoringConfig) {
        int cacheExpiry = Optional.ofNullable(monitoringConfig.getCacheExpiry()).filter(expiry -> expiry > 0).orElse(3);

        cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(cacheExpiry, TimeUnit.DAYS)
                .build();
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }
    public Set<K> getAllKeys() {
        return cache.asMap().keySet();
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    /**
     * Retrieves all alerts that match the "alerts-email-YYYYMMDD" pattern.
     */
    public List<V> getAllEmailAlerts() {
        String todayKeyPrefix = "alerts-email-" + LocalDate.now().toString().replace("-", "");

        return cache.asMap().entrySet().stream()
                .filter(entry -> entry.getKey().toString().startsWith(todayKeyPrefix))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }


}
