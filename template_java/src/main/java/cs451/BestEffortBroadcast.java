package cs451;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BestEffortBroadcast extends Thread implements MyEventListener {
    private PerfectLinks pl;
    private String output;
    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();
    
    public BestEffortBroadcast(PerfectLinks pl) {
        this.pl = pl;
        this.pl.setMyEventListener(this);
    }

    // Broadcast All
    public void broadcastAll() {
        pl.sendAll();
    }

    // Broadcast
    public void broadcast(String m) {
        // For all peers, pl.send(pi, m)
        List<Host> peers = pl.getPeers();
        for (Host peer: peers) {
            pl.send(peer.getId(), m);
        }
        writeBroadcast(m);
    }

    // Run server
    public void run() {
        pl.start();
    }

    // Return output
    public String close() {
        return pl.close();
    }

    @Override
    public void PerfectLinksDeliver(int p, String m) {
        deliver(p, m);
        System.out.println("Caught the delivery");
    }

    private void deliver(int p, String m) {
        writeDeliver(p, m);
    }

    private void writeDeliver(int p, String m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, p, m);
        outputLock.writeLock().unlock();
    }

    private void writeBroadcast(String m) {
        outputLock.writeLock().lock();
        output = String.format("%sb %s\n", output, m);
        outputLock.writeLock().unlock();
    }
}
