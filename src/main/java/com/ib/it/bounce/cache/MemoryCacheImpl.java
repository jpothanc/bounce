package com.ib.it.bounce.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

@Component
public class MemoryCacheImpl<K, V> implements MemoryCache<K, V> {
    private Cache<K, V> cache;

    public MemoryCacheImpl() {
        cache = Caffeine.newBuilder()
                .maximumSize(100)
                .build();
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
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
}
