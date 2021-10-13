package vn.vnpay.messageapi.config;

import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.NoSuchElementException;

@Component
public class ChannelPool implements Cloneable {
    final
    ChannelFactory channelFactory;

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    public static String queueName = "queueA";

    private GenericObjectPool<Channel> internalPool;

    public ChannelPool(ChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    public GenericObjectPool<Channel> getInternalPool() {
        return internalPool;
    }

    @PostConstruct
    public void init() {
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMinIdle(1);
        genericObjectPoolConfig.setMaxTotal(5);
        internalPool = new GenericObjectPool<Channel>(channelFactory, genericObjectPoolConfig);
    }

    private void closeInternalPool() {
        try {
            internalPool.close();
        } catch (Exception e) {
            logger.error("Could not destroy the pool", e);
        }
    }

    public void returnChannel(Channel channel) {
        try {
            if (channel.isOpen()) {
                internalPool.returnObject(channel);
            } else {
                internalPool.invalidateObject(channel);
            }
        } catch (Exception e) {
            logger.error("Could not return the resource to the pool", e);
        }
    }

    public Channel getChannel() throws Exception {
        try {
            return internalPool.borrowObject();
        } catch (NoSuchElementException nse) {
            if (null == nse.getCause()) { // The exception was caused by an exhausted pool
                throw new ChannelException("Could not get a resource since the pool is exhausted", nse);
            }
            // Otherwise, the exception was caused by the implemented activateObject() or ValidateObject()
            throw new ChannelException("Could not get a resource from the pool", nse);
        } catch (Exception e) {
            throw new Exception("Could not get a resource from the pool", e);
        }
    }
}
