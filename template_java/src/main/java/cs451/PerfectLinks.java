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
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerfectLinks extends Thread {
    private Host me;
    public DatagramSocket socket;
    public List<Config> configs;
    public Hosts hosts;
    private List<Host> peers;
    private String output;
    private MyEventListener listener;
    private Messages messages;

    private boolean running;

    private byte[] inBuf = new byte[256];
    private byte[] outBuf = new byte[256];
    // HashMap<Integer, ArrayList<String>> delivered = new HashMap<Integer, ArrayList<String>>();
    // HashMap<Integer, ArrayList<String>> sent = new HashMap<Integer, ArrayList<String>>();
    // HashMap<Integer, ArrayList<String>> messages = new HashMap<Integer, ArrayList<String>>();
    // HashMap<Integer, ArrayList<String>> ack = new HashMap<Integer, ArrayList<String>>();

    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();

    public PerfectLinks(Host me, List<Config> configs, Hosts hosts) {
        this.me = me;
        this.configs = configs;
        this.hosts = hosts;
        this.output = "";
        this.running = false;
        this.peers = new ArrayList<Host>();
        this.messages = new Messages(configs);

        // Create socket
        InetSocketAddress address = new InetSocketAddress(me.getIp(), me.getPort());
        try {
            socket = new DatagramSocket(address);
            socket.setSoTimeout(1000); 
        } catch(SocketException e) {
            System.err.println("Cannot Create Socket: " + e);
        }

    }

    public boolean send(Host dest, Message m) {
        // System.out.println("Inside send");

        InetSocketAddress address = dest.getAddress();

        // Create output buffer
        outBuf = new byte[256];
        outBuf = m.getContent().getBytes();

        // Create packet
        DatagramPacket packet = new DatagramPacket(outBuf, 0, outBuf.length, address);

        try {
            // Send
            socket.send(packet);

            // Update sent map
            if (m.getType() != MessageType.ACK) {
                messages.putMessageInMap(messages.getSent(), dest, m);
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
        while (!messages.doesAckEqualMessages()) {
            // System.out.println("ACK does not equal messages");
            int i = 1;
            while (i <= maxMessages) {
                for (Config config: configs) {
                    Host receiver = Hosts.getHostById(config.getId());
                    // Host receiver = getHostById(config.getId());
                    // InetSocketAddress address = new InetSocketAddress(receiver.getIp(), receiver.getPort());
                    if (i <= config.getM()) {
                        Message message = new Message(MessageType.BROADCAST, Integer.toString(i));
                        if (!messages.isMessageInMap(messages.getAck(), receiver, message)) {
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
    
                Host from = hosts.getHostByAddress(address, port);
                // System.out.printf("RECEIVED MESSAGE: %s\n", message);
                if (!message.contains("ACK/")) {
                    // System.out.println("About to deliver message");
                    Message m = new Message(MessageType.BROADCAST, message);
                    deliver(from, m);

                    // Send ack back, even if already delivered
                    // System.out.printf("This is what I am sending back: %s\n", String.format("ACK/%s", message));
                    Message ack = new Message(MessageType.ACK, message);
                    send(from, ack);
                } else {
                    // Process ACK
                    if (message.split("/").length > 1) {
                        if (!message.split("/")[1].equals("")) {
                            // System.out.printf("Message Length: %s\n", message.split("/").length);
                            // System.out.printf("This is what I am putting in ACK: %s\n", message.split("/")[1]);
                            Message m = new Message(MessageType.ACK, message.split("/")[1]);
                            messages.putMessageInMap(messages.getAck(), from, m);
                            // Trigger receive ACK event
                            // listener.ReceivedAck(m);

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

    // /**
    //  * Checks if message in delivered
    //  * If not in delivered, adds to delivered
    //  * @param from
    //  * @param message
    //  * @return boolean
    //  */
    // private boolean putMessageInMap(HashMap<Integer, ArrayList<String>> map, Integer from, String message) {
    //     if (message.equals("")) {
    //         return false;
    //     }
        
    //     ArrayList<String> msgList = map.get(from);

    //     if(msgList == null) {
    //         // If no messages in delivered, create list
    //         msgList = new ArrayList<String>();
    //         msgList.add(message);
    //         map.put(from, msgList);
    //         return true;
    //     } else {
    //         // If messages in delivered, make sure not a duplicate
    //         if(!msgList.contains(message)) {
    //             msgList.add(message);
    //             return true;
    //         } 
    //     }

    //     return false;
    // }

    // private boolean isMessageInMap(HashMap<Integer, ArrayList<String>> map, Integer from, String message) {
    //     ArrayList<String> msgList = map.get(from);

    //     if(msgList == null) {
    //         // If no messages, not in list
    //         return false;
    //     } else {
    //         // If messages in delivered, make sure not a duplicate
    //         if(msgList.contains(message)) {
    //             return true;
    //         } 
    //     }

    //     return false;
    // }

    // private boolean doesAckEqualMessages(HashMap<Integer, ArrayList<String>> ack, HashMap<Integer, ArrayList<String>> messages) {
    //     // Loop through configs, get receiver address
    //     for (Config config: configs) {
    //         if (config.getId() != me.getId()) {
    //             int receiver = config.getId();

    //             ArrayList<String> ackList = ack.get(receiver);
    //             ArrayList<String> messageList = messages.get(receiver);

    //             if(ackList == null) {
    //                 // If no messages, not in list
    //                 return false;
    //             } else {
    //                 for (String message: messageList) {
    //                     if (!ackList.contains(message)) {
    //                         return false;
    //                     }
    //                 }
    //             }
    //         }
    //     }

    //     return true;
                
    // }

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

    private void deliver(Host src, Message m) {
        if (messages.putMessageInMap(messages.getDelivered(), src, m)) {
            // If message has not been delivered, deliver message
            deliver(src, m);
            writeDeliver(src, m);
            listener.PerfectLinksDeliver(src, m);
        }
    }

    public List<Config> getConfigs() {
        return configs;
    }

    public Hosts getHosts() {
        return hosts;
    }

    public Host getMe() {
        return me;
    }

    public List<Host> getPeers() {
        return peers;
    }

    private void writeDeliver(Host p, Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, p.getId(), m.getContent());
        outputLock.writeLock().unlock();
    }

    private void writeBroadcast(Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sb %s\n", output, m.getContent());
        outputLock.writeLock().unlock();
    }
}
