package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class UDP {
    private boolean running;

    public DatagramSocket socket;

    public UDP(Host me) {
        // Create socket
        InetSocketAddress address = new InetSocketAddress(me.getIp(), me.getPort());
        try {
            socket = new DatagramSocket(address);
            socket.setSoTimeout(1000); 
        } catch(SocketException e) {
            System.err.println("Cannot Create Socket: " + e);
        }
    }

    public boolean send(InetSocketAddress dest, String m) {
        // Create output buffer
        byte[] buf = new byte[256];
        buf = m.getBytes();

        // Create packet
        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length, dest);
            socket.send(packet);
        } catch(IOException e) {
            System.out.println("Client.Send IOException Error: " + e);
            return false;
        } catch (IllegalArgumentException e) {
            System.out.printf("OutBuf: %s\n", buf.toString());
            System.out.printf("Length: %s\n", buf.length);
            System.out.printf("Message: %s\n", m);
            System.out.println("Client.Send IllegalArgumentException Error: " + e);
            return false;
        }
        return true;
    }

    public DatagramPacket receive() {
        // System.out.println("Inside UDP Receive");
        byte[] buf = new byte[256];

        // Receive Packet
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);

            return packet;
            // InetAddress address = packet.getAddress();
            // int port = packet.getPort();
            // packet = new DatagramPacket(inBuf, inBuf.length, address, port);
            
            // String message = new String(packet.getData(), packet.getOffset(),  packet.getLength()).trim();
            // inBuf = new byte[256];
            // Arrays.fill(inBuf,(byte)0);
        } catch (SocketTimeoutException e) {
            // Do nothing
            return null;
        } catch (IOException e) {
            System.err.println("Server Cannot Receive Packet: " + e);
            return null;
        }
    }
}
