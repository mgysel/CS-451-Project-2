package cs451;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerfectLinks extends Thread {
    private Host me;
    public List<Config> configs;
    public Hosts hosts;
    private String output;
    private MyEventListener listener;
    private Messages messages;
    private UDP udp;

    private boolean running;

    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();

    public PerfectLinks(Host me, List<Config> configs, Hosts hosts) {
        this.me = me;
        this.configs = configs;
        this.hosts = hosts;
        this.output = "";
        this.running = false;
        this.messages = new Messages(me, configs, hosts);
        this.udp = new UDP(me);
    }

    public boolean send(Host dest, Message m) {
        System.out.println("Inside send");
        InetSocketAddress address = dest.getAddress();
        String content = m.getContent();

        if (udp.send(address, content)) {
            // Update sent map
            if (m.getType() != MessageType.ACK) {
                messages.putMessageInMap(messages.getSent(), dest, m);
            }

            return true;
        }

        return false;
    }

    /**
     * Send messages per configuration
     * Do not send messages to self
     */
    public void sendAll() {
        
        // Send messages until we receive all acks
        boolean firstBroadcast = true;
        while (true) {
            // For Host in config that is not me
            for (Config config: configs) {
                Host peer = hosts.getHostById(config.getId());
                if (peer.getId() != me.getId()) {
                    // Send all messages
                    List<Message> msgList = messages.getMessages().get(peer);
                    if (msgList != null) {
                        for (Message m: msgList) {
                            send(peer, m);
                            writeBroadcast(m, firstBroadcast);
                        }
                    }
                    firstBroadcast = false;
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
            DatagramPacket packet = udp.receive();

            if (packet != null) {
                Host from = hosts.getHostByAddress(packet.getAddress(), packet.getPort());
                String message = new String(packet.getData(), packet.getOffset(),  packet.getLength()).trim();

                // System.out.println("***** Inside Receive");
                // System.out.printf("RECEIVED MESSAGE: %s\n", message);
                if (!message.contains("A/")) {
                    // System.out.println("About to deliver message");
                    Message m = new Message(MessageType.BROADCAST, message);
                    deliver(from, m);

                    // Send ack back, even if already delivered
                    // System.out.printf("This is what I am sending back: %s\n", String.format("ACK/%s", message));
                    Message ack = new Message(MessageType.ACK, message);
                    send(from, ack);
                } else {
                    // Process ACK
                    if (message.split("/").length == 2) {
                        if (!message.split("/")[1].equals("")) {
                            Message m = new Message(MessageType.BROADCAST, message.split("/")[1]);
                            messages.removeMessage(messages.getMessages(), from, m);
    
                        }
                    } else {
                        System.out.println("***** Not proper messages sent");
                        System.out.printf("Message: %s\n", message);
                    }
                }
            }
        }
    }

    public String close() {
        running = false;
        udp.socket.close();
        return output;
    }

    public void setMyEventListener (MyEventListener listener) {
        this.listener = listener;
    }

    private void deliver(Host src, Message m) {
        if (messages.putMessageInMap(messages.getDelivered(), src, m)) {
            deliver(src, m);
            writeDeliver(src, m);
            // listener.PerfectLinksDeliver(src, m);
        }
    }

    private void writeDeliver(Host p, Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, p.getId(), m.getContent());
        outputLock.writeLock().unlock();
    }

    private void writeBroadcast(Message m, Boolean firstBroadcast) {
        if (firstBroadcast) {
            outputLock.writeLock().lock();
            output = String.format("%sb %s\n", output, m.getContent());
            outputLock.writeLock().unlock();
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
}
