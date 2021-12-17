package cs451;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PerfectLinks extends Thread implements MyEventListener {
    private Host me;
    public List<LCBConfig> configs;
    public Hosts hosts;
    private UDP udp;
    static ConcurrentHashMap<Host, ArrayList<Message>> messages;
    static ConcurrentHashMap<Host, ArrayList<Message>> delivered;
    private MyEventListener listener; 
    private boolean running;

    // ***** TODO - For Testing
    private int numDelivered = 0;
    private int numSent = 0;

    public PerfectLinks(BroadcastConfig bConfig) {
        this.me = bConfig.getMe();
        this.configs = bConfig.getConfigs();
        this.hosts = bConfig.getHosts();
        
        this.udp = new UDP(me, this);
        this.udp.start();
        this.running = false;

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
        // try {
        //     TimeUnit.SECONDS.sleep(1);
        // } catch (InterruptedException ex) {
        //     System.out.printf("Sleep exception: %s\n", ex);
        // }

        // Send m
        if (udp.send(address, content)) {
            // If broadcast m, update messages map
            if (m.getType() == MessageType.BROADCAST) {
                Messages.addMessageToMap(dest, m, PerfectLinks.messages);
            }
            // ***** TODO - For Testing
            numSent++;

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
        if (listener != null) {
            if (Messages.addMessageToMap(src, m, PerfectLinks.delivered)) {
                listener.plDeliver(src, m);
                // ***** For Testing
                numDelivered++;
            }
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
        // System.out.printf("\n***** PL RECEIVED - %s\n", message.toString());
        if (message != null && from != null) {
            if (message.getType() == MessageType.BROADCAST) {
                deliver(from, message);
                // Send ack back, even if already delivered
                Message ack = new Message(MessageType.ACK, message.getSequenceNumber(), message.getFrom(), message.getContent(), message.getVC());
                send(from, ack);
            } else if (message.getType() == MessageType.ACK) {
                // Process ACK - Remove from messages, add to delivered
                Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), message.getFrom(), message.getContent(), message.getVC());
                // Remove from messages
                Messages.removeMessageFromMap(from, m, PerfectLinks.messages);
                deliver(me, m);
            } else {
                System.out.println("***** Not proper messages sent");
                System.out.printf("Message: %s\n", received);
            }
        }
    }

    /**
    * Send all unacked packets
    */
    public void run() {
        running = true;

        // Send messages until we receive all acks
        while (running) {
            ConcurrentHashMap<Host, ArrayList<Message>> messagesClone = Messages.getMapClone(PerfectLinks.messages);

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
        running = false;
        udp.setRunning(false);
        udp.socket.close();

        // ***** For Testing
        System.out.printf("PL - numSent: %d\n", numSent);
        System.out.printf("PL - numDelivered: %d\n", numDelivered);
        System.out.printf("PL - length of Messages: %d\n", PerfectLinks.messages.size());
        System.out.printf("PL - length of Delivered: %d\n", PerfectLinks.delivered.size());
    }

    public List<LCBConfig> getConfigs() {
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
        // Nothing
    }
    
    @Override
    public void bebDeliver(Host p, Message m) {
        // Nothing
    }

    @Override
    public void ubDeliver(Host p, Message m) {
        // Nothing
    }
}
