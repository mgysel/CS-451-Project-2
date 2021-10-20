package cs451;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Server extends Thread {

    private DatagramSocket socket;
    private String output;
    private boolean running;
    // TODO: HOW BIG SHOULD BUFFER BE? IS IT IN GLOBAL VARIABLES?
    private byte[] buf = new byte[256];

    public Server(DatagramSocket socket, String output) {
        // Create datagram socket with ip and port
        this.socket = socket;
        this.running = false;
        this.output = output;
    }

    // NOTE: start is used to run a thread asynchronously
    public void run() {
        System.out.println("INSIDE SERVER.RUN");

        running = true;

        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                System.err.println("Server Cannot Receive Packet: " + e);
            }
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            String message = new String(packet.getData(), 0, packet.getLength());

            System.out.printf("Received %s\n", message);

            // TODO: Log packet
        }
    }

    public void close() {
        running = false;
        socket.close();
    }
}
