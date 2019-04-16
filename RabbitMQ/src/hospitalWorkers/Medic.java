package hospitalWorkers;

import Administration.ExaminationType;
import Administration.Exchange;
import Administration.TopicNames;
import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Medic {
    private Channel requestsQueue;
    private Channel resultsQueue;
    private final String medicName;
    public Medic() throws Exception {
        //set connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        Administrator.getInfoFromAdmin(connection);

        //set requests queue
        requestsQueue = connection.createChannel();
        requestsQueue.queueDeclare();
        requestsQueue.exchangeDeclare(Exchange.Name, Exchange.Type);

        //init doctor
        System.out.println("Enter medic name:");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        this.medicName = br.readLine();

        //set results queue
        resultsQueue = connection.createChannel();
        String queueName = resultsQueue.queueDeclare(TopicNames.getAnyExamResponse(medicName), false, false, false, null).getQueue();
        resultsQueue.exchangeDeclare(Exchange.Name, BuiltinExchangeType.TOPIC);

        resultsQueue.queueBind(queueName, Exchange.Name, queueName);
        System.out.println("created queue: " + queueName);

        Consumer consumer = new DefaultConsumer(resultsQueue) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received: " + message);
            }
        };

        System.out.println("Waiting for messages...");
        resultsQueue.basicConsume(queueName, true, consumer);
    }

    public static void main(String[] args) throws Exception {
        new Medic().sendRequest();
    }

    private void sendRequest() throws Exception {

        while (true) {

            // read msg
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter examination type: ");
            ExaminationType examinationType = ExaminationType.valueOf(br.readLine().toUpperCase());

            String message = medicName + "." + examinationType +".";
            requestsQueue.basicPublish(Exchange.Name, TopicNames.setRequestTopic(examinationType, medicName), null, (message).getBytes("UTF-8"));
            System.out.println("Sent: " + message);
        }
    }

}
