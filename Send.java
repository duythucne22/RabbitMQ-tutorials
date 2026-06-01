import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.util.Map;

public class Send {
    private static final String QUEUE_NAME = "hello";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
        ) {
            Map<String, Object> argsMap = Map.of("x-queue-type", "quorum");
            channel.queueDeclare(QUEUE_NAME, true, false, false, argsMap);

            String message = "Hello World";

            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println(" [*] Sent: " + message);
        }                  
    }
}