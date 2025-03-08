package com.ib.it.bounce.cache;

public interface MemoryCache <K, V> {

    V get(K key);

    void put(K key, V value);

    void remove(K key);

    void clear();
}
