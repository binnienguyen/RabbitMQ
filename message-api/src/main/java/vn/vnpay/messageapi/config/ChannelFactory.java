package vn.vnpay.messageapi.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
public class ChannelFactory implements PooledObjectFactory<Channel> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("conn")
    Connection connection;

    @Override
    public PooledObject<Channel> makeObject() throws Exception {
        return new DefaultPooledObject<Channel>(connection.createChannel());
    }

    @Override
    public void destroyObject(PooledObject<Channel> pooledObject) throws Exception {
        final Channel channel = pooledObject.getObject();
        if (channel.isOpen()) {
            try {
                channel.close();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<Channel> pooledObject) {
        final Channel channel = pooledObject.getObject();
        return channel.isOpen();
    }

    @Override
    public void activateObject(PooledObject<Channel> pooledObject) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<Channel> pooledObject) throws Exception {
    }
}
