package vn.vnpay.messagecore;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import vn.vnpay.messagecore.config.ChannelPool;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static vn.vnpay.messagecore.config.ChannelPool.queueName;

@Component
public class ReadMessage {

    private static final Logger logger = LoggerFactory.getLogger(MessageCoreApplication.class);

    public static final String queue = "queueB";

    @Autowired
    public ChannelPool channelPool;

    @Value("${redis.host}")
    public String host;

    @Value("${redis.port}")
    public String port;

    @Autowired
    @Qualifier("connectionRedis")
    JedisPool jedisPool;

    public static String key = "messageQueueA";

    public Jedis jedis = new Jedis();

    @PostConstruct
    public void readMsg() {
        try {
            Channel channel = channelPool.getChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            Map<String, String> message = new HashMap<String, String>();
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String mess = new String(body, StandardCharsets.UTF_8);
                    String[] getString = mess.split(",");
                    String token = UUID.randomUUID().toString();
                    message.put(token, mess);
                    jedis = jedisPool.getResource();
                    jedis.hmset(key, message);
                    logger.info("hset: {}", jedis.hgetAll(key));
                    try {
                        Channel channel1 = channelPool.getChannel();
                        int time = Integer.parseInt(String.valueOf(10)) * 60 * 1000;
                        String messageSend = getString[0] + ", publish to queue B success" + ", " + (System.currentTimeMillis() + time);
                        String mess2 = token + ", " + messageSend;
                        channel1.queueDeclare(queue, false, false, false, null);
                        channel1.basicPublish("", queue, null, mess2.getBytes(StandardCharsets.UTF_8));
                        logger.info("Message send to queue B: {}", mess2);
                        logger.info("token: {} | message: {}", token, messageSend);
                        channelPool.returnChannel(channel1);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage());
                    }
                }
            };
            channel.basicConsume(queueName, true, consumer);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }
}
