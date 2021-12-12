package cs451;

public interface MyEventListener {
    void plDeliver(Host p, Message m);
    void bebDeliver(Host p, Message m);
    // void ReceivedAck(String m);
}
