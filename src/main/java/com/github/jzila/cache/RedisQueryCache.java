package com.github.jzila.cache;

import org.apache.openjpa.datacache.*;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.util.Collection;


public class RedisQueryCache extends AbstractQueryCache {
    protected String _queryCachePrefix = "";
    protected ShardedJedisPool _jedisPool = null;

    @Override
    public synchronized void initialize(DataCacheManager manager) {
        super.initialize(manager);

        RedisCacheHelper cacheHelper = RedisCacheHelper.getInstance();
        _jedisPool = cacheHelper.getJedisPool();
        _queryCachePrefix = cacheHelper.getCachePrefix() + "query:";
    }

    @Override
    public void writeLock() {}

    @Override
    public void writeUnlock() {}

    @Override
    protected boolean pinInternal(QueryKey queryKey) {
        return false;
    }

    @Override
    protected boolean unpinInternal(QueryKey queryKey) {
        return false;
    }

    @Override
    protected Collection keySet() {
        return RedisCacheHelper.getInstance().getAllKeys(_queryCachePrefix + "*");
    }

    @Override
    protected QueryResult putInternal(QueryKey queryKey, QueryResult objects) {
        String cacheKey = _queryCachePrefix + queryKey.getCandidateTypeName() + ":" + queryKey.toString();

        ShardedJedis jedis = _jedisPool.getResource();
        try {
            jedis.set(cacheKey, RedisCacheHelper.serialize(objects));

            return objects;
        } finally {
            _jedisPool.returnResource(jedis);
        }
    }

    @Override
    protected QueryResult getInternal(QueryKey queryKey) {
        String cacheKey = _queryCachePrefix + queryKey.getCandidateTypeName() + ":" + queryKey.toString();

        ShardedJedis jedis = _jedisPool.getResource();
        try {
            QueryResult result = getInternalJedis(jedis, cacheKey);
            return result;
        } finally {
            _jedisPool.returnResource(jedis);
        }
    }

    @Override
    protected QueryResult removeInternal(QueryKey queryKey) {
        String cacheKey = _queryCachePrefix + queryKey.getCandidateTypeName() + ":" + queryKey.toString();

        ShardedJedis jedis = _jedisPool.getResource();
        try {
            QueryResult data = getInternalJedis(jedis, cacheKey);

            jedis.del(cacheKey);

            return data;
        } finally {
            _jedisPool.returnResource(jedis);
        }
    }

    @Override
    protected void clearInternal() {
        RedisCacheHelper.getInstance().clearAll(_queryCachePrefix + "*");
    }

    private QueryResult getInternalJedis(ShardedJedis jedis, String cacheKey) {
        String fromCache = jedis.get(cacheKey);
        return (QueryResult) RedisCacheHelper.deserialize(fromCache);
    }

}
