package cs451;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Messages {
    HashMap<Host, ArrayList<Message>> delivered;
    HashMap<Host, ArrayList<Message>> sent;
    HashMap<Host, ArrayList<Message>> messages;
    HashMap<Host, ArrayList<Message>> ack;
    
    public Messages(List<Config> configs) {
        this.delivered = new HashMap<Host, ArrayList<Message>>();
        this.sent = new HashMap<Host, ArrayList<Message>>();
        this.messages = new HashMap<Host, ArrayList<Message>>();
        this.ack = new HashMap<Host, ArrayList<Message>>();

        // Initialize messages with messages to send
        for (Config config: configs) {
            if (config.getId() != me.getId()) {
                int receiver = config.getId();

                // Add messages to messages map
                int i = 1;
                while (i <= config.getM()) {
                    // Put each message in map
                    String message = Integer.toString(i);
                    putMessageInMap(messages, receiver, message);
                    i++;
                }
            }
        }
    }
}
