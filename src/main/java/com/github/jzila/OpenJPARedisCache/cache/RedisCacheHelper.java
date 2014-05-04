package com.github.jzila.OpenJPARedisCache.cache;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedisCacheHelper {
    private static final int DEFAULT_REDIS_PORT = 6379;
    private static final String CONFIG_FILE_NAME = "redis_openjpa.xml";
    private String _cachePrefix = "";

    private static RedisCacheHelper _instance;

    private List<JedisShardInfo> _shards = new ArrayList<>();
    private ShardedJedisPool _jedisPool = null;

    static synchronized RedisCacheHelper getInstance() {
        if (_instance == null) {
            _instance = new RedisCacheHelper();
        }
        return _instance;
    }

    static String serialize(Serializable o) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream oStream = new ObjectOutputStream(stream);
            oStream.writeObject(o);
            return Base64.encodeBase64String(stream.toByteArray());
        } catch (IOException ex) {
            return "";
        }
    }

    static Object deserialize(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(Base64.decodeBase64(s));
            ObjectInputStream iStream = new ObjectInputStream(stream);
            return iStream.readObject();
        } catch (IOException|ClassNotFoundException ex) {
            return null;
        }
    }

    ShardedJedisPool getJedisPool() {
        return _jedisPool;
    }

    String getCachePrefix() {
        return _cachePrefix;
    }

    Set<String> getAllKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        for (JedisShardInfo shardInfo: _shards) {
            Jedis shard = shardInfo.createResource();
            keys.addAll(shard.keys(pattern));
        }

        return keys;
    }

    void clearAll(String pattern) {
        for (JedisShardInfo shardInfo: _shards) {
            Jedis shard = shardInfo.createResource();
            Set<String> keys = shard.keys(pattern);
            for (String key: keys) {
                shard.del(key);
            }
        }
    }

    private RedisCacheHelper() {
        try {
            XMLConfiguration config = new XMLConfiguration(CONFIG_FILE_NAME);

            List<HierarchicalConfiguration> serverProps = config.configurationsAt("redis.servers.server");
            for (HierarchicalConfiguration serverProp: serverProps) {
                String host = serverProp.getString("host");
                Integer port = serverProp.getInteger("port", DEFAULT_REDIS_PORT);

                _shards.add(new JedisShardInfo(host, port));
            }
            _cachePrefix = config.getString("redis.cacheprefix");
        } catch (ConfigurationException ex) {
        } finally {
            if (_shards.isEmpty()) {
                _shards.add(new JedisShardInfo("localhost", DEFAULT_REDIS_PORT));
            }
            _jedisPool = new ShardedJedisPool(new JedisPoolConfig(), _shards);
        }
    }
}
