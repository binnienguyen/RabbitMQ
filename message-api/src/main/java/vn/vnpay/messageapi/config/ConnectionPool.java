package vn.vnpay.messageapi.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionPool {

    @Value("${spring.rabbitmq.host}")
    public String host;

    @Value("${spring.rabbitmq.port}")
    public String port;

    @Value("${spring.rabbitmq.username}")
    public String username;

    @Value("${spring.rabbitmq.password}")
    public String password;

    private ConnectionFactory connectionFactory;

    @Bean("conn")
    public Connection getConnection() {
        try {
            connectionFactory = new ConnectionFactory();
            connectionFactory.setHost(host);
            connectionFactory.setPort(Integer.parseInt(port));
            connectionFactory.setUsername(username);
            connectionFactory.setPassword(password);
            return connectionFactory.newConnection();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }
}
