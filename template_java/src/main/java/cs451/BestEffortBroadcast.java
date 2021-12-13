package cs451;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BestEffortBroadcast extends Thread implements MyEventListener {
    private PerfectLinks pl;
    static ConcurrentHashMap<Host, ArrayList<Message>> delivered;
    private static final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();

    private MyEventListener listener; 

    private static String output;
    private int M;
    
    public BestEffortBroadcast(PerfectLinks pl, BroadcastConfig bConfig) {
        this.pl = pl;
        this.pl.setMyEventListener(this);
        this.M = bConfig.getM();

        BestEffortBroadcast.output = "";

        BestEffortBroadcast.delivered = new ConcurrentHashMap<Host, ArrayList<Message>>();
    }

    /**
     * Broadcast all messages
     */
    public void broadcastAll() {
        System.out.println("Inside Broadcast All");
        int i = 1;
        while (i <= M) {
            Message m = new Message(MessageType.BROADCAST, i, pl.getMe(), Integer.toString(i));
            System.out.printf("Message: %s\n", m.toString());
            broadcast(m);
            i += 1;
        }
    }

    // Broadcast
    public void broadcast(Message m) {
        System.out.println("Inside Broadcast");

        // For all peers, pl.send(pi, m)
        List<Host> hosts = pl.getHosts().getHosts();
        for (Host dest: hosts) {
            pl.send(dest, m);
        }
        writeBroadcast(m);
        System.out.printf("Output: %s\n", output);
    }

    /**
     * If m is not delivered, deliver m
     * @param src
     * @param m
     */
    private void deliver(Host src, Message m) {
        if (Messages.addMessageToMap(src, m, delivered)) {
            System.out.println("Writing deliver");
            writeDeliver(src, m);
            System.out.printf("Output: %s\n", BestEffortBroadcast.output);
        }
    }

    public static void writeDeliver(Host h, Message m) {
        outputLock.writeLock().lock();
        BestEffortBroadcast.output = String.format("%sd %s %s\n", BestEffortBroadcast.output, h.getId(), m.getContent());
        outputLock.writeLock().unlock();
    }

    public static void writeBroadcast(Message m) {
        outputLock.writeLock().lock();
        BestEffortBroadcast.output = String.format("%sb %s\n", BestEffortBroadcast.output, m.getContent());
        outputLock.writeLock().unlock();
    }

    public void setMyEventListener (MyEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void plDeliver(Host src, Message m) {
        deliver(src, m);
        listener.bebDeliver(src, m);
        // System.out.println("Caught the delivery");
    }

    @Override
    public void bebDeliver(Host p, Message m) {
        // TODO Auto-generated method stub
        
    }

    public String close() {
        pl.close();
        return output;
    }
}
