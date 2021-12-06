package cs451;

/**
 * HostAck used as a wrapper for host, so we know if a message
 */
public class HostAck {
    private Host host;
    private boolean receivedAck;
    private boolean delivered;

    public HostAck(Host host, boolean receivedAck, boolean delivered) {
        this.host = host;
        this.receivedAck = receivedAck;
        this.delivered = delivered;
    }

    public Host getHost() {
        return this.host;
    }

    public boolean getReceivedAck() {
        return this.receivedAck;
    }

    public void setReceivedAck(boolean bool) {
        this.receivedAck = bool;
    }

    public boolean getDelivered() {
        return this.delivered;
    }

    public void setDelivered(boolean bool) {
        this.delivered = bool;
    }

    public static HostAck getClone(HostAck h) {
        HostAck clone = new HostAck(h.getHost(), h.getReceivedAck(), h.getDelivered());
        return clone;
    }
}