package pl.edu.agh.dsrg.sr.protos;

import pl.edu.agh.dsrg.sr.protos.BankOperationProtos.BankOperation;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("bank operations started");

        DatagramSocket socket = new DatagramSocket();

        for(int i = 0; i < 2000; i++){
            InetAddress address = InetAddress.getByName("224.0.0.7");

            BankOperation operation;
            operation = BankOperation.newBuilder()
                    .setValue(Math.random() / 100 + 1.0)
                    .setType(BankOperation.OperationType.ADD).build(); //.setType(BankOperation.OperationType.MULTIPLY).build();


            byte[] sendBuffer = operation.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, 6789);
            socket.send(sendPacket);
            Thread.sleep((long) (Math.random()*10));
        }

        System.out.println("bank operations ended");
    }
}