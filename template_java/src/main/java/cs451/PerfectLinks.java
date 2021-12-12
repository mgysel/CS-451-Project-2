package cs451;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerfectLinks extends Thread implements MyEventListener {
    private Host me;
    public List<Config> configs;
    public Hosts hosts;
    private String output;
    private UDP udp;
    static ConcurrentHashMap<Host, ArrayList<Message>> messages;
    static ConcurrentHashMap<Host, ArrayList<Message>> delivered;
    private MyEventListener listener; 

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
        InetSocketAddress address = dest.getAddress();
        String content = m.toString();

        // NOTE: For testing
        // try {
        //     TimeUnit.SECONDS.sleep(2);
        // } catch (InterruptedException ex) {
        //     System.out.printf("Sleep exception: %s\n", ex);
        // }

        // Send m
        if (udp.send(address, content)) {
            // If broadcast m, update messages map
            if (m.getType() == MessageType.BROADCAST) {
                addMessageToMap(dest, m, messages);
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
        if (isMessageInMap(src, m, delivered) == null) {
            writeDeliver(src, m);
            listener.PerfectLinksDeliver(src, m);
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
            deliver(from, message);
            // Send ack back, even if already delivered
            Message ack = new Message(MessageType.ACK, message.getSequenceNumber(), me, from, message.getContent());
            send(from, ack);
        } else if (message.getType() == MessageType.ACK) {
            // Process ACK - Remove from messages, add to delivered
            Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), me, from, message.getContent());
            removeMessageFromMap(from, m, messages);
            addMessageToMap(from, m, delivered);
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
            ConcurrentHashMap<Host, ArrayList<Message>> messagesClone = getMapClone(messages);

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

    /**
     * Adds message to host's messageList
     * @param h
     * @param m
     * @return
     */
    private boolean addMessageToMap(Host h, Message m, ConcurrentHashMap<Host, ArrayList<Message>> map) {
        readLock.lock();
        ArrayList<Message> msgList = map.get(h);

        // If h does not have any messages, create new message list
        if (msgList == null) {
            ArrayList<Message> newMsgList = new ArrayList<Message>();
            newMsgList.add(m);
            
            readLock.unlock();
            writeLock.lock();
            map.put(h, newMsgList);
            writeLock.unlock();
            return true;
        }
        
        // If h has msgList, add m
        int index = msgList.indexOf(m);
        if(index == -1) {
            readLock.unlock();
            writeLock.lock();
            msgList.add(m);
            writeLock.unlock();
            readLock.unlock();
            return true;
        }

        // If m already in h's msgList, return false
        readLock.unlock();
        return false;
    }

    private Message isMessageInMap(Host h, Message m, ConcurrentHashMap<Host, ArrayList<Message>> map) {
        Message msg = null;

        readLock.lock();
        ArrayList<Message> msgList = map.get(h);
        if (msgList != null) {
            int index = msgList.indexOf(m);
            if (index != -1) {
                msg = msgList.get(index);
                readLock.unlock();
                return msg;
            }
        }

        readLock.unlock();
        return msg;
    }

    private boolean removeMessageFromMap(Host h, Message m, ConcurrentHashMap<Host, ArrayList<Message>> map) {
        readLock.lock();
        ArrayList<Message> msgList = map.get(h);
        if (msgList != null) {
            int index = msgList.indexOf(m);
            if (index != -1) {
                readLock.unlock();
                writeLock.lock();
                msgList.remove(index);
                writeLock.unlock();
                return true;
            }
        }

        readLock.unlock();
        return false;
    }

    /**
     * Gets Clone of a Messages Map
     * @param map
     * @return
     */
    public ConcurrentHashMap<Host, ArrayList<Message>> getMapClone(ConcurrentHashMap<Host, ArrayList<Message>> map) {
        ConcurrentHashMap<Host, ArrayList<Message>> mapClone = new ConcurrentHashMap<Host, ArrayList<Message>>();

        readLock.lock();
        for (ConcurrentHashMap.Entry<Host, ArrayList<Message>> entry : map.entrySet()) {
            Host key = entry.getKey();

            ArrayList<Message> newValue = new ArrayList<Message>();
            for (Message msg: entry.getValue()) {
                Message clone = msg.getClone();
                newValue.add(clone);
            }
            mapClone.put(key, newValue);
        }
        readLock.unlock();

        return mapClone;
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

    public void setMyEventListener (MyEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void PerfectLinksDeliver(Host p, Message m) {
        // TODO Auto-generated method stub
        
    }
}
