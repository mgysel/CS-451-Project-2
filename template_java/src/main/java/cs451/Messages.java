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
    static ConcurrentHashMap<String, ArrayList<Message>> messages;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    ReentrantLock lock = new ReentrantLock();
    
    public Messages(Host me, List<Config> configs, Hosts hosts) {
        Messages.configs = configs;
        Messages.hosts = hosts;
        Messages.me = me;
        Messages.messages = new ConcurrentHashMap<String, ArrayList<Message>>();

        // Initialize messages for each host
        for (Config config: configs) {
            Host to = hosts.getHostById(config.getId());
            // Add messages to messages map
            int i = 1;
            while (i <= config.getM()) {
                // Put each message in map
                Message message = new Message(MessageType.BROADCAST, i, me, to, Integer.toString(i));
                MessageWrapper messageWrapper = new MessageWrapper(message, false, false)
                addMessage(message);
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
    public boolean addMessage(Message m) {        
        readLock.lock();
        ArrayList<Message> msgList = messages.get(m.toString());
        readLock.unlock();

        // If messages in delivered, make sure not a duplicate
        if (doesListContainMessage(msgList, m)) {
            return false;
        }

        writeLock.lock();
        msgList.add(m);
        writeLock.unlock();

        return true;
    }

    // /**
    //  * Gets list of messages for host
    //  * @param host
    //  * @return msgList
    //  */
    // public ArrayList<Message> getMessageList(Host host) {
    //     readLock.lock();
    //     ArrayList<Message> msgList = messages.get(host);
    //     readLock.unlock();

    //     return msgList;
    // }

    /**
     * Checks if list contains message
     * @param msgList
     * @param m
     * @return boolean
     */
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
        // Put message in messages for each host (these are never from me)
        for (Host host: hosts.getHosts()) {
            Message clone = m.getClone();
            if (host.equals(me) || host.equals(from) || host.equals(m.getFrom())) {
                // If host is me or from, update ack, as I do not need to send to myself
                clone.setReceivedAck(true);
            } 
            addMessage(clone);
        }
    }

    /**
     * Gets original message object
     * @param host
     * @param m
     * @return msg
     */
    public Message getOGMessage(Message m) {
        Message OG = null;
        
        readLock.lock();
        ArrayList<Message> msgList = messages.get(m.toString());
        
        int index = msgList.indexOf(m);
        if(index != -1) {
            OG = msgList.get(index);
        }

        readLock.unlock();
        return OG;
    }

    // /**
    //  * Gets original messages
    //  * @param m
    //  * @return list of original messages
    //  */
    // public ArrayList<Message> getOGMessages(Message m) {
    //     ArrayList<Message> OGMessages = new ArrayList<Message>();
        
    //     for (Host host: hosts.getHosts()) {
    //         OGMessages.add(getOGMessage(m));
    //     }
        
    //     return OGMessages;
    // }

    /**
     * Updates ack from host for message
     * @param from
     * @param message
     * @return
     */
    public boolean updateAck(Message message) {
        Message m = getOGMessage(message);
        if (m != null) {
            writeLock.lock();
            m.setReceivedAck(true);
            writeLock.unlock();
            return true;
        } 

        return false;
    }

    /**
     * Updates message.isDelivered in messages
     * @param message
     * @return boolean
     */
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

    /**
     * Checks if message can be delivered
     * Message can be delivered if majority acks and not yet delivered
     * @param message
     * @return boolean
     */
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
            // Add message to canDeliver
            Message first = messages.get(0);
            if (first != null) {
                addToCanDeliver(messages.get(0));
            }

            return true;
        }

        return false;
    }

    /**
     * Adds message to canDeliver
     * Does not add if duplicate
     * @param m
     */
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

    /**
     * Gets messages to deliver from canDeliver
     * @param m
     * @return
     */
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
            if (sequenceNumber != i) {
                break;
            }
            if (!isDelivered) {
                updateDelivered(thisMsg);
                deliverList.add(thisMsg.getCopy());
            }
            i++;
        }

        return deliverList;
    }

    /**
     * Prints a map - useful for debugging
     * @param map
     */
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
