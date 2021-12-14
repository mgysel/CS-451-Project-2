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
        int i = 1;
        while (i <= M) {
            Message m = new Message(MessageType.BROADCAST, i, pl.getMe(), Integer.toString(i), new ArrayList<Integer>());
            // System.out.printf("Message: %s\n", m.toString());
            broadcast(m);
            writeBroadcast(m);
            i += 1;
        }
    }

    // Broadcast
    public void broadcast(Message m) {
        // For all peers, pl.send(pi, m)
        List<Host> hosts = pl.getHosts().getHosts();
        for (Host dest: hosts) {
            pl.send(dest, m);
        }
    }

    /**
     * If m is not delivered, deliver m
     * @param src
     * @param m
     */
    private void deliver(Host src, Message m) {
        if (Messages.addMessageToMap(src, m, delivered)) {
            // System.out.println("Writing deliver");
            writeDeliver(src, m);
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
    public void plDeliver(Host h, Message m) {
        deliver(h, m);
        listener.bebDeliver(h, m);
    }

    @Override
    public void bebDeliver(Host h, Message m) {
        // TODO Auto-generated method stub
    }

    @Override
    public void ubDeliver(Host h, Message m) {
        // TODO Auto-generated method stub
    }

    public String close() {
        pl.close();
        return output;
    }
}
