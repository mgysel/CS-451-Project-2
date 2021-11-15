package cs451;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Messages {
    static Host me;
    static List<Config> configs;
    static Hosts hosts;

    static HashMap<Host, ArrayList<Message>> delivered;
    static HashMap<Host, ArrayList<Message>> sent;
    static HashMap<Host, ArrayList<Message>> messages;
    static HashMap<Host, ArrayList<Message>> ack;
    
    public Messages(Host me, List<Config> configs, Hosts hosts) {
        Messages.configs = configs;
        Messages.delivered = new HashMap<Host, ArrayList<Message>>();
        Messages.sent = new HashMap<Host, ArrayList<Message>>();
        Messages.messages = new HashMap<Host, ArrayList<Message>>();
        Messages.ack = new HashMap<Host, ArrayList<Message>>();

        // Initialize messages with messages to send
        for (Config config: configs) {
            Host receiver = hosts.getHostById(config.getId());

            // Add messages to messages map
            int i = 1;
            while (i <= config.getM()) {
                // Put each message in map
                Message message = new Message(MessageType.BROADCAST, me, Integer.toString(i));
                putMessageInMap(messages, receiver, message);
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
    public boolean putMessageInMap(HashMap<Host, ArrayList<Message>> map, Host from, Message message) {        
        ArrayList<Message> msgList = map.get(from);

        if(msgList == null) {
            // If no messages in delivered, create list
            msgList = new ArrayList<Message>();
            msgList.add(message);
            map.put(from, msgList);
            return true;
        } else {
            // If messages in delivered, make sure not a duplicate
            if(!msgList.contains(message)) {
                msgList.add(message);
                return true;
            } 
        }

        return false;
    }

    public boolean isMessageInMap(HashMap<Host, ArrayList<Message>> map, Host from, Message message) {
        ArrayList<Message> msgList = map.get(from);

        if(msgList == null) {
            // If no messages, not in list
            return false;
        } else {
            // If messages in delivered, make sure not a duplicate
            if(msgList.contains(message)) {
                System.out.println("Message is in map");
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
                System.out.println("Ack does not equal messages");
                return false;
            } else {
                for (Message message: messageList) {
                    if (!ackList.contains(message)) {
                        System.out.println("Ack does not equal messages");
                        return false;
                    }
                }
            }
        }

        System.out.println("Ack equals messages");
        return true;  
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
}
