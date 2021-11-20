package cs451;

enum MessageType {
    BROADCAST,
    ACK,
    FORWARD
}

public class Message {
    private MessageType type;
    private Host from;
    private String content;
    private boolean receivedAck;
    private boolean isDelivered;

    public Message(MessageType type, Host from, String content) {
        this.type = type;
        this.from = from;
        this.content = content;
        this.receivedAck = false;
        this.isDelivered = false;
    }

    public Message(MessageType type, Host from, String content, boolean receivedAck) {
        this.type = type;
        this.from = from;
        this.content = content;
        this.receivedAck = receivedAck;
    }

    public Message(String message, Hosts hosts) {
        String[] messageComponents = message.split("/");
        if (messageComponents.length == 3) {
            if (messageComponents[0].equals("A")) {
                this.type = MessageType.ACK;
            } else if (messageComponents[0].equals("B")) {
                this.type = MessageType.BROADCAST;
            } else if (messageComponents[0].equals("F")) {
                this.type = MessageType.FORWARD;
            }
            try {
                Integer id = Integer.parseInt(messageComponents[1]);
                this.from = hosts.getHostById(id);
            } catch (NumberFormatException e) {
                System.out.printf("Cannot convert message because ID is not an integer: ", e);
            } catch (NullPointerException e) {
                System.out.printf("Cannot convert message because ID is a null pointer: ", e);
            }
            this.content = messageComponents[2];
            this.receivedAck = false;
        }
    }

    public MessageType getType() {
        return this.type;
    }

    public Host getHost() {
        return this.from;
    }

    public String getContent() {
        return this.content;
    }

    public Host getFrom() {
        return this.from;
    }

    public boolean getReceivedAck() {
        return this.receivedAck;
    }

    public void setReceivedAck(boolean bool) {
        this.receivedAck = bool;
    }

    public boolean getIsDelivered() {
        return this.isDelivered;
    }

    public void setIsDelivered(boolean bool) {
        this.isDelivered = bool;
    }

    public Message getCopy() {
        MessageType type = this.getType();
        Host from = this.getHost();
        String content = new String(this.getContent());
        boolean receivedAck = this.getReceivedAck();

        Message newM = new Message(type, from, content, receivedAck);

        return newM;

    }

    public static boolean isValidMessage(String message) {
        String[] messageComponents = message.split("/");
        if (messageComponents.length == 3) {
            return true;
        }
        return false;
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
        if (this != null) {
            if (this.type == MessageType.BROADCAST) {
                output += "B";
            } else if (this.type == MessageType.ACK) {
                output += "A";
            } else if (this.type == MessageType.FORWARD) {
                output += "F";
            }
            output = String.format("%s/%d/%s", output, this.from.getId(), this.content);
        }

        return output;
    }
}
