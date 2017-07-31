package com.core.helper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Administrator on 2015/12/8.
 */
public class CacheManager {
    private static Map<String, CacheInfo> cacheMap = new HashMap<String, CacheInfo>();

    static class CacheInfo {
        private String key;
        private Object value;
        private long timeout;

        public CacheInfo(String key, Object value, long timeout) {
            this.key = key;
            this.value = value;
            this.timeout = System.currentTimeMillis() + timeout * 1000;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            if (timeout <= 0 || timeout >= System.currentTimeMillis())
                return false;
            else
                return true;
        }
    }

    public synchronized static void put(String key, Object value, long timeout) {
        cacheMap.put(key, new CacheInfo(key, value, timeout));
    }

    public synchronized static Object get(String key) {
        if (hasCache(key))
            return cacheMap.get(key).getValue();
        else
            return null;
    }

    public synchronized static boolean hasCache(String key) {
        if (cacheMap.containsKey(key)) {
            if (cacheMap.get(key).isExpired()) {
                remove(key);
                return false;
            } else
                return true;
        } else
            return false;
    }

    public synchronized static void removeAll() {
        cacheMap.clear();
    }

    public synchronized static void remove(String key) {
        cacheMap.remove(key);
    }

    public synchronized static int getSize() {
        return getAll().size();
    }

    public synchronized static Map<String, Object> getAll() {
        Map<String, Object> map = new HashMap<String, Object>();
        Iterator iterator = cacheMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheInfo> entry = (Map.Entry<String, CacheInfo>) iterator.next();
            map.put(entry.getKey(), entry.getValue().getValue());
        }
        return map;
    }
}