package cs451;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UniformBroadcast extends Thread implements MyEventListener {
    BestEffortBroadcast beb;
    BroadcastConfig bConfig;
    private static String output;

    // Stores messages beb-delivered but not urb-delivered
    static ArrayList<Message> pending;
    // Stores correct processes that have seen message
    static ConcurrentHashMap<Message, ArrayList<Host>> ack;
    // Stores messages that have been delivered
    static ArrayList<Message> delivered;

    private static final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();

    public UniformBroadcast(BestEffortBroadcast beb, BroadcastConfig bConfig)  {
        this.bConfig = bConfig;
        this.beb = beb;
        this.beb.setMyEventListener(this);

        UniformBroadcast.pending = new ArrayList<Message>();
        UniformBroadcast.ack = new ConcurrentHashMap<Message, ArrayList<Host>>();
        UniformBroadcast.delivered = new ArrayList<Message>();

        UniformBroadcast.output = "";
    }

    // Broadcast
    public void broadcast(Message m) {
        // Add m to pending
        Messages.addMessageToList(m, pending);

        // Trigger beb Broadcast
        beb.broadcast(m);

        // If Broadcast from me, writeBroadcast
        if (m.getFrom().equals(bConfig.getMe())) {
            writeBroadcast(m);
        }
    }

    public void deliver(Message m) {
        writeDeliver(m);
        Messages.addMessageToList(m, delivered);
    }

    private boolean canDeliver(Message m) {
        if (Messages.isMajorityInMap(bConfig.getHosts().getHosts().size(), m, ack)) {
            return true;
        }

        return false;
    }

    /**
    * Check if messages can be delivered, deliver
    */
    public void run() {
        while (true) {
            // Loop through pending messages
            ArrayList<Message> pendingClone = Messages.getListClone(UniformBroadcast.pending);
            for (Message m: pendingClone) {
                // If majority hosts for m and m not delivered, deliver
                if (canDeliver(m) && !Messages.isMessageInList(m, delivered)) {
                    deliver(m);
                }
            }
        }
    }

    /**
     * On beb deliver, add m to ack.
     * If m not in pending, add to pending and beb broadcast
     * @param h
     * @param m
     */
    @Override
    public void bebDeliver(Host h, Message m) {
        // Add message to ack
        Messages.addHostToMap(h, m, UniformBroadcast.ack);
        
        // If not in pending, add to pending
        if (Messages.addMessageToList(m, UniformBroadcast.pending)) {
            // Broadcast
            beb.broadcast(m);
        }
    }

    @Override
    public void plDeliver(Host p, Message m) {
        // TODO Auto-generated method stub
        
    }

    public static void writeDeliver(Message m) {
        outputLock.writeLock().lock();
        UniformBroadcast.output = String.format("%sd %s %s\n", UniformBroadcast.output, m.getFrom(), m.getContent());
        outputLock.writeLock().unlock();
    }

    public static void writeBroadcast(Message m) {
        outputLock.writeLock().lock();
        UniformBroadcast.output = String.format("%sb %s\n", UniformBroadcast.output, m.getContent());
        outputLock.writeLock().unlock();
    }

    public String close() {
        beb.close();
        return output;
    }
}
