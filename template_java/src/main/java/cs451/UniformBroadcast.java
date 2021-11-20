package cs451;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

public class UniformBroadcast extends Thread implements MyEventListener {
    private PerfectLinks pl;
    private Host me;
    public Hosts hosts;
    public List<Config> configs;
    private Messages messages;
    private UDP udp;
    private Output output;

    private boolean running;
    
    public UniformBroadcast(PerfectLinks pl) {
        this.pl = pl;
        this.pl.setMyEventListener(this);
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
        System.out.println("Inside SendAll");
        
        // Send messages until we receive all acks
        boolean firstBroadcast = true;
        while (true) {
            // For Host in config (including me)
            for (Host host: hosts.getHosts()) {
                // System.out.printf("Host: %s\n", host.getId());
                // System.out.printf("Host length: %d\n", hosts.getHosts().size());
                // Send all messages
                List<Message> msgList = messages.getMessages().get(host);
                if (msgList != null) {
                    // System.out.println("Message list is not null");
                    // System.out.printf("MstList: %s\n", msgList);
                    // System.out.printf("MsgList length: %d\n", msgList.size());
                    for (Message m: msgList) {
                        if (m.getReceivedAck() == false) {
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
        System.out.println("INSIDE RUN");

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
                            messages.putMessagesInMap(from, message);
                        }

                        // If only two nodes, need to deliver here
                        deliver(from, message);
                        
                        // Send ack back, even if already delivered
                        Message ack = new Message(MessageType.ACK, message.getFrom(), message.getContent());
                        pl.send(from, ack);
                    } else if (message.getType() == MessageType.ACK) {
                        // Process ACK
                        // Create Broadcast message from ACK
                        Message m = new Message(MessageType.BROADCAST, message.getFrom(), message.getContent());
                        
                        // Put message in delivered, unless already in
                        messages.putMessageInMap(messages.getDelivered(), from, m);
                        // Update ack in messages
                        messages.updateAck(from, m);

                        // If received ack from all hosts, deliver message
                        deliver(from, m);
                    } else {
                        System.out.println("***** Not proper messages sent");
                        System.out.printf("Message: %s\n", received);
                    }
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

    @Override
    public void PerfectLinksDeliver(Host p, Message m) {
        // If we have acks for all peers, then deliver
        // deliver(p, m);
        // System.out.println("Caught the delivery");
    }

    private void deliver(Host src, Message m) {
        // deliver(src, m);
        
        if (messages.canDeliverMessage(m)) {
            System.out.println("***** Inside deliver");
            System.out.println("Can deliver message");
            System.out.println(m.toString());
            messages.deliverMessage(m);
            output.writeDeliver(m);
        } else {
            // System.out.println("Cannot deliver message");
        }
            // listener.PerfectLinksDeliver(src, m);
    }

    @Override
    public void ReceivedAck(String m) {
        // If we have received all acks, then deliver message
        
    }
}
