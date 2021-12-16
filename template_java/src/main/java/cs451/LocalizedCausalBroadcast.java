package cs451;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LocalizedCausalBroadcast extends Thread implements MyEventListener {
    UniformBroadcast ub;
    BroadcastConfig bConfig;
    List<Integer> dependencies;

    private boolean running;

    // Stores messages beb-delivered but not urb-delivered
    static ArrayList<Message> pending;
    ArrayList<Integer> VC;
    int myDelivered;

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
        this.running = false;

        // Initialize vectorClock
        this.VC = new ArrayList<Integer>();
        for (int i=0; i < bConfig.getHosts().getHosts().size(); i++) {
            VC.add(0);
        }
        // Initialize myDelivered
        this.myDelivered = 0;

        // Get my dependencies
        for (LCBConfig config: bConfig.getConfigs()) {
            if (config.getId() == bConfig.getMe().getId()) {
                this.dependencies = config.getDependencies();
                break;
            }
        }

        System.out.printf("Dependencies: %s\n", writeDependencies(dependencies));
    }

    /**
     * Broadcast all messages
    */
    public void broadcastAll() {
        System.out.println("Inside Broadcast All");
        int i = 1;
        while (i <= bConfig.getM()) {
            Message m = new Message(MessageType.BROADCAST, i, bConfig.getMe(), Integer.toString(i), getDependenciesVCClone());
            // System.out.printf("Message: %s\n", m.toString());
            broadcast(m);
            i += 1;
        }
    }

    // Broadcast
    public void broadcast(Message m) {
        // System.out.println("\n***** LCB - Broadcast");

        // Trigger ub Broadcast
        ub.broadcast(m);
        // Increase my vector after broadcast m
        incVectorClock(bConfig.getMe());
        writeBroadcast(m);
        
    }

    public void deliver(Message m) {
        // System.out.println("\n***** LCB - Deliver");
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
        
        running = true;
        while (running) {
            // Loop through every message in pending
            ArrayList<Message> pendingClone = Messages.getListClone(pending);
            // System.out.printf("Pending Length: %d\n", pendingClone.size());
            for (Message m: pendingClone) {
                boolean canDeliver = true;

                // Check if my VC >= message VC for all processes
                for (int i=0; i < VC.size(); i++) {
                    if (i == bConfig.getMe().getId() - 1) {
                        // If i = me, check myDelivered >= messageVC
                        if (myDelivered < m.getVC().get(i)) {
                            canDeliver = false;
                            continue;
                        }
                    } else {
                        // If i != me, check myVC >= messageVC
                        if (VC.get(i) < m.getVC().get(i)) {
                            canDeliver = false;
                            continue;
                        }
                    }
                }
                // If from me, check FIFO
                if (m.getFrom().equals(bConfig.getMe())) {
                    if (!(m.getSequenceNumber() == myDelivered+1)) {
                        // If my message does not equal myDelivered + 1, breaks FIFO property
                        canDeliver = false;
                    }
                }


                if (canDeliver) {
                    // Remove m from pending
                    Messages.removeMessageFromList(m, pending);
                    
                    if (!m.getFrom().equals(bConfig.getMe())) {
                        // update VC
                        incVectorClock(m.getFrom());
                    } else {
                        // update myDelivered
                        myDelivered++;
                    }

                    // deliver m
                    deliver(m);
                }
            }
        }
    }

    public ArrayList<Integer> getVCClone() {
        ArrayList<Integer> VCClone = new ArrayList<Integer>();
        for (int i=0; i<VC.size(); i++) {
            VCClone.add(VC.get(i));
        }

        return VCClone;
    }

    public ArrayList<Integer> getDependenciesVCClone() {
        ArrayList<Integer> VCClone = new ArrayList<Integer>();
        for (int i=0; i<VC.size(); i++) {
            if (dependencies.contains(i+1) || bConfig.getMe().getId() == i+1) {
                // If i corresponds to me or dependencies, add to VCClone
                VCClone.add(VC.get(i));
            } else {
                // If i is not me or a dependency, add 0
                VCClone.add(0);
            }
        }

        return VCClone;
    }

    public String writeVectorClock(ArrayList<Integer> vc) {
        String vcString = "[";
        for (int i=0; i < vc.size(); i++) {
            if (i != 0) {
                vcString += ", ";
            }
            vcString += Integer.toString(vc.get(i));
        }
        vcString += "]";
        return vcString;
    }

    public String writeDependencies(List<Integer> d) {
        String vcString = "[";
        for (int i=0; i < d.size(); i++) {
            if (i != 0) {
                vcString += ", ";
            }
            vcString += Integer.toString(d.get(i));
        }
        vcString += "]";
        return vcString;
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
        // System.out.println("\n***** LCB - Inside ubDeliver");
        
        // Add m to pending
        Messages.addMessageToList(m, pending);
    }

    public void writeDeliver(Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, m.getFrom().getId(), m.getContent());
        
        // // ***** For testing
        // // output += "Vector Clock D: ";
        // // output = String.format("%s%s\n", output, writeVectorClock(getDependenciesVCClone()));
        // output += "Vector Clock B: ";
        // ArrayList<Integer> thisVCClone = getVCClone();
        // thisVCClone.set(m.getFrom().getId() - 1, thisVCClone.get(m.getFrom().getId() - 1) - 1);
        // output = String.format("%s%s\n", output, writeVectorClock(thisVCClone));
        // output += "Vector Clock M: ";
        // output = String.format("%s%s\n", output, writeVectorClock(m.getVC()));
        // output += "Vector Clock A: ";
        // output = String.format("%s%s\n", output, writeVectorClock(getVCClone()));
        // output += "Vector Clock D: ";
        // output = String.format("%s%s\n", output, writeVectorClock(getDependenciesVCClone()));

        outputLock.writeLock().unlock();
    }

    public void writeBroadcast(Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sb %s\n", output, m.getContent());

        // // ***** For testing
        // output += "Vector Clock B: ";
        // ArrayList<Integer> thisVCClone = getVCClone();
        // thisVCClone.set(m.getFrom().getId() - 1, thisVCClone.get(m.getFrom().getId() - 1) - 1);
        // output = String.format("%s%s\n", output, writeVectorClock(thisVCClone));
        // output += "Vector Clock M: ";
        // output = String.format("%s%s\n", output, writeVectorClock(m.getVC()));
        // output += "Vector Clock A: ";
        // output = String.format("%s%s\n", output, writeVectorClock(getVCClone()));
        // output += "Vector Clock D: ";
        // output = String.format("%s%s\n", output, writeVectorClock(getDependenciesVCClone()));

        outputLock.writeLock().unlock();
    }

    public String close() {
        ub.close();
        running = false;
        return output;
    }
}
