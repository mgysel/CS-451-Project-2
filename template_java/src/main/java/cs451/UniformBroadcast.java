package cs451;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UniformBroadcast extends Thread implements MyEventListener {
    BestEffortBroadcast beb;
    BroadcastConfig bConfig;
    private static String output;

    private boolean running;

    private MyEventListener listener; 

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

        this.running = false;

        UniformBroadcast.pending = new ArrayList<Message>();
        UniformBroadcast.ack = new ConcurrentHashMap<Message, ArrayList<Host>>();
        UniformBroadcast.delivered = new ArrayList<Message>();

        UniformBroadcast.output = "";
    }

    /**
     * Broadcast all messages
    */
    public void broadcastAll() {
        // System.out.println("Inside Broadcast All");
        int i = 1;
        while (i <= bConfig.getM()) {
            Message m = new Message(MessageType.BROADCAST, i, bConfig.getMe(), Integer.toString(i), new ArrayList<Integer>());
            broadcast(m);
            i += 1;
        }
    }

    /**
     * UniformBroadcast m
     * @param m
     */
    public void broadcast(Message m) {
        // Add m to pending
        Messages.addMessageToList(m, UniformBroadcast.pending);

        // Trigger beb Broadcast
        beb.broadcast(m);

        // If Broadcast from me, writeBroadcast
        if (m.getFrom().equals(bConfig.getMe())) {
            writeBroadcast(m);
        }
    }

    /**
     * Delivers m
     * @param m
     */
    public void deliver(Message m) {
        // System.out.println("ub - deliver");
        writeDeliver(m);
        Messages.addMessageToList(m, UniformBroadcast.delivered);
    }

    /**
     * Checks if m has received a majority of acks
     * @param m
     * @return
     */
    private boolean canDeliver(Message m) {
        if (Messages.isMajorityInMap(bConfig.getHosts().getHosts().size(), m, UniformBroadcast.ack)) {
            return true;
        }

        return false;
    }

    /**
     * Loops through pending messages and delivers 
     */
    public void run() {

        running = true;
        while (running) {
            // Loop through pending messages
            ArrayList<Message> pendingClone = Messages.getListClone(UniformBroadcast.pending);

            for (Message m: pendingClone) {
                // If majority hosts for m and m not delivered, deliver
                if (canDeliver(m) && !Messages.isMessageInList(m, UniformBroadcast.delivered)) {
                    deliver(m);
                    if (listener != null) {
                        listener.ubDeliver(m.getFrom(), m);
                    }
                }
            }
        }
    }

    public void setMyEventListener (MyEventListener listener) {
        this.listener = listener;
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
            // If not in pending, Broadcast
            beb.broadcast(m);
        }
    }

    @Override
    public void plDeliver(Host h, Message m) {
        // Nothing
    }

    /**
     * Send ubDeliver event to LCB
     */
    @Override
    public void ubDeliver(Host h, Message m) {
        // Nothing
    }

    /**
     * Writes deliver event to output
     * @param m
     */
    public static void writeDeliver(Message m) {
        outputLock.writeLock().lock();
        UniformBroadcast.output = String.format("%sd %s %s\n", UniformBroadcast.output, m.getFrom().getId(), m.getContent());
        outputLock.writeLock().unlock();
    }

    /**
     * Writes broadcast event to output
     * @param m
     */
    public static void writeBroadcast(Message m) {
        outputLock.writeLock().lock();
        UniformBroadcast.output = String.format("%sb %s\n", UniformBroadcast.output, m.getContent());
        outputLock.writeLock().unlock();
    }

    public String close() {
        running = false;
        beb.close();
        return output;
    }
}
