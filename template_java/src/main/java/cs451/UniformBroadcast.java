package cs451;

import java.net.DatagramPacket;
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
            for (Config config: configs) {
                Host peer = hosts.getHostById(config.getId());
                // Send all messages
                List<Message> msgList = messages.getMessages().get(peer);
                if (msgList != null) {
                    for (Message m: msgList) {
                        if (m.getReceivedAck() == false) {
                            pl.send(peer, m);
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
                Message message = new Message(received, hosts);
                System.out.println("***** Inside Receive");
                System.out.printf("Received: %s\n", received);
                System.out.println(received == null);
                System.out.printf("RECEIVED MESSAGE: %s\n", received);
                System.out.printf("FORMATTED MESSAGE: %s\n", message.toString());
                System.out.printf("TYPE: %s\n", message.getType());
                System.out.printf("CONTENT: %s\n", message.getContent());
                if (message.getType() == MessageType.BROADCAST) {
                    // Add message to messages for each host, unless already in messages
                    messages.putMessagesInMap(message);

                    deliver(from, message);
                    // Send ack back, even if already delivered
                    Message ack = new Message(MessageType.ACK, me, message.getContent());
                    pl.send(from, ack);
                } else if (message.getType() == MessageType.ACK) {
                    // Process ACK
                    Message m = new Message(MessageType.BROADCAST, me, message.getContent());
                    messages.updateAck(from, m);
                } else {
                    System.out.println("***** Not proper messages sent");
                    System.out.printf("Message: %s\n", received);
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
        if (messages.putMessageInMap(messages.getDelivered(), src, m)) {
            deliver(src, m);
            output.writeDeliver(src, m);
            // listener.PerfectLinksDeliver(src, m);
        }
    }

    @Override
    public void ReceivedAck(String m) {
        // If we have received all acks, then deliver message
        
    }
}
