import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class EmitLogTopic {

  private static final String EXCHANGE_NAME = "topic_logs";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");

        String routingKey = getRouting(argv);
        String message = getMessage(argv);

        channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
        System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
    }
  }

  private static String getMessage(String[] strings) {
    if (strings.length < 2) return "Hello World!";
    return joinStrings(strings, " ", 1);
  }

  private static String joinStrings(String[] strings, String delimiter, int startIndex) {
    StringBuilder sb = new StringBuilder();
    for (int i = startIndex; i < strings.length; i++) {
        sb.append(strings[i]);
        if (i < strings.length - 1) {
            sb.append(delimiter);
        }
    }
    return sb.toString();
  }

  private static String getRouting(String[] strings) {
    if (strings.length < 1) {
        return "anonymous.info";
    }
    return strings[0];
  }
  
}