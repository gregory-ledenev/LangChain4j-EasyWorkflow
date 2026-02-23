/*
 * Copyright 2025 Gregory Ledenev (gregory.ledenev37@gmail.com)
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gl.appframework;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A thread-safe container for managing values that are computed lazily.
 * It allows registering suppliers for specific keys and ensures that the value
 * is only computed when first requested.
 *
 * @param <V> the type of values held by this container
 */
public class LazyValues<V> {
    private final ConcurrentHashMap<String, V> map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Supplier<V>> supplierMap = new ConcurrentHashMap<>();

    /**
     * Retrieves the value associated with the given key. If the value is not already present,
     * it attempts to compute it using a registered supplier.
     */
    public V get(String key) {
        return map.computeIfAbsent(key, s -> {
            Supplier<V> supplier = supplierMap.get(key);
            return supplier != null ? supplier.get() : null;
        });
    }

    /**
     * Registers a supplier to be used for lazy initialization of a value for the given key.
     */
    public void addSupplier(String key, Supplier<V> supplier) {
        supplierMap.put(key, supplier);
    }

    /**
     * Manually adds a value to the container, bypassing lazy initialization.
     */
    public void add(String key, V value) {
        map.put(key, value);
    }

    /**
     * Removes the cached value for the given key, forcing a re-computation on the next access.
     */
    public void remove(String key) {
        map.remove(key);
    }
}
