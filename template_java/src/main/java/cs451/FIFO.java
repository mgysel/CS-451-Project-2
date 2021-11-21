package cs451;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class FIFO extends Thread {
    private PerfectLinks pl;
    private Host me;
    public Hosts hosts;
    public List<Config> configs;
    private Messages messages;
    private UDP udp;
    private Output output;

    private boolean running;

    ReentrantLock lock = new ReentrantLock();
    
    public FIFO(PerfectLinks pl) {
        this.pl = pl;
        this.configs = pl.getConfigs();
        this.me = pl.getMe();
        this.hosts = pl.getHosts();
        this.messages = pl.getMessages();
        this.udp = pl.getUDP();
        this.output = new Output();
    }

    /**
     * Send messages per configuration
     * Do not send messages to self
     */
    public void broadcast() {
        // System.out.println("Inside Broadcast");
        
        // Send messages until we receive all acks
        boolean firstBroadcast = true;
        while (true) {
            ConcurrentHashMap<Host, ArrayList<Message>> messagesClone = messages.getMessagesClone();

            // For Host in config (including me)
            for (Host host: hosts.getHosts()) {
                // Send each message have not received an ack for
                List<Message> msgList = messagesClone.get(host);
                if (msgList != null) {
                    for (Message m: msgList) {
                        if (!m.getReceivedAck()) {
                            pl.send(host, m);
                            output.writeBroadcast(m, firstBroadcast);
                        }
                    }
                } 
                firstBroadcast = false;
            }
        }
    }

    /**
     * Receive packets, process
     */
    public void run() {
        // System.out.println("INSIDE RUN");

        running = true;
        while (running) {
            // Receive Packet
            DatagramPacket packet = udp.receive();

            if (packet != null) {
                Host from = hosts.getHostByAddress(packet.getAddress(), packet.getPort());
                String received = new String(packet.getData(), packet.getOffset(),  packet.getLength()).trim();
                
                if (Message.isValidMessage(received)) {
                    Message message = new Message(received, hosts);

                    // System.out.println("***** Inside Receive");
                    // System.out.printf("Received: %s\n", received);
                    // System.out.printf("From: %d\n", from.getId());
                    if (message.getType() == MessageType.BROADCAST) {
                        // If Broadcast from someone else, put in messages
                        if (!from.equals(me)) {
                            // System.out.println("Putting messages in map");
                            messages.addMessages(from, message);
                            // messages.printMap(messages.getMessages());
                        }

                        // If only two nodes, need to deliver here
                        deliver(from, message);
                        
                        // Send ack back, even if already delivered
                        Message ack = new Message(MessageType.ACK, message.getSequenceNumber(), message.getFrom(), message.getContent());
                        pl.send(from, ack);
                        if (!from.equals(ack.getFrom())) {
                            pl.send(ack.getFrom(), ack);
                        }
                    } else if (message.getType() == MessageType.ACK) {
                        // Create Broadcast message from ACK
                        Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), message.getFrom(), message.getContent());
                        
                        // Update ack in messages
                        messages.updateAck(from, m);

                        // If received ack from majority of hosts, deliver message
                        deliver(from, m);
                    } else {
                        System.out.println("***** Not proper messages sent");
                        System.out.printf("Message: %s\n", received);
                    }
                }
            }
        }
    }

    /**
     * Deliver message
     * @param src
     * @param m
     */
    private void deliver(Host src, Message m) {
        if (messages.canDeliverMessage(m)) {
            ArrayList<Message> deliverList = messages.getDeliverMessages(m);
            Collections.sort(deliverList);

            for (Message message: deliverList) {
                output.writeDeliver(message);
            }
        }
    }

    // Close socket, Return output
    public String close() {
        running = false;
        udp.socket.close();
        return output.getOutput();
    }
}
