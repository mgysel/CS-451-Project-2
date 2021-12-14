package cs451;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LocalizedCausalBroadcast extends Thread implements MyEventListener {
    UniformBroadcast ub;
    BroadcastConfig bConfig;

    // Stores messages beb-delivered but not urb-delivered
    static ArrayList<Message> pending;
    ArrayList<Integer> VC;

    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Lock readLock = rwLock.readLock();
    private static final Lock writeLock = rwLock.writeLock();

    private String output;
    private static final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();
    
    public LocalizedCausalBroadcast(UniformBroadcast ub, BroadcastConfig bConfig)  {
        this.ub = ub;
        this.bConfig = bConfig;
        this.ub.setMyEventListener(this);
        LocalizedCausalBroadcast.pending = new ArrayList<Message>();
        this.output = "";

        // Initialize vectorClock
        this.VC = new ArrayList<Integer>();
        for (int i=0; i < bConfig.getHosts().getHosts().size(); i++) {
            VC.add(0);
        }
    }

    /**
     * Broadcast all messages
    */
    public void broadcastAll() {
        System.out.println("Inside Broadcast All");
        int i = 1;
        while (i <= bConfig.getM()) {
            Message m = new Message(MessageType.BROADCAST, i, bConfig.getMe(), Integer.toString(i), getVCClone());
            // System.out.printf("Message: %s\n", m.toString());
            broadcast(m);
            i += 1;
        }
    }

    // Broadcast
    public void broadcast(Message m) {
        System.out.println("\n***** LCB - Broadcast");

        // Trigger ub Broadcast
        ub.broadcast(m);
        // Increase my vector after broadcast m
        incVectorClock(bConfig.getMe());
        writeBroadcast(m);
        
    }

    public void deliver(Message m) {
        System.out.println("\n***** LCB - Deliver");
        writeDeliver(m);
    }

    private void incVectorClock(Host h) {
        readLock.lock();
        int thisVC = this.VC.get(h.getId() - 1);
        readLock.unlock();

        writeLock.lock();
        this.VC.set(h.getId() - 1, thisVC + 1);
        writeLock.unlock();
    }

    public void run() {
        System.out.println("***** LCB - Inside run");
        Messages.printMessageList(pending);

        while (true) {
            // Loop through every message in pending
            ArrayList<Message> pendingClone = Messages.getListClone(pending);
            for (Message m: pendingClone) {
                boolean canDeliver = true;
                for (int i=0; i < VC.size(); i++) {
                    // If my VC >= message VC for all processes
                    // AND (FIFO) 
                    if (VC.get(i) < m.getVC().get(i)) {
                        canDeliver = false;
                        continue;
                    }
                }

                if (canDeliver) {
                    // Remove m from pending
                    Messages.removeMessageFromList(m, pending);
                    
                    if (!m.getFrom().equals(bConfig.getMe())) {
                        // update VC
                        incVectorClock(m.getFrom());
                    }

                    // deliver m
                    deliver(m);
                }
            }
        }
    }

    public void writeDeliver(Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, m.getFrom().getId(), m.getContent());
        output = String.format("%s%s\n", output, writeVectorClock());
        outputLock.writeLock().unlock();
    }

    public void writeBroadcast(Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sb %s\n", output, m.getContent());
        output = String.format("%s%s\n", output, writeVectorClock());
        outputLock.writeLock().unlock();
    }

    public String writeVectorClock() {
        String vcString = "Vector Clock: [";
        for (int i=0; i < VC.size(); i++) {
            if (i != 0) {
                vcString += ", ";
            }
            vcString += Integer.toString(VC.get(i));
        }
        vcString += "]";
        return vcString;
    }

    public ArrayList<Integer> getVCClone() {
        ArrayList<Integer> VCClone = new ArrayList<Integer>();
        for (int i=0; i<VC.size(); i++) {
            VCClone.add(VC.get(i));
        }

        return VCClone;
    }

    @Override
    public void plDeliver(Host h, Message m) {
        // TODO Auto-generated method stub
    }

    @Override
    public void bebDeliver(Host h, Message m) {
        // TODO Auto-generated method stub
    }

    @Override
    public void ubDeliver(Host h, Message m) {
        System.out.println("\n***** LCB - Inside ubDeliver");
        
        // Add m to pending
        // NOTE: Class algo does not do this for me, but we need to wait to deliver me
        // TODO: Should pending be organized with source?
        Messages.addMessageToList(m, pending);

        // Call deliver-pending - THIS IS NOW RUN
        // deliverPending();
    }

    public String close() {
        ub.close();
        return output;
    }
}
