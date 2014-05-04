package com.github.jzila.OpenJPARedisCache.cache;

import org.apache.openjpa.datacache.AbstractDataCache;
import org.apache.openjpa.datacache.DataCacheManager;
import org.apache.openjpa.datacache.DataCachePCData;
import org.apache.openjpa.util.OpenJPAId;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.util.Set;

public class RedisDataCache extends AbstractDataCache {
    protected String _dataCachePrefix = "";
    protected String _classCachePrefix = "";
    protected ShardedJedisPool _jedisPool = null;

    @Override
    public synchronized void initialize(DataCacheManager manager) {
        super.initialize(manager);

        RedisCacheHelper cacheHelper = RedisCacheHelper.getInstance();
        _jedisPool = cacheHelper.getJedisPool();
        _dataCachePrefix = cacheHelper.getCachePrefix() + "data:";
        _classCachePrefix = cacheHelper.getCachePrefix() + "class:";
    }

    public void writeLock() {}

    public void writeUnlock() {}

    @Override
    protected boolean pinInternal(Object o) {
        return false;
    }

    @Override
    protected boolean unpinInternal(Object o) { return false; }

    @Override
    protected DataCachePCData removeInternal(Object o) {
        OpenJPAId oId = (OpenJPAId) o;
        String objectKey = _dataCachePrefix + oId.getType().getName() + ":" + o.toString();

        ShardedJedis jedis = _jedisPool.getResource();
        try {
            DataCachePCData data = getInternalJedis(jedis, objectKey);
            String classKey = _classCachePrefix + data.getType().getName();

            jedis.del(objectKey);
            jedis.srem(classKey, objectKey);

            return data;
        } finally {
            _jedisPool.returnResource(jedis);
        }
    }

    @Override
    protected void removeAllInternal(Class<?> aClass, boolean b) {
        String classKey = _classCachePrefix + aClass.getName();

        ShardedJedis jedis = _jedisPool.getResource();
        try {
            Set<String> classObjects = jedis.smembers(classKey);

            for (String entry : classObjects) {
                jedis.srem(classKey, entry);
                jedis.del(entry);
            }
        } finally {
            _jedisPool.returnResource(jedis);
        }

    }

    @Override
    protected DataCachePCData getInternal(Object o) {
        OpenJPAId oId = (OpenJPAId) o;
        String objectKey = _dataCachePrefix + oId.getType().getName() + ":" + o.toString();

        ShardedJedis jedis = _jedisPool.getResource();
        try {
            DataCachePCData data = getInternalJedis(jedis, objectKey);
            return data;
        } finally {
            _jedisPool.returnResource(jedis);
        }
    }

    @Override
    protected DataCachePCData putInternal(Object o, DataCachePCData dataCachePCData) {
        String classKey = _classCachePrefix + dataCachePCData.getType().getName();

        OpenJPAId oId = (OpenJPAId) o;
        String objectKey = _dataCachePrefix + oId.getType().getName() + ":" + o.toString();

        ShardedJedis jedis = _jedisPool.getResource();
        try {
            jedis.set(objectKey, RedisCacheHelper.serialize(dataCachePCData));
            jedis.sadd(classKey, objectKey);

            return dataCachePCData;
        } finally {
            _jedisPool.returnResource(jedis);
        }
    }

    @Override
    protected void clearInternal() {
        // TODO there's a race here, obviously.
        RedisCacheHelper.getInstance().clearAll(_classCachePrefix + "*");
        RedisCacheHelper.getInstance().clearAll(_dataCachePrefix + "*");
    }


    private DataCachePCData getInternalJedis(ShardedJedis jedis, String objectKey) {
        String fromCache = jedis.get(objectKey);
        return (DataCachePCData) RedisCacheHelper.deserialize(fromCache);
    }

}
