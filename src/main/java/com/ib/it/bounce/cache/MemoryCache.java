package com.ib.it.bounce.cache;

import java.util.Set;

public interface MemoryCache <K, V> {

    V get(K key);

    Set<K> getAllKeys();

    void put(K key, V value);

    void remove(K key);

    void clear();
}
