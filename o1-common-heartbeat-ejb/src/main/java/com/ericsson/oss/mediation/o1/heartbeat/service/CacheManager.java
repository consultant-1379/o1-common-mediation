/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.o1.heartbeat.service;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.cache.Cache;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;

/**
 * CacheManager lazy loads the underlying cache and exposes basic operations.
 */
public class CacheManager<T> {

    private String name = null;
    private Cache<String, T> cache = null;

    @Inject
    private CacheProviderBean bean;

    public void initializeName(String name) {
        this.name = name;
    }

    public boolean contains(final String key) {
        return getCache().containsKey(key);
    }

    public T get(final String key) {
        return getCache().get(key);
    }

    public Map<String, T> getAll(Predicate<Cache.Entry<String, T>> filter) {
        return StreamSupport.stream(this.getCache().spliterator(), false)
                .filter(filter)
                .collect(Collectors.toMap(Cache.Entry::getKey, Cache.Entry::getValue));
    }

    public void remove(final String key) {
        getCache().remove(key);
    }

    public void put(final String key, final T value) {
        getCache().put(key, value);
    }

    public void update(final String key, Function<T, String> function) {
        T entry = getCache().get(key);
        function.apply(entry);
        getCache().put(key, entry);
    }

    private Cache<String, T> getCache() {
        if (this.name == null) {
            throw new IllegalStateException("Cache name is not initialized");
        }
        if (this.cache == null) {
            this.cache = bean.createOrGetModeledCache(name);
        }
        return cache;
    }
}
