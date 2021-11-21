package cs451;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

        // Initialize messages with messages to send
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

        // Initialize delivered for each host
        for (Host host: hosts.getHosts()) {
            Messages.delivered.put(host, new ArrayList<Message>());
            this.canDeliver.put(host, new ArrayList<Message>());
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


        ArrayList<Message> msgList = messages.get(from);

        // System.out.println("* About to check msgList");
        if(msgList == null) {
            // System.out.println("* MsgList is null");
            // If no messages in delivered, create list
            msgList = new ArrayList<Message>();
            msgList.add(m);

            writeLock.lock();
            messages.put(from, msgList);
            writeLock.unlock();

            return true;
        } else {
            // If messages in delivered, make sure not a duplicate
            for (Message message: msgList) {
                if (message.equals(m)) {
                    // System.out.println("* MsgList contains m");
                    // System.out.printf("Message: %s\n", message.toString());
                    // System.out.printf("M: %s\n", m.toString());
                    return false;
                }
            }
            writeLock.lock();
            msgList.add(m);
            writeLock.unlock();
            // System.out.println("* MsgList does not contain m");
            // printMap(messages);

            return true;
        }
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
            if (host.equals(me) || host.equals(from)) {
                // If host is me or from, update ack, as I do not need to send to myself
                copy.setReceivedAck(true);
            } 
            addMessage(host, copy);
        }
    }

    public boolean isMessageInMap(HashMap<Host, ArrayList<Message>> map, Host from, Message message) {
        ArrayList<Message> msgList = map.get(from);

        if(msgList == null) {
            // If no messages, not in list
            return false;
        } else {
            // If messages in delivered, make sure not a duplicate
            if(msgList.contains(message)) {
                // System.out.println("Message is in map");
                return true;
            } 
        }

        return false;
    }

    public boolean removeMessage(HashMap<Host, ArrayList<Message>> map, Host from, Message message) {
        ArrayList<Message> msgList = map.get(from);
        Message remove = null;

        if (msgList == null) {
            return false;
        } else {
            for (Message m: msgList) {
                if (m.equals(message)) {
                    remove = m;
                    break;
                }
            }
        }

        if (remove != null) {
            msgList.remove(remove);
            return true;
        } 
        return false;
    }

    public boolean doesAckEqualMessages() {
        // Loop through configs, get receiver address
        for (Config config: configs) {
            Host receiver = hosts.getHostById(config.getId());

            ArrayList<Message> ackList = ack.get(receiver);
            ArrayList<Message> messageList = messages.get(receiver);

            if(ackList == null) {
                // If no messages, not in list
                // System.out.println("Ack does not equal messages");
                return false;
            } else {
                for (Message message: messageList) {
                    if (!ackList.contains(message)) {
                        // System.out.println("Ack does not equal messages");
                        return false;
                    }
                }
            }
        }

        // System.out.println("Ack equals messages");
        return true;  
    }

    public boolean updateAck(Host from, Message message) {
        // System.out.println("***** Inside updateAck");
        ArrayList<Message> msgList = messages.get(from);

        if (msgList == null) {
            return false;
        } else {
            for (Message m: msgList) {
                if (m.equals(message)) {
                    // System.out.println("Updating received ack");
                    // System.out.println(message.toString());
                    m.setReceivedAck(true);
                    // System.out.printf("ReceivedAck: %s\n", m.getReceivedAck());
                    break;
                }
            }
        }

        return true;
    }

    public boolean updateDelivered(Message message) {
        // System.out.println("***** Inside updateDelivered");

        for (Host h: hosts.getHosts()) {
            ArrayList<Message> msgList = messages.get(h);

            if (msgList == null) {
                break;
            } else {
                for (Message m: msgList) {
                    if (m.equals(message)) {
                        // System.out.println("Updating delivered");
                        // System.out.printf("Message: %s\n", message.toString());
                        // System.out.printf("M: %s\n", m.toString());
                        m.setIsDelivered(true);
                        // System.out.printf("Delivered: %s\n", m.getIsDelivered());
                        // printMap(delivered);
                    }
                }
            }
        }

        ArrayList<Message> deliverList = canDeliver.get(message.getHost());
        for (Message m: deliverList) {
            if (m.equals(message)) {
                m.setIsDelivered(true);
            }
        }

        return true;
    }







    /**
     * Checks if received Ack for message from majority of hosts
     * @param message
     * @return
     */
    public boolean canDeliverMessage(Message message) {   
        // System.out.println("Inside receivedMajority");
        // System.out.printf("message: %s\n", message.toString());

        // System.out.println("***** Inside canDeliverMessage");     
        // HashMap<Host, ArrayList<Message>> messagesClone = getMessagesClone();
        double numAcks = 0.0;
        double total = (double) hosts.getHosts().size();
        double majority = 0.0;

        // If each host does not have
        boolean isDelivered = false;
        ArrayList<Message> equalMessages = new ArrayList<Message>();






        // HashMap<Host, ArrayList<Message>> messagesClone = new HashMap<Host, ArrayList<Message>>();

        // lock.lock();
        // Set<Map.Entry<Host, ArrayList<Message>>> entrySet = messages.entrySet();

        // // Collection Iterator
        // Iterator<Entry<Host, ArrayList<Message>>> iterator = entrySet.iterator();

        // while(iterator.hasNext()) {
        //     Entry<Host, ArrayList<Message>> entry = iterator.next();
        //     Host key = entry.getKey();

        //     ArrayList<Message> messagesCopy = new ArrayList<Message>();
        //     for (Message msgOG: entry.getValue()) {
        //         Message msgCopy = msgOG.getCopy();
        //         messagesCopy.add(msgCopy);
        //     }
        //     messagesClone.put(key, messagesCopy);
        // }
        // lock.unlock();






        Set<Map.Entry<Host, ArrayList<Message>>> entrySet = messages.entrySet();

        // Collection Iterator
        Iterator<Entry<Host, ArrayList<Message>>> iterator = entrySet.iterator();

        while(iterator.hasNext()) {
            Entry<Host, ArrayList<Message>> entry = iterator.next();
            Host key = entry.getKey();
            ArrayList<Message> hostMessages = entry.getValue();

            for (Message m: hostMessages) {
                // System.out.printf("CDM: Message Comparison\n");
                // System.out.printf("M: %s\n", m.toString());
                // System.out.printf("Message: %s\n", message.toString());
                if (m.equals(message)) {
                    // System.out.printf("%s equals %s\n", m.toString(), message.toString());
                    // System.out.printf("Ack: %s\n", m.getReceivedAck());
                    // System.out.printf("Delivered: %s\n", m.getIsDelivered());
                    equalMessages.add(message);
                    if (m.getReceivedAck()) {
                        // System.out.println("Received ack and not delivered");
                        numAcks += 1.0;
                    }
                    if (m.getIsDelivered()) {
                        isDelivered = true;
                    }
                } else {
                    // System.out.printf("%s does not equal %s\n", m.toString(), message.toString());
                }
            }
            majority = numAcks / total;

            // If majority, update ACKs for all, add to canDeliver
            if (majority > 0.5) {
                // System.out.printf("Majority found for: %s\n", message.toString());
                for (Message m: equalMessages) {
                    m.setReceivedAck(true);
                }
                // Add message to canDeliver, make sure not a duplicate
                Message thisMsg = equalMessages.get(0);
                if (thisMsg != null) {
                    boolean isDuplicate = false;
                    ArrayList<Message> deliverList = canDeliver.get(thisMsg.getHost());
                    for (Message thisMsgs: canDeliver.get(thisMsg.getHost())) {
                        if (thisMsg.equals(thisMsgs)) {
                            isDuplicate = true;
                        }
                    }
                    if (!isDuplicate) {
                        deliverList.add(thisMsg.getCopy());
                    }
                }
            }
        }

        if (!isDelivered && (majority > 0.5)) {
            ArrayList<Message> msgList = messages.get(message.getHost());
            msgList.add(message);
            return true;
        }

        // System.out.printf("Can deliver: %s\n", message.toString());
        // printMap(messages);
        return false;
    }






    // /**
    //  * Checks if received Ack for message from majority of hosts
    //  * @param message
    //  * @return
    //  */
    // public boolean canDeliverMessage(Message message) {   
    //     // System.out.println("Inside receivedMajority");
    //     // System.out.printf("message: %s\n", message.toString());

    //     // System.out.println("***** Inside canDeliverMessage");     
    //     // HashMap<Host, ArrayList<Message>> messagesClone = getMessagesClone();
    //     double numAcks = 0.0;
    //     double total = (double) hosts.getHosts().size();
    //     double majority = 0.0;

    //     // If each host does not have
    //     boolean isDelivered = false;
    //     ArrayList<Message> equalMessages = new ArrayList<Message>();

    //     for (Map.Entry<Host, ArrayList<Message>> entry : messages.entrySet()) {
            
    //         // Check that each host has this message
    //         // Message must have received an ack and not already be delivered
    //         Host host = entry.getKey();
    //         ArrayList<Message> hostMessages = entry.getValue();
    //         // System.out.printf("* CDM: Host: %s\n", host.getId());

    //         for (Message m: hostMessages) {
    //             // System.out.printf("CDM: Message Comparison\n");
    //             // System.out.printf("M: %s\n", m.toString());
    //             // System.out.printf("Message: %s\n", message.toString());
    //             if (m.equals(message)) {
    //                 // System.out.printf("%s equals %s\n", m.toString(), message.toString());
    //                 // System.out.printf("Ack: %s\n", m.getReceivedAck());
    //                 // System.out.printf("Delivered: %s\n", m.getIsDelivered());
    //                 equalMessages.add(message);
    //                 if (m.getReceivedAck()) {
    //                     // System.out.println("Received ack and not delivered");
    //                     numAcks += 1.0;
    //                 }
    //                 if (m.getIsDelivered()) {
    //                     isDelivered = true;
    //                 }
    //             } else {
    //                 // System.out.printf("%s does not equal %s\n", m.toString(), message.toString());
    //             }
    //         }
    //         majority = numAcks / total;

    //         // If majority, update ACKs for all, add to canDeliver
    //         if (majority > 0.5) {
    //             // System.out.printf("Majority found for: %s\n", message.toString());
    //             for (Message m: equalMessages) {
    //                 m.setReceivedAck(true);
    //             }
    //             // Add message to canDeliver, make sure not a duplicate
    //             Message thisMsg = equalMessages.get(0);
    //             if (thisMsg != null) {
    //                 boolean isDuplicate = false;
    //                 ArrayList<Message> deliverList = canDeliver.get(thisMsg.getHost());
    //                 for (Message thisMsgs: canDeliver.get(thisMsg.getHost())) {
    //                     if (thisMsg.equals(thisMsgs)) {
    //                         isDuplicate = true;
    //                     }
    //                 }
    //                 if (!isDuplicate) {
    //                     deliverList.add(thisMsg.getCopy());
    //                 }
    //             }
    //         }
    //     }

    //     if (!isDelivered && (majority > 0.5)) {
    //         ArrayList<Message> msgList = messages.get(message.getHost());
    //         msgList.add(message);
    //         return true;
    //     }

    //     // System.out.printf("Can deliver: %s\n", message.toString());
    //     // printMap(messages);
    //     return false;
    // }

    public List<Message> getPrevMessages(Message message) {
        // Get all messages with lower sequence numbers
        List<Message> prevMessages = new ArrayList<Message>();
        int i = 1;
        while (i < message.getSequenceNumber()) {
            Message copy = message.getCopy();
            copy.setSequenceNumber(i);
            prevMessages.add(copy);
        }
        prevMessages.add(message);

        return prevMessages;
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

    // public HashMap<Host, ArrayList<Message>> getMessagesClone() {
    //     HashMap<Host, ArrayList<Message>> messagesClone = new HashMap<Host, ArrayList<Message>>();

    //     messagesLock.readLock().lock();
    //     for (HashMap.Entry<Host, ArrayList<Message>> entry : messages.entrySet()) {
    //         Host key = entry.getKey();

    //         ArrayList<Message> newValue = new ArrayList<Message>();
    //         for (Message oldMsg: entry.getValue()) {
    //             Message newMsg = oldMsg.getCopy();
    //             newValue.add(newMsg);
    //         }

    //         messagesClone.put(key, newValue);
    //     }
    //     messagesLock.readLock().unlock();

    //     return messagesClone;
    // }

    public ConcurrentHashMap<Host, ArrayList<Message>> getMessagesClone() {
        ConcurrentHashMap<Host, ArrayList<Message>> messagesClone = new ConcurrentHashMap<Host, ArrayList<Message>>();

        // readLock.lock();
        // Set<Map.Entry<Host, ArrayList<Message>>> entrySet = messages.entrySet();

        // // Collection Iterator
        // Iterator<Entry<Host, ArrayList<Message>>> iterator = entrySet.iterator();

        // while(iterator.hasNext()) {
        //     Entry<Host, ArrayList<Message>> entry = iterator.next();
        //     Host key = entry.getKey();

        //     ArrayList<Message> messagesCopy = new ArrayList<Message>();
        //     for (Message msgOG: entry.getValue()) {
        //         Message msgCopy = msgOG.getCopy();
        //         messagesCopy.add(msgCopy);
        //     }
        //     messagesClone.put(key, messagesCopy);
        // }
        // readLock.unlock();

        // Entry<String, List<String>> entry = iterator.next();




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
