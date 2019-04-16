package hospitalWorkers;

import Administration.ExaminationType;
import Administration.Exchange;

import Administration.TopicNames;
import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Technician{
    private List<ExaminationType> examinations = new ArrayList<>();
    private Connection connection;
    private Channel responseQueue;

    public static void main(String[] argv) throws Exception {
        new Technician().initChannels();
    }

    public Technician() throws Exception {
        //set connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        this.connection = factory.newConnection();
        Administrator.getInfoFromAdmin(connection);

        //set response queue
        responseQueue = connection.createChannel();
        responseQueue.queueDeclare();
        responseQueue.exchangeDeclare(Exchange.Name, Exchange.Type);

        //get specializations
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Enter first specialization: ");
        examinations.add(ExaminationType.valueOf(br.readLine().toUpperCase()));

        System.out.println("Enter second specialization: ");
        examinations.add(ExaminationType.valueOf(br.readLine().toUpperCase()));
    }

    public void initChannels() throws Exception {
        for (ExaminationType examination : examinations) {
            channelForEachExamination(examination);
        }
    }

    private void channelForEachExamination(ExaminationType examinationType) throws Exception {
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare(TopicNames.setRequestTopic(examinationType, "*"), false, false, false, null).getQueue();
        channel.exchangeDeclare(Exchange.Name, Exchange.Type);

        channel.queueBind(queueName, Exchange.Name, queueName);
        System.out.println("created queue: " + queueName);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received message: " + message);
                responseQueue.basicPublish(Exchange.Name, TopicNames.getExamResponse(envelope.getRoutingKey()), null, (message+"done").getBytes("UTF-8"));
            }
        };

        System.out.println("Waiting for messages...");
        channel.basicConsume(queueName, true, consumer);
    }


}
