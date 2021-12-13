package cs451;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PerfectLinks extends Thread implements MyEventListener {
    private Host me;
    public List<Config> configs;
    public Hosts hosts;
    private UDP udp;
    static ConcurrentHashMap<Host, ArrayList<Message>> messages;
    static ConcurrentHashMap<Host, ArrayList<Message>> delivered;
    private MyEventListener listener; 

    public PerfectLinks(BroadcastConfig bConfig) {
        this.me = bConfig.getMe();
        this.configs = bConfig.getConfigs();
        this.hosts = bConfig.getHosts();
        
        this.udp = new UDP(me, this);
        this.udp.start();

        PerfectLinks.messages = new ConcurrentHashMap<Host, ArrayList<Message>>();
        PerfectLinks.delivered = new ConcurrentHashMap<Host, ArrayList<Message>>();
    }

    /**
     * Send message to dest
     * @param dest
     * @param m
     * @return
     */
    public boolean send(Host dest, Message m) {
        // System.out.printf("Inside send: %s\n", m.toString());
        
        InetSocketAddress address = dest.getAddress();
        String content = m.toString();

        // NOTE: For testing
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ex) {
            System.out.printf("Sleep exception: %s\n", ex);
        }

        // Send m
        if (udp.send(address, content)) {
            // If broadcast m, update messages map
            if (m.getType() == MessageType.BROADCAST) {
                Messages.addMessageToMap(dest, m, messages);
            }
            return true;
        }

        return false;
    }

    /**
     * If m is not delivered, deliver m
     * @param src
     * @param m
     */
    private void deliver(Host src, Message m) {
        if (Messages.addMessageToMap(src, m, delivered)) {
            Messages.printMap(delivered);
            System.out.printf("");
            listener.plDeliver(src, m);
        }
    }

    /**
     * Process messages received from UDP
     * @param packet
     */
    public void process(DatagramPacket packet) {
        // Process packet
        Host from = hosts.getHostByAddress(packet.getAddress(), packet.getPort());
        String received = new String(packet.getData(), packet.getOffset(), packet.getLength()).trim();
        Message message = new Message(received, hosts, me);
        if (message.getType() == MessageType.BROADCAST) {
            deliver(message.getFrom(), message);
            // Send ack back, even if already delivered
            Message ack = new Message(MessageType.ACK, message.getSequenceNumber(), message.getFrom(), message.getContent());
            send(from, ack);
        } else if (message.getType() == MessageType.ACK) {
            // Process ACK - Remove from messages, add to delivered
            Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), message.getFrom(), message.getContent());
            Messages.removeMessageFromMap(m.getFrom(), m, messages);
            Messages.addMessageToMap(m.getFrom(), m, delivered);
        } else {
            System.out.println("***** Not proper messages sent");
            System.out.printf("Message: %s\n", received);
        }
    }

    /**
    * Send all unacked packets
    */
    public void run() {
        // Send messages until we receive all acks
        while (true) {
            ConcurrentHashMap<Host, ArrayList<Message>> messagesClone = Messages.getMapClone(messages);

            // For Host in config (including me)
            for (Host host: hosts.getHosts()) {
                // Send each message have not received an ack for
                List<Message> msgList = messagesClone.get(host);
                if (msgList != null) {
                    for (Message m: msgList) {
                        send(host, m);
                    }
                } 
            }
        }
    }

    /**
     * Close udp socket
     * @return output
     */
    public void close() {
        udp.setRunning(false);
        udp.socket.close();
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

    public UDP getUDP() {
        return udp;
    }

    public void setMyEventListener (MyEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void plDeliver(Host src, Message m) {
        // deliver(src, m);
        // System.out.println("Caught the delivery");
    }
    
    @Override
    public void bebDeliver(Host p, Message m) {
        // TODO Auto-generated method stub
        
    }
}
