package cs451;

public class MessageWrapper {
    Message message;
    boolean ack;
    boolean delivered;

    public MessageWrapper(Message message, boolean ack, boolean delivered) {
        this.message = message;
        this.ack = ack;
        this.delivered = delivered;
    }

    public Message getMessage() {
        return this.message;
    }

    public boolean getAck() {
        return this.ack;
    }

    public void setAck(boolean bool) {
        this.ack = bool;
    }

    public boolean getDelivered() {
        return this.delivered;
    }

    public void setDelivered(boolean bool) {
        this.delivered = bool;
    }
}
