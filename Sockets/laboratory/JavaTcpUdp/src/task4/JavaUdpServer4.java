package task4;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class JavaUdpServer4 {
    public static void main(String args[])
    {
        System.out.println("JAVA UDP SERVER");
        DatagramSocket socket = null;
        int portNumber = 9011;

        try{
            socket = new DatagramSocket(portNumber);
            byte[] receiveBuffer = new byte[1024];

            while(true) {
                //receive
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                String msg = new String(receivePacket.getData());

                System.out.println(receivePacket.getSocketAddress());
                System.out.println(receivePacket.getAddress());
                System.out.println(receivePacket.getPort());
                byte[] sendBuffer = "".getBytes();
                if(msg.contains("Java")){
                    System.out.println("send to java");
                    sendBuffer = "Hello Java!".getBytes();
                } else if(msg.contains("Python")) {
                    System.out.println("send to python");
                    sendBuffer = "Hello Python!".getBytes();
                }

                //send
                InetAddress address = receivePacket.getAddress();
                int port = receivePacket.getPort();
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
                socket.send(sendPacket);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
