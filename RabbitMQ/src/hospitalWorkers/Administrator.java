package hospitalWorkers;

import Administration.Exchange;
import Administration.TopicNames;
import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.logging.Logger;

public class Administrator {
        private Channel sniffing;
        private Channel broadcast;
        String sniffingQueue;

        public static void main(String[] args) throws Exception {
            new Administrator().adminSendInfoLoop();
        }

        public Administrator() throws Exception {
            //set connection
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            sniffing = connection.createChannel();

            //listen all hosts
            this.sniffingQueue = sniffing.queueDeclare().getQueue();
            sniffing.exchangeDeclare(Exchange.Name, Exchange.Type);
            sniffing.queueBind(sniffingQueue, Exchange.Name, "#");
            Logger.getGlobal().info("Starting sniffing channel");

            //create broadcast channel
            broadcast = connection.createChannel();
            broadcast.queueDeclare();
            broadcast.exchangeDeclare(Exchange.Name, Exchange.Type);

            sendBroadcast("Start sniffing.");
            startSniffer();
        }

        private void adminSendInfoLoop() throws Exception {
            while (true) {
                // read msg
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter message: ");
                String message = br.readLine();

                // break condition
                if ("exit".equals(message)) {
                    break;
                }
                sendBroadcast(message);
            }
        }

        public void sendBroadcast(String msg) throws Exception {
            broadcast.basicPublish(Exchange.Name, TopicNames.Info, null, msg.getBytes("UTF-8"));
        }

        public void startSniffer() throws Exception {
            Consumer consumer = new DefaultConsumer(sniffing) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println(new Date() + " " + envelope.getRoutingKey() + ": " + message);
                }
            };

            System.out.println("Waiting for messages...");
            sniffing.basicConsume(sniffingQueue, true, consumer);
        }


        public static void getInfoFromAdmin(Connection connection) throws Exception {
            Channel channel = connection.createChannel();
            String channelName = channel.queueDeclare().getQueue();
            channel.exchangeDeclare(Exchange.Name, Exchange.Type);

            channel.queueBind(channelName, Exchange.Name, TopicNames.Info);
            System.out.println("created queue: " + channelName);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println("Message from admin: " + message);
                }
            };

            System.out.println("Waiting for messages...");
            channel.basicConsume(channelName, true, consumer);
        }


}
