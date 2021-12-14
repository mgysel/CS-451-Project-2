package cs451;

public interface MyEventListener {
    void plDeliver(Host h, Message m);
    void bebDeliver(Host h, Message m);
    void ubDeliver(Host h, Message m);
    // void ReceivedAck(String m);
}
