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
        // System.out.println("Inside SendAll");
        
        // Send messages until we receive all acks
        boolean firstBroadcast = true;
        while (true) {
            ConcurrentHashMap<Host, ArrayList<Message>> messagesClone = messages.getMessagesClone();

            // For Host in config (including me)
            for (Host host: hosts.getHosts()) {
                // System.out.printf("Host: %s\n", host.getId());
                // System.out.printf("Host length: %d\n", hosts.getHosts().size());
                // Send all messages
                // List<Message> msgList = messages.getMessages().get(host);
                List<Message> msgList = messagesClone.get(host);
                if (msgList != null) {
                    // System.out.println("Message list is not null");
                    // System.out.printf("MstList: %s\n", msgList);
                    // System.out.printf("MsgList length: %d\n", msgList.size());
                    for (Message m: msgList) {
                        // System.out.println("***** Inside Iterator");
                        if (!m.getReceivedAck()) {
                            System.out.println("\n***** Sending message\n");
                            System.out.printf("Message: %s\n", m.toString());
                            System.out.printf("Received ack? %s\n", m.getReceivedAck());
                            pl.send(host, m);
                            output.writeBroadcast(m, firstBroadcast);
                        }
                    }
                } 
                firstBroadcast = false;
            }
        }
    }

    // NOTE: start is used to run a thread asynchronously
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

                    System.out.println("***** Inside Receive");
                    System.out.printf("Received: %s\n", received);
                    System.out.printf("From: %d\n", from.getId());
                    // System.out.println(received == null);
                    // System.out.printf("RECEIVED MESSAGE: %s\n", received);
                    // System.out.printf("FORMATTED MESSAGE: %s\n", message.toString());
                    // System.out.printf("TYPE: %s\n", message.getType());
                    // System.out.printf("CONTENT: %s\n", message.getContent());
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
                        // Process ACK
                        // Create Broadcast message from ACK
                        Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), message.getFrom(), message.getContent());
                        
                        // Put message in delivered, unless already in
                        // messages.putMessageInMap(messages.getDelivered(), from, m);
                        // Update ack in messages
                        Message ogM = messages.getOGMessage(from, m);

                        System.out.printf("BEFORE: Received ack? %s\n", ogM.getReceivedAck());
                        messages.updateAck(from, m);
                        System.out.printf("AFTER: Received ack? %s\n", ogM.getReceivedAck());

                        // If received ack from majority of hosts, deliver message
                        deliver(from, m);
                    } else {
                        System.out.println("***** Not proper messages sent");
                        System.out.printf("Message: %s\n", received);
                    }

                    // messages.printMap(messages.getMessages());
                }
            }
        }
    }


    // Return output
    public String close() {
        running = false;
        udp.socket.close();
        return output.getOutput();
    }

    private void deliver(Host src, Message m) {
        // System.out.println("\n***** Inside deliver");
        // messages.printMap(messages.getMessagesClone());
        
        if (messages.canDeliverMessage(m)) {
            ArrayList<Message> deliverList = messages.getDeliverMessages(m);
            Collections.sort(deliverList);

            for (Message message: deliverList) {
                output.writeDeliver(message);
            }
        }
    }
}
