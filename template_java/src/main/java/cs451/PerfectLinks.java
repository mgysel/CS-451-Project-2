package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerfectLinks extends Thread {
    private Host me;
    private DatagramSocket socket;
    private List<Config> configs;
    private List<Host> hosts;
    private List<Host> peers;
    private String output;
    private MyEventListener listener;

    private boolean running;

    private byte[] inBuf = new byte[256];
    private byte[] outBuf = new byte[256];
    HashMap<Integer, ArrayList<String>> delivered = new HashMap<Integer, ArrayList<String>>();
    HashMap<Integer, ArrayList<String>> sent = new HashMap<Integer, ArrayList<String>>();
    HashMap<Integer, ArrayList<String>> messages = new HashMap<Integer, ArrayList<String>>();
    HashMap<Integer, ArrayList<String>> ack = new HashMap<Integer, ArrayList<String>>();

    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();

    public PerfectLinks(Host me, List<Config> configs, List<Host> hosts) {
        this.me = me;
        this.configs = configs;
        this.hosts = hosts;
        this.output = "";
        this.running = false;
        this.peers = new ArrayList<Host>();

        // Create socket
        InetSocketAddress address = new InetSocketAddress(me.getIp(), me.getPort());
        try {
            socket = new DatagramSocket(address);
            socket.setSoTimeout(1000); 
        } catch(SocketException e) {
            System.err.println("Cannot Create Socket: " + e);
        }

        // Initialize messages with messages to send
        for (Config config: configs) {
            if (config.getId() != me.getId()) {
                int receiver = config.getId();

                // Add messages to messages map
                int i = 1;
                while (i <= config.getM()) {
                    // Put each message in map
                    String message = Integer.toString(i);
                    putMessageInMap(messages, receiver, message);
                    i++;
                }
            }
        }

    }

    public boolean send(int dest, String m) {
        // System.out.println("Inside send");

        InetSocketAddress address = getAddressById(dest);

        // Create output buffer
        outBuf = new byte[256];
        outBuf = m.getBytes();

        // Create packet
        DatagramPacket packet = new DatagramPacket(outBuf, 0, outBuf.length, address);

        try {
            // Send
            socket.send(packet);

            // Update sent map
            if (!m.contains("ACK/")) {
                putMessageInMap(sent, dest, m);
            }
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
        
        // Send messages until we receive all acks
        boolean firstBroadcastRound = true;
        int maxMessages = getMaxMessages(configs);
        Boolean[] firstBroadcastI = new Boolean[maxMessages + 1];
        Arrays.fill(firstBroadcastI, Boolean.TRUE);
        while (!doesAckEqualMessages(ack, messages)) {
            // System.out.println("ACK does not equal messages");
            int i = 1;
            while (i <= maxMessages) {
                for (Config config: configs) {
                    int receiver = config.getId();
                    // Host receiver = getHostById(config.getId());
                    // InetSocketAddress address = new InetSocketAddress(receiver.getIp(), receiver.getPort());
                    if (config.getId() != me.getId() && i <= config.getM()) {
                        String message = Integer.toString(i);
                        if (!isMessageInMap(ack, receiver, message)) {
                            // System.out.println("INSIDE SENDALL");
                            // System.out.printf("Sending %s\n", message);
                            if (firstBroadcastI[i] && firstBroadcastRound) {
                                writeBroadcast(message);
                                firstBroadcastI[i] = false;
                            }
                            send(receiver, message);
                        }
                        
                    }
                }
                
                i++;
            }
            firstBroadcastRound = false;
        }
        System.out.println("Ack equals sent");
    }

    // NOTE: start is used to run a thread asynchronously
    public void run() {
        System.out.println("INSIDE RUN");

        running = true;

        while (running) {
            // Receive Packet
            DatagramPacket packet = new DatagramPacket(inBuf, inBuf.length);
            try {
                packet.setLength(inBuf.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(inBuf, inBuf.length, address, port);
                
                String message = new String(packet.getData(), packet.getOffset(),  packet.getLength()).trim();
                // System.out.printf("PACKET LENGTH: %s\n", packet.getLength());
                // System.out.printf("MESSAGE LENGTH: %s\n", message.length());
                // Clear buffer after processing it
    
                int id = getHostByAddress(address, port).getId();
                // System.out.printf("RECEIVED MESSAGE: %s\n", message);
                if (!message.contains("ACK/")) {
                    // System.out.println("About to deliver message");
                    deliver(id, message);

                    // Send ack back, even if already delivered
                    // System.out.printf("This is what I am sending back: %s\n", String.format("ACK/%s", message));
                    send(id, String.format("ACK/%s", message));
                } else {
                    // Process ACK
                    if (message.split("/").length > 1) {
                        if (!message.split("/")[1].equals("")) {
                            // System.out.printf("Message Length: %s\n", message.split("/").length);
                            // System.out.printf("This is what I am putting in ACK: %s\n", message.split("/")[1]);
                            putMessageInMap(ack, id, message.split("/")[1]);
                        }
                    }
                }
                inBuf = new byte[256];
                // Arrays.fill(inBuf,(byte)0);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                System.err.println("Server Cannot Receive Packet: " + e);
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

    private InetSocketAddress getAddressById(int id) {
        Host host = getHostById(id);
        InetSocketAddress address = new InetSocketAddress(host.getIp(), host.getPort());
        return address;
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
    private boolean putMessageInMap(HashMap<Integer, ArrayList<String>> map, Integer from, String message) {
        if (message.equals("")) {
            return false;
        }
        
        ArrayList<String> msgList = map.get(from);

        if(msgList == null) {
            // If no messages in delivered, create list
            msgList = new ArrayList<String>();
            msgList.add(message);
            map.put(from, msgList);
            return true;
        } else {
            // If messages in delivered, make sure not a duplicate
            if(!msgList.contains(message)) {
                msgList.add(message);
                return true;
            } 
        }

        return false;
    }

    private boolean isMessageInMap(HashMap<Integer, ArrayList<String>> map, Integer from, String message) {
        ArrayList<String> msgList = map.get(from);

        if(msgList == null) {
            // If no messages, not in list
            return false;
        } else {
            // If messages in delivered, make sure not a duplicate
            if(msgList.contains(message)) {
                return true;
            } 
        }

        return false;
    }

    private boolean doesAckEqualMessages(HashMap<Integer, ArrayList<String>> ack, HashMap<Integer, ArrayList<String>> messages) {
        // Loop through configs, get receiver address
        for (Config config: configs) {
            if (config.getId() != me.getId()) {
                int receiver = config.getId();

                ArrayList<String> ackList = ack.get(receiver);
                ArrayList<String> messageList = messages.get(receiver);

                if(ackList == null) {
                    // If no messages, not in list
                    return false;
                } else {
                    for (String message: messageList) {
                        if (!ackList.contains(message)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
                
    }

    private int getMaxMessages(List<Config> configs) {
        int maxMessages = 0;
        for (Config config: configs) {
            int configMessages = config.getM();
            if (config.getM() > maxMessages) {
                maxMessages = configMessages;
            }
        }
        return maxMessages;
    }

    public void setMyEventListener (MyEventListener listener) {
        this.listener = listener;
    }

    private void deliver(int src, String m) {
        if (putMessageInMap(delivered, src, m)) {
            // If message has not been delivered, deliver message
            deliver(src, m);
            writeDeliver(src, m);
            listener.PerfectLinksDeliver(src, m);
        }
    }

    public List<Config> getConfigs() {
        return configs;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public Host getMe() {
        return me;
    }

    public List<Host> getPeers() {
        return peers;
    }

    private void writeDeliver(int p, String m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, p, m);
        outputLock.writeLock().unlock();
    }

    private void writeBroadcast(String m) {
        outputLock.writeLock().lock();
        output = String.format("%sb %s\n", output, m);
        outputLock.writeLock().unlock();
    }
}
