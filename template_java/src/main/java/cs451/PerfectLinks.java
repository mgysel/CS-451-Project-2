package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PerfectLinks extends Thread {
    private Host me;
    private DatagramSocket socket;
    private List<Config> configs;
    private List<Host> hosts;
    private String output;

    private boolean running;

    // TODO: DOES BUFFER SIZE NEED TO BE BIGGER?
    private byte[] buf = new byte[256];
    HashMap<InetSocketAddress, ArrayList<String>> delivered = new HashMap<InetSocketAddress, ArrayList<String>>();

    public PerfectLinks(Host me, List<Config> configs, List<Host> hosts) {
        this.me = me;
        this.configs = configs;
        this.hosts = hosts;
        this.output = "";
        this.running = false;

        // Create socket
        InetSocketAddress address = new InetSocketAddress(me.getIp(), me.getPort());
        try {
            socket = new DatagramSocket(address);
            socket.setSoTimeout(1000); 
        } catch(SocketException e) {
            System.err.println("Cannot Create Socket: " + e);
        }
    }

    public boolean send(InetSocketAddress address, String message) {
        System.out.println("INSIDE SEND\n");
        buf = message.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, address);

        try {
            socket.send(packet);
            output = String.format("%sb %s\n", output, message);
            System.out.println("OUTPUT");
            System.out.printf("%s\n", output);
        } catch(IOException e) {
            System.err.println("Client.Send Error: " + e);
            return false;
        }
        
        return true;
    }

    /**
     * Send messages per configuration
     * Do not send messages to self
     */
    public void sendAll() {
        System.out.println("INSIDE SENDALL");

        // Loop through configs, get receiver address
        for (Config config: configs) {
            if (config.getId() != me.getId()) {
                Host receiver = getHostById(config.getId());
                InetSocketAddress address = new InetSocketAddress(receiver.getIp(), receiver.getPort());
    
                // Send number of messages
                int i = 1;
                while (i <= config.getM()) {
                    System.out.printf("Sending %d\n", i);
                    send(address, Integer.toString(i));
                    i++;
                }
            }
        }
    }

    // NOTE: start is used to run a thread asynchronously
    public void run() {
        System.out.println("INSIDE RUN");

        running = true;

        while (running) {
            // Receive Packet
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
            String message = new String(packet.getData(), 0, packet.getLength()).trim();

            // Add packet to delivered
            InetSocketAddress from = new InetSocketAddress(address, port);

            if (!isMessageInDelivered(from, message)) {
                // If message has not been delivered, deliver message
                System.out.printf("Received %s\n", message);
                int id = getHostByAddress(address, port).getId();
                output = String.format("%s d %s %s\n", output, Integer.toString(id), message);
            }
        }
    }

    public String close() {
        running = false;
        socket.close();
        return output;
    }

    private Host getHostById(int id) {
        for (Host host: hosts) {
            if (host.getId() == id) {
                return host;
            }
        }

        return null;
    }

    private Host getHostByAddress(InetAddress ip, int port) {
        for (Host host: hosts) {
            // Create Socket Address for host and ip/port
            InetSocketAddress host1 = new InetSocketAddress(host.getIp(), host.getPort());
            InetSocketAddress host2 = new InetSocketAddress(ip, port);

            if (host1.equals(host2)) {
                return host;
            }
        }

        return null;
    }

    /**
     * Checks if message in delivered
     * If not in delivered, adds to delivered
     * @param from
     * @param message
     * @return boolean
     */
    private boolean isMessageInDelivered(InetSocketAddress from, String message) {
        ArrayList<String> msgList = delivered.get(from);

        if(msgList == null) {
            // If no messages in delivered, create list
            msgList = new ArrayList<String>();
            msgList.add(message);
            delivered.put(from, msgList);
            return false;
        } else {
            // If messages in delivered, make sure not a duplicate
            if(!msgList.contains(message)) {
                msgList.add(message);
                return false;
            } 
        }

        return true;
    }
}
