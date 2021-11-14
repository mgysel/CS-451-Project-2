package cs451;

enum MessageType {
    BROADCAST,
    ACK,
    FORWARD
}

public class Message {
    private MessageType type;
    private String content;

    public Message(MessageType type, String content) {
        this.type = type;

        // Content differs based on message type
        if (type == MessageType.BROADCAST) {
            this.content = content;
        } else if (type == MessageType.ACK) {
            this.content = String.format("A/%s", content);
        } else if (type == MessageType.FORWARD) {
            this.content = String.format("F/%s", content);
        }
    }

    public MessageType getType() {
        return this.type;
    }

    public String getContent() {
        return this.content;
    }

    // Compare Message objects
    @Override
    public boolean equals(Object o) {
  
        // If the object is compared with itself then return true 
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Message or not
        "null instanceof [type]" also returns false */
        if (!(o instanceof Message)) {
            return false;
        }
        
        // typecast o to Message so that we can compare data members
        Message m = (Message) o;
        
        // Compare the data members and return accordingly
        if (m.getType() == this.getType() && m.getContent() == this.getContent()) {
            return true;
        }

        return false;
    }
}
