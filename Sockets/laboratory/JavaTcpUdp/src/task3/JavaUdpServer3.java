package task3;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class JavaUdpServer3 {
    public static void main(String args[])
    {
        System.out.println("JAVA UDP SERVER");
        DatagramSocket socket = null;
        int portNumber = 9010;

        try{
            socket = new DatagramSocket(portNumber);
            byte[] receiveBuffer = new byte[1024];

            while(true) {
                //receive
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                String msg = new String(receivePacket.getData());

                //convert
                int nb = ByteBuffer.wrap(receiveBuffer).getInt();
                nb = Integer.reverseBytes(nb);
                System.out.println("Client value is: " + nb);
                nb++;

                //send
                InetAddress address = receivePacket.getAddress();
                int port = receivePacket.getPort();
                byte[] sendBuffer = ByteBuffer.allocate(4).putInt(nb).array();
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
