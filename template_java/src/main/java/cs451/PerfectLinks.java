package cs451;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerfectLinks extends Thread {
    private Host me;
    public List<Config> configs;
    public Hosts hosts;
    private String output;
    private UDP udp;
    private ConcurrentHashMap<String, ArrayList<MessageWrapper>> delivered;

    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    public PerfectLinks(Host me, List<Config> configs, Hosts hosts) {
        this.me = me;
        this.configs = configs;
        this.hosts = hosts;
        this.output = "";
        this.udp = new UDP(me, this);
        this.delivered = new ConcurrentHashMap<String, ArrayList<MessageWrapper>>();
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
        // Process packet
        Host from = hosts.getHostByAddress(packet.getAddress(), packet.getPort());
        String received = new String(packet.getData(), packet.getOffset(), packet.getLength()).trim();
        Message message = new Message(received, hosts, me);
        if (message.getType() == MessageType.BROADCAST) {
            deliver(from, message);
            // Send ack back, even if already delivered
            Message ack = new Message(MessageType.ACK, message.getSequenceNumber(), me, from, message.getContent());
            send(from, ack);
        } else if (message.getType() == MessageType.ACK) {
            // Process ACK
            Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), me, from, message.getContent());
            updateAck(m);
        } else {
            System.out.println("***** Not proper messages sent");
            System.out.printf("Message: %s\n", received);
        }
    }


    public void setMyEventListener (MyEventListener listener) {
        this.listener = listener;
    }

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
        if (updateDelivered(m)) {
            deliver(src, m);
            writeDeliver(src, m);
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

    private boolean updateAck(Message m) {
        MessageWrapper mw = getMessageWrapper(m);
        if (mw != null) {
            if (mw.getAck() == false) {
                mw.setAck(true);
                return true;
            }
        }
        return false;
    }

    private boolean updateDelivered(Message m) {
        MessageWrapper mw = getMessageWrapper(m);
        if (mw != null) {
            if (mw.getDelivered() == false) {
                mw.setDelivered(true);
                return true;
            }
        }
        return false;
    }

    private MessageWrapper getMessageWrapper(Message m) {
        MessageWrapper mw = null;
        
        readLock.lock();
        ArrayList<MessageWrapper> mwList = delivered.get(m.toString());
        for (MessageWrapper thisMW: mwList) {
            if (thisMW.getMessage().equals(m)) {
                mw = thisMW;
                break;
            }
        }
        readLock.unlock();

        return mw;
    }

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
}
