package simonlee.elegant.utils;

import java.util.HashMap;
import java.util.Map;

// a Bundle is a bundle with a pair of k, v, plus some k-v extras
public class Bundle<K, T> {

    private K k;
    private T v;

    private Map<String, Object> extras = new HashMap<>();

    public Bundle(K k, T v) { this.k = k; this.v = v; }

    public K getK() {
        return k;
    }

    public T getV() {
        return v;
    }

    public Object getExtra(String key) {
        return this.extras.get(key);
    }

    public void putExtra(String key, Object value) {
        this.extras.put(key, value);
    }
}
