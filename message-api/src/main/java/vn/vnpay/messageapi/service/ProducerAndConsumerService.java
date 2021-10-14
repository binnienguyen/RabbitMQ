package vn.vnpay.messageapi.service;

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
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import vn.vnpay.messageapi.base.ServiceResponse;
import vn.vnpay.messageapi.config.ChannelPool;
import vn.vnpay.messageapi.constant.Message;
import vn.vnpay.messageapi.constant.MessageCode;
import vn.vnpay.messageapi.entity.RequestDto;
import vn.vnpay.messageapi.entity.ResponseDto;
import vn.vnpay.messageapi.entity.SendEntity;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static vn.vnpay.messageapi.config.ChannelPool.queueName;

@Service
public class ProducerAndConsumerService {

    @Autowired
    ChannelPool channelPool;

    public static String queue = "queueB";

    static int count = 0;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public HashMap<String, String> messageQueueB = new HashMap<String, String>();

    @Value("${redis.host}")
    public String host;

    @Value("${redis.port}")
    public String port;


    public static String key = "messages";

    public Jedis jedis = new Jedis();

    @Autowired
    @Qualifier("connectionRedis")
    JedisPool jedisPool;

    public ServiceResponse<ResponseDto> sendMessage(SendEntity sendEntity) {
        logger.info("========BEGIN========");

        String message = sendEntity.getRequestId() + " , " + sendEntity.getMessage();
        ResponseDto responseDto = new ResponseDto();

        try {
            Channel channel = channelPool.getChannel();
            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
            responseDto.setRequestId(sendEntity.getRequestId());
            responseDto.setMessageResponse("Send message: " + sendEntity.getMessage() + " successfully");
            logger.info("Message: {} | requestID: {} | count: {}", sendEntity.getMessage(), sendEntity.getRequestId(), ++count);
            channelPool.returnChannel(channel);
            logger.info("Response: {}", responseDto);
            logger.info("========END========");
            return new ServiceResponse<ResponseDto>(MessageCode.SUCCESS, Message.SUCCESS, responseDto);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
            responseDto.setMessageResponse(ex.toString());
            return new ServiceResponse<ResponseDto>(MessageCode.FAIL, Message.FAIL, responseDto);
        }
    }

    public void readMessage() {
        try {
            Channel channel = channelPool.getChannel();
            channel.queueDeclare(queue, false, false, false, null);
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    String mess = new String(body, StandardCharsets.UTF_8);
                    String token = mess.split(",")[0].trim();
                    logger.info("token: {}", token);
                    messageQueueB.put(mess.split(",")[1].trim(), mess.split(",")[2].trim() + "," + mess.split(",")[3].trim());
                    jedis = jedisPool.getResource();
                    jedis.hmset(key, messageQueueB);
                    jedis.setex(mess.split(",")[1].trim(), Long.parseLong(mess.split(",")[3].trim()), mess.split(",")[2].trim());
                    logger.info("set: {}", jedis.get(mess.split(",")[1].trim()));
                    logger.info("get ttl: {}", jedis.ttl(mess.split(",")[1].trim()));
                }
            };
            channel.basicConsume(queue, true, consumer);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Error:", ex);
        }
    }

    public ServiceResponse<ResponseDto> checkMessageV1(RequestDto requestDto) {
        logger.info("========BEGIN========");
        try {
            Map<String, String> retrieveMap =  jedis.hgetAll(key);

            ResponseDto responseDto = new ResponseDto();
            logger.info("request: {}", requestDto);
            if (retrieveMap.isEmpty()) {
                logger.info("map queue b check is empty: {}", retrieveMap.isEmpty());
                return new ServiceResponse<ResponseDto>(MessageCode.FAIL, "Not found...!!!", null);
            }
            String message = retrieveMap.get(requestDto.getRequestId());
            if (message == null) {
                logger.info("value of key {}: {}", requestDto.getRequestId(), message);
                return new ServiceResponse<ResponseDto>(MessageCode.FAIL, "Not found...!!!", null);
            }
            long time = Long.parseLong(message.split(",")[1].trim());
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS ");
            Date date = new Date(time);
            logger.info("parse: {}", time);
            if (time < System.currentTimeMillis()) {
                logger.info("Expired date: {}", date);
                return new ServiceResponse<ResponseDto>(MessageCode.FAIL, "Not found...!!!", null);
            }
            responseDto.setRequestId(requestDto.getRequestId());
            responseDto.setMessageResponse(message.split(",")[0]);
            responseDto.setDateExpired(dateFormat.format(date));
            logger.info("Response: {}", responseDto);
            logger.info("========END========");
            return new ServiceResponse<ResponseDto>(MessageCode.SUCCESS, Message.SUCCESS, responseDto);
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            logger.info("========END========");
            return new ServiceResponse<ResponseDto>(MessageCode.FAIL, Message.FAIL, null);
        }
    }

    public ServiceResponse<ResponseDto> checkMessageV2(RequestDto requestDto) {
        logger.info("========BEGIN========");
        try {
            String getSet = jedis.get(requestDto.getRequestId());
            ResponseDto responseDto = new ResponseDto();
            logger.info("request: {}", requestDto);
            if (getSet == null) {
                logger.info("map queue b check is empty");
                return new ServiceResponse<ResponseDto>(MessageCode.FAIL, "Not found...!!!", null);
            }
            //check expired
            long time = jedis.ttl(requestDto.getRequestId());
            logger.info("get ttl: {}", time);
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date date = new Date(time);
            logger.info("convert date: {}", dateFormat.format(new Date(time)));
            if (time < System.currentTimeMillis()) {
                logger.info("Expired date: {}", dateFormat.format(date));
                return new ServiceResponse<ResponseDto>(MessageCode.FAIL, "Not found...!!!", null);
            }
            responseDto.setRequestId(requestDto.getRequestId());
            responseDto.setMessageResponse(getSet);
            responseDto.setDateExpired(dateFormat.format(date));
            logger.info("Response: {}", responseDto);
            logger.info("========END========");
            return new ServiceResponse<ResponseDto>(MessageCode.SUCCESS, Message.SUCCESS, responseDto);
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            return new ServiceResponse<ResponseDto>(MessageCode.FAIL, Message.FAIL, null);
        }
    }


}
