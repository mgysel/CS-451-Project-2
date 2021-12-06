package cs451;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerfectLinks extends Thread {
    private Host me;
    public List<Config> configs;
    public Hosts hosts;
    private String output;
    private Messages messages;
    private UDP udp;

    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();

    public PerfectLinks(Host me, List<Config> configs, Hosts hosts, Messages messages) {
        this.me = me;
        this.configs = configs;
        this.hosts = hosts;
        this.output = "";
        this.messages = messages;
        this.udp = new UDP(me, this);
    }

    public boolean send(Host dest, Message m) {
        InetSocketAddress address = dest.getAddress();
        String content = m.toString();

        // NOTE: For testing
        // try {
        //     TimeUnit.SECONDS.sleep(2);
        // } catch (InterruptedException ex) {
        //     System.out.printf("Sleep exception: %s\n", ex);
        // }
        
        if (udp.send(address, content)) {
            return true;
        }

        return false;
    }

    public void process(DatagramPacket packet) {
        Host from = hosts.getHostByAddress(packet.getAddress(), packet.getPort());
        String received = new String(packet.getData(), packet.getOffset(), packet.getLength()).trim();
        Message message = new Message(received, hosts, me);
        if (message.getType() == MessageType.BROADCAST) {
            deliver(from, message);
            // Send ack back, even if already delivered
            Message ack = new Message(MessageType.ACK, message.getSequenceNumber(), me, from, message.getContent(), false, false);
            send(from, ack);
        } else if (message.getType() == MessageType.ACK) {
            // Process ACK
            Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), me, from, message.getContent(), false, false);
            messages.updateAck(from, m);
        } else {
            System.out.println("***** Not proper messages sent");
            System.out.printf("Message: %s\n", received);
        }
    }


    // public void setMyEventListener (MyEventListener listener) {
    //     this.listener = listener;
    // }

    /**
     * If m is not delivered, deliver m
     * @param src
     * @param m
     */
    private void deliver(Host src, Message m) {
        // if (messages.putMessageInMap(messages.getDelivered(), src, m)) {
        //     deliver(src, m);
        //     writeDeliver(src, m);
        //     // listener.PerfectLinksDeliver(src, m);
        // } 
        deliver(src, m);
        // writeDeliver(src, m);
    }

    // private void writeDeliver(Host p, Message m) {
    //     outputLock.writeLock().lock();
    //     output = String.format("%sd %s %s\n", output, p.getId(), m.getContent());
    //     outputLock.writeLock().unlock();
    // }

    // private void writeBroadcast(Message m, Boolean firstBroadcast) {
    //     if (firstBroadcast) {
    //         outputLock.writeLock().lock();
    //         output = String.format("%sb %s\n", output, m.getContent());
    //         outputLock.writeLock().unlock();
    //     }
    // }

    /**
     * Close udp socket
     * @return output
     */
    public String close() {
        udp.setRunning(false);
        udp.socket.close();
        return output;
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

    public Messages getMessages() {
        return messages;
    }
}
