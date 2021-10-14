package vn.vnpay.messageapi.config;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
@AllArgsConstructor
@NoArgsConstructor
public class RedisConfig {
    @Value("${redis.host}")
    public String host;

    @Value("${redis.port}")
    public String port;

    public static JedisPool pool = null;

    @Bean("connectionRedis")
    public JedisPool getRedis(){
        return pool = new JedisPool("127.0.0.1", 6379);
    }
}
