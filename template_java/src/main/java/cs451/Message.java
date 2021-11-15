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
        this.content = content;
    }

    public Message(String message) {
        String[] messageComponents = message.split("/");
        if (messageComponents.length == 2) {
            if (messageComponents[0].equals("A")) {
                this.type = MessageType.ACK;
            } else if (messageComponents[0].equals("B")) {
                this.type = MessageType.BROADCAST;
            } else if (messageComponents[0].equals("F")) {
                this.type = MessageType.FORWARD;
            }
            this.content = messageComponents[1];
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
        if (m.getType() == this.getType() && m.getContent().equals(this.getContent())) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        String output = "";
        if (this.type == MessageType.BROADCAST) {
            output += "B/";
        } else if (this.type == MessageType.ACK) {
            output += "A/";
        } else if (this.type == MessageType.FORWARD) {
            output += "F/";
        }
        output += this.content;

        return output;
    }
}
