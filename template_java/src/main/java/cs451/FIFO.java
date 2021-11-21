package cs451;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;
import java.util.Iterator;

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

    // // Broadcast
    // public void broadcast() {
    //     // Send all messages
    //     pl.sendAll();
    // }

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
                        System.out.println("***** Inside Iterator");
                        if (m.getReceivedAck() == false) {
                            // System.out.println("\n***** Sending message\n");
                            pl.send(host, m);
                            output.writeBroadcast(m, firstBroadcast);
                        }
                    }


                    // Iterator<Message> iterator = msgList.iterator(); while(iterator.hasNext()) { 
                    //     System.out.println("***** Inside Iterator");
                    //     System.out.printf("Iterator: %s\n", iterator.toString());
                    //     System.out.printf("Next: %s\n", iterator.next().toString());
                    //     Message m = iterator.next();
                    //     if (iterator.next().getReceivedAck() == false) {
                    //         // System.out.println("\n***** Sending message\n");
                    //         pl.send(host, m);
                    //         output.writeBroadcast(m, firstBroadcast);
                    //     }
                    // }
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

                    // System.out.println("***** Inside Receive");
                    // System.out.printf("Received: %s\n", received);
                    // System.out.println(received == null);
                    // System.out.printf("From: %d\n", from.getId());
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
                    } else if (message.getType() == MessageType.ACK) {
                        // Process ACK
                        // Create Broadcast message from ACK
                        Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), message.getFrom(), message.getContent());
                        
                        // Put message in delivered, unless already in
                        // messages.putMessageInMap(messages.getDelivered(), from, m);
                        // Update ack in messages
                        messages.updateAck(from, m);

                        // If received ack from all hosts, deliver message
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
            // System.out.printf("Can deliver message: %s\n", m);
            // Deliver all messages in canDeliver
            ArrayList<Message> msgList = messages.getCanDeliver().get(m.getFrom());
            Collections.sort(msgList);
            
            int i = 1;
            int total = msgList.size();
            while(i <= total) {
                Message thisMsg = msgList.get(i-1);
                // System.out.printf("I: %d\n", i);
                // System.out.printf("thisMsg: %s\n", thisMsg.toString());
                if (thisMsg.getSequenceNumber() != i) {
                    break;
                }
                if (!thisMsg.getIsDelivered()) {
                    // System.out.printf("Can deliver message: %s\n", m.toString());
                    // System.out.printf("Hpst: %s\n", src);
                    // System.out.printf("M: %s\n", thisMsg.toString());
                    messages.updateDelivered(thisMsg);
                    output.writeDeliver(thisMsg);
                }
                i++;
            }
        }
    }
}
