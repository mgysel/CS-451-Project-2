package cs451;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class UniformBroadcast extends Thread implements MyEventListener {
    BestEffortBroadcast beb;
    private static String output;
    private int M;

    // Stores messages beb-delivered but not urb-delivered
    static ConcurrentHashMap<Message, Boolean> pending;
    // Stores messages that have been delivered
    static ConcurrentHashMap<Host, ArrayList<Message>> delivered;
    // Stores correct processes that have seen message
    static ConcurrentHashMap<Message, ArrayList<Host>> ack;

    public UniformBroadcast(BestEffortBroadcast beb, int M)  {
        this.M = M;
        this.beb = beb;
        this.beb.setMyEventListener(this);

        UniformBroadcast.pending = new ConcurrentHashMap<Message, Boolean>();
        UniformBroadcast.delivered = new ConcurrentHashMap<Host, ArrayList<Message>>();
        UniformBroadcast.ack = new ConcurrentHashMap<Message, ArrayList<Host>>();

        UniformBroadcast.output = "";
    }

    // Broadcast
    public void broadcast(Message m) {
        // 1. Add m to pending
        // UniformBroadcast.put(m, true);

        // 2. Trigger BEB
        beb.broadcast(m);
    }



    @Override
    public void plDeliver(Host p, Message m) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void bebDeliver(Host p, Message m) {
        // TODO Auto-generated method stub
        
    }
}
