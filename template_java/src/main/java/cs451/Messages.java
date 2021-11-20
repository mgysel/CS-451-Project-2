package cs451;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Messages {
    static Host me;
    static List<Config> configs;
    static Hosts hosts;

    static HashMap<Host, ArrayList<Message>> delivered;
    static HashMap<Host, ArrayList<Message>> sent;
    static HashMap<Host, ArrayList<Message>> messages;
    static HashMap<Host, ArrayList<Message>> ack;
    private HashMap<Host, ArrayList<Message>> toDeliver;

    private final ReentrantReadWriteLock messagesLock = new ReentrantReadWriteLock();
    ReentrantLock lock = new ReentrantLock();
    
    public Messages(Host me, List<Config> configs, Hosts hosts) {
        Messages.configs = configs;
        Messages.hosts = hosts;
        Messages.me = me;

        Messages.delivered = new HashMap<Host, ArrayList<Message>>();
        Messages.sent = new HashMap<Host, ArrayList<Message>>();
        Messages.messages = new HashMap<Host, ArrayList<Message>>();
        Messages.ack = new HashMap<Host, ArrayList<Message>>();
        this.toDeliver = new HashMap<Host, ArrayList<Message>>();

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
            this.toDeliver.put(host, new ArrayList<Message>());
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
            messages.put(from, msgList);
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
            msgList.add(m);
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
                return false;
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

        return true;
    }

    /**
     * Checks if received Ack for message from majority of hosts
     * @param message
     * @return
     */
    public boolean canDeliverMessage(Message message) {   
        System.out.println("Inside receivedMajority");

        // System.out.println("***** Inside canDeliverMessage");     
        // HashMap<Host, ArrayList<Message>> messagesClone = getMessagesClone();
        double numAcks = 0.0;
        double total = (double) hosts.getHosts().size();
        double majority = 0.0;

        // If each host does not have
        boolean isDelivered = false;
        ArrayList<Message> equalMessages = new ArrayList<Message>();
        for (Map.Entry<Host, ArrayList<Message>> entry : messages.entrySet()) {
            // Check that each host has this message
            // Message must have received an ack and not already be delivered
            Host host = entry.getKey();
            ArrayList<Message> hostMessages = entry.getValue();
            // System.out.printf("* CDM: Host: %s\n", host.getId());

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

            // If majority, update ACKs for all
            if (majority > 0.5) {
                for (Message m: equalMessages) {
                    m.setReceivedAck(true);
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

    // public List<Message> messagesToDeliver(Message message) {
    //     List<Message> deliver = new ArrayList<Message>();
        
    //     List<Message> messages = toDeliver.get(message.getHost());
    //     Collections.sort(messages);

    //     int i = message.getSequenceNumber();
    //     boolean cont = true;
        



    //     // Get all messages with lower sequence numbers
    //     List<Message> prevMessages = new ArrayList<Message>();
    //     int i = 1;
    //     while (i < message.getSequenceNumber()) {
    //         Message copy = message.getCopy();
    //         copy.setSequenceNumber(i);
    //         prevMessages.add(copy);
    //     }
    //     prevMessages.add(message);

    //     // Check if each message can be delivered, add to toDeliver
    //     for (Message m: prevMessages) {
    //         if (canDeliverMessage(m)) {
    //             toDeliver.add(m);
    //         }
    //     }
        
    //     return toDeliver;
    // }

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

    public HashMap<Host, ArrayList<Message>> getSent() {
        return Messages.sent;
    }

    public HashMap<Host, ArrayList<Message>> getDelivered() {
        return Messages.delivered;
    }

    public HashMap<Host, ArrayList<Message>> getAck() {
        return Messages.ack;
    }

    public HashMap<Host, ArrayList<Message>> getMessages() {
        return Messages.messages;
    }

    public HashMap<Host, ArrayList<Message>> getMessagesClone() {
        HashMap<Host, ArrayList<Message>> messagesClone = new HashMap<Host, ArrayList<Message>>();

        messagesLock.readLock().lock();
        for (HashMap.Entry<Host, ArrayList<Message>> entry : messages.entrySet()) {
            Host key = entry.getKey();

            ArrayList<Message> newValue = new ArrayList<Message>();
            for (Message oldMsg: entry.getValue()) {
                Message newMsg = oldMsg.getCopy();
                newValue.add(newMsg);
            }

            messagesClone.put(key, newValue);
        }
        messagesLock.readLock().unlock();

        return messagesClone;
    }
}
