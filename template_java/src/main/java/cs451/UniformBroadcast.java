package cs451;

import java.net.DatagramPacket;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UniformBroadcast extends Thread implements MyEventListener {
    private PerfectLinks pl;
    private Host me;
    public Hosts hosts;
    public List<Config> configs;
    private Messages messages;
    private UDP udp;

    private String output;

    private boolean running;
    
    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();
    
    public UniformBroadcast(PerfectLinks pl) {
        this.pl = pl;
        this.pl.setMyEventListener(this);
        this.configs = pl.getConfigs();
        this.me = pl.getMe();
        this.hosts = pl.getHosts();
        this.messages = pl.getMessages();
        this.udp = pl.getUDP();
    }

    // Broadcast
    public void broadcast() {
        // Send all messages
        pl.sendAll();
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
                // System.out.println("***** Inside Receive");
                // System.out.printf("RECEIVED MESSAGE: %s\n", received);
                // System.out.printf("FORMATTED MESSAGE: %s\n", message.toString());
                // System.out.printf("TYPE: %s\n", message.getType());
                // System.out.printf("CONTENT: %s\n", message.getContent());
                
                if (message.getType() == MessageType.BROADCAST) {
                    // If we receive a broadcast message
                    // 1. Add broadcast to messages, unless we have an ack for everyone
                    // 2. Send ack back
                    // 3. Broadcast to all who we do not already have an ack for
                    deliver(from, message);
                    // Send ack back, even if already delivered
                    Message ack = new Message(MessageType.ACK, me, message.getContent());
                    pl.send(from, ack);
                } else if (message.getType() == MessageType.ACK) {
                    // Process ACK
                    Message removeMessage = new Message(MessageType.BROADCAST, me, message.getContent());
                    messages.removeMessage(messages.getMessages(), from, removeMessage);
                } else {
                    System.out.println("***** Not proper messages sent");
                    System.out.printf("Message: %s\n", received);
                }
            }
        }
    }


    // Return output
    public String close() {
        return pl.close();
    }

    @Override
    public void PerfectLinksDeliver(Host p, Message m) {
        // If we have acks for all peers, then deliver
        // deliver(p, m);
        // System.out.println("Caught the delivery");
    }

    private void deliver(Host src, Message m) {
        writeDeliver(src, m);
    }

    private void writeDeliver(Host src, Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, src.getId(), m.getContent());
        outputLock.writeLock().unlock();
    }

    @Override
    public void ReceivedAck(String m) {
        // If we have received all acks, then deliver message
        
    }
}
