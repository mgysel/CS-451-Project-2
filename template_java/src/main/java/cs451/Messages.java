package cs451;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Messages {
    static Host me;
    static List<Config> configs;
    static Hosts hosts;

    static ConcurrentHashMap<Host, ArrayList<Message>> delivered;
    static ConcurrentHashMap<Host, ArrayList<Message>> sent;
    static ConcurrentHashMap<Host, ArrayList<Message>> messages;
    static ConcurrentHashMap<Host, ArrayList<Message>> ack;
    private ConcurrentHashMap<Host, ArrayList<Message>> canDeliver;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    ReentrantLock lock = new ReentrantLock();
    
    public Messages(Host me, List<Config> configs, Hosts hosts) {
        Messages.configs = configs;
        Messages.hosts = hosts;
        Messages.me = me;

        Messages.delivered = new ConcurrentHashMap<Host, ArrayList<Message>>();
        Messages.sent = new ConcurrentHashMap<Host, ArrayList<Message>>();
        Messages.messages = new ConcurrentHashMap<Host, ArrayList<Message>>();
        Messages.ack = new ConcurrentHashMap<Host, ArrayList<Message>>();
        this.canDeliver = new ConcurrentHashMap<Host, ArrayList<Message>>();

        // Initialize delivered for each host
        for (Host host: hosts.getHosts()) {
            Messages.delivered.put(host, new ArrayList<Message>());
            Messages.sent.put(host, new ArrayList<Message>());
            Messages.messages.put(host, new ArrayList<Message>());
            Messages.ack.put(host, new ArrayList<Message>());
            this.canDeliver.put(host, new ArrayList<Message>());
        }

        for (Config config: configs) {
            Host receiver = hosts.getHostById(config.getId());

            // Add messages to messages map
            int i = 1;
            while (i <= config.getM()) {
                // Put each message in map
                Message message = new Message(MessageType.BROADCAST, i, me, Integer.toString(i));
                addMessage(receiver, message);
                i++;
            }
        }
    }

    /**
     * Checks if message in delivered
     * If not in delivered, adds to delivered
     * @param from
     * @param message
     * @return boolean
     */
    public boolean addMessage(Host from, Message m) {        
        // System.out.println("***** Inside addMessage");
        // System.out.printf("Host: %s\n", from.getId());
        // System.out.printf("Message: %s\n", m.toString());

        ArrayList<Message> msgList = getMessageList(from);

        // If messages in delivered, make sure not a duplicate
        if (doesListContainMessage(msgList, m)) {
            // System.out.println("* MsgList contains m");
            // System.out.printf("Message: %s\n", message.toString());
            // System.out.printf("M: %s\n", m.toString());
            return false;
        }

        writeLock.lock();
        msgList.add(m);
        writeLock.unlock();
        // System.out.println("* MsgList does not contain m");
        // printMap(messages);

        return true;
    }

    public ArrayList<Message> getMessageList(Host host) {
        readLock.lock();
        ArrayList<Message> msgList = messages.get(host);
        readLock.unlock();

        return msgList;
    }

    public boolean doesListContainMessage(List<Message> msgList, Message m) {
        readLock.lock();
        for (Message message: msgList) {
            if (message.equals(m)) {
                readLock.unlock();
                return true;
            }
        }
        readLock.unlock();
        return false;
    }

    /**
     * Puts copy of m in messages map
     * @param m
     */
    public void addMessages(Host from, Message m) {
        // System.out.println("***** Inside adMessages");
        // System.out.printf("Host: %s\n", from.getId());
        // System.out.printf("Message: %s\n", m.toString());
        // Put message in messages for each host (these are never from me)
        for (Host host: hosts.getHosts()) {
            // System.out.printf("Host: %s\n", host.getId());
            Message copy = m.getCopy();
            if (host.equals(me) || host.equals(from) || host.equals(m.getHost())) {
                // If host is me or from, update ack, as I do not need to send to myself
                copy.setReceivedAck(true);
            } 
            addMessage(host, copy);
        }
    }

    public Message getOGMessage(Host host, Message m) {
        Message equalM = null;
        
        readLock.lock();
        ArrayList<Message> msgList = messages.get(host);
        
        int index = msgList.indexOf(m);
        if(index != -1) {
            equalM = msgList.get(index);
        }

        readLock.unlock();
        return equalM;
    }

    public ArrayList<Message> getOGMessages(Message m) {
        ArrayList<Message> OGMessages = new ArrayList<Message>();
        
        for (Host host: hosts.getHosts()) {
            OGMessages.add(getOGMessage(host, m));
        }
        
        return OGMessages;
    }

    public boolean updateAck(Host from, Message message) {
        System.out.println("***** Inside updateAck");
        Message m = getOGMessage(from, message);
        if (m != null) {
            System.out.printf("updateAck: Before update: %s\n", m.getReceivedAck());
            writeLock.lock();
            m.setReceivedAck(true);
            writeLock.unlock();
            System.out.printf("updateAck: After update: %s\n", m.getReceivedAck());
            return true;
        } else {
            System.out.println("updateAck: Message is null");
        }

        return false;
    }

    public boolean updateDelivered(Message message) {
        // System.out.println("***** Inside updateDelivered");
        ArrayList<Message> messages = getOGMessages(message);

        for (Message m: messages) {
            writeLock.lock();
            m.setIsDelivered(true);
            writeLock.unlock();
        }

        readLock.lock();
        ArrayList<Message> deliverList = canDeliver.get(message.getHost());
        readLock.unlock();
        
        for (Message m: deliverList) {
            if (m.equals(message)) {
                writeLock.lock();
                m.setIsDelivered(true);
                writeLock.unlock();
            }
        }

        return true;
    }

    public boolean canDeliverMessage(Message message) {   
        // Get equal messages
        ArrayList<Message> messages = getOGMessages(message);

        boolean delivered = false;

        double numAcks = 0.0;
        double total = (double) hosts.getHosts().size();
        double majority = 0.0;

        readLock.lock();
        for (Message m: messages) {
            if (m.getIsDelivered()) {
                delivered = true;
            }
            if (m.getReceivedAck()) {
                numAcks += 1.0;
            }
        }
        readLock.unlock();

        // If majority acks, update acks for all, add to canDeliver
        majority = numAcks / total;
        if (majority > 0.5 && delivered == false) {
            // System.out.printf("Majority found for: %s\n", message.toString());
            // Update ACKs

            // Add message to canDeliver
            Message first = messages.get(0);
            if (first != null) {
                addToCanDeliver(messages.get(0));
            }

            return true;
        }

        return false;
    }

    public void addToCanDeliver(Message m) {
        readLock.lock();
        ArrayList<Message> msgList = canDeliver.get(m.getHost());
        int indexOf = msgList.indexOf(m);
        readLock.unlock();

        if(indexOf == -1) {
            writeLock.lock();
            msgList.add(m);
            writeLock.unlock();
        }
    }

    public ArrayList<Message> getDeliverMessages(Message m) {
        ArrayList<Message> deliverList = new ArrayList<Message>();
        
        readLock.lock();
        ArrayList<Message> msgList = canDeliver.get(m.getFrom());
        Collections.sort(msgList);
        readLock.unlock();

        int i = 1;
        int total = msgList.size();
        while(i <= total) {
            readLock.lock();
            Message thisMsg = msgList.get(i-1);
            int sequenceNumber = thisMsg.getSequenceNumber();
            boolean isDelivered = thisMsg.getIsDelivered();
            readLock.unlock();


            // System.out.printf("I: %d\n", i);
            // System.out.printf("thisMsg: %s\n", thisMsg.toString());
            if (sequenceNumber != i) {
                break;
            }
            if (!isDelivered) {
                // System.out.printf("Can deliver message: %s\n", m.toString());
                // System.out.printf("Hpst: %s\n", src);
                // System.out.printf("M: %s\n", thisMsg.toString());
                updateDelivered(thisMsg);
                deliverList.add(thisMsg.getCopy());
            }
            i++;
        }

        return deliverList;
    }

    public void printMap(HashMap<Host, ArrayList<Message>> map) {
        lock.lock();
        System.out.println("***** Print Map");
        for (Map.Entry<Host, ArrayList<Message>> entry : map.entrySet()) {
            Host host = entry.getKey();
            ArrayList<Message> hostDelivered = entry.getValue();

            System.out.printf("**PM Host: %d\n", host.getId());
            for (Message m: hostDelivered) {
                System.out.printf("PM Message: %s\n", m.toString());
                System.out.printf("PM Received Ack: %s\n", m.getReceivedAck());
                System.out.printf("PM Is Delivered: %s\n", m.getIsDelivered());
            }
        }
        lock.unlock();
    }

    public ConcurrentHashMap<Host, ArrayList<Message>> getSent() {
        return Messages.sent;
    }

    public ConcurrentHashMap<Host, ArrayList<Message>> getDelivered() {
        return Messages.delivered;
    }

    public ConcurrentHashMap<Host, ArrayList<Message>> getAck() {
        return Messages.ack;
    }

    public ConcurrentHashMap<Host, ArrayList<Message>> getMessages() {
        return Messages.messages;
    }

    public ConcurrentHashMap<Host, ArrayList<Message>> getCanDeliver() {
        return this.canDeliver;
    }

    public ConcurrentHashMap<Host, ArrayList<Message>> getMessagesClone() {
        ConcurrentHashMap<Host, ArrayList<Message>> messagesClone = new ConcurrentHashMap<Host, ArrayList<Message>>();

        readLock.lock();
        for (HashMap.Entry<Host, ArrayList<Message>> entry : messages.entrySet()) {
            Host key = entry.getKey();

            ArrayList<Message> newValue = new ArrayList<Message>();
            for (Message oldMsg: entry.getValue()) {
                Message newMsg = oldMsg.getCopy();
                newValue.add(newMsg);
            }
            messagesClone.put(key, newValue);
        }
        readLock.unlock();

        return messagesClone;
    }
}
