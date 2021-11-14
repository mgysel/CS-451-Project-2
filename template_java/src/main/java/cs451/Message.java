package cs451;

enum MessageType {
    BROADCAST,
    ACK,
    FORWARD
}

public class Message {
    private MessageType type;
    private String content;

    public Message(String content, MessageType type) {
        this.type = type;
        this.content = content;
    }

    public MessageType getType() {
        return this.type;
    }

    public String getContent() {
        return this.content;
    }
}
