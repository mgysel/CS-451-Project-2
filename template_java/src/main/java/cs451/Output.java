package cs451;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Output {
    String output;
    private final ReentrantReadWriteLock outputLock;

    public Output() {
        this.output = "";
        this.outputLock = new ReentrantReadWriteLock();
    }

    public void writeDeliver(Message m) {
        System.out.println("***** Inside writeDeliver");
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, m.getHost().getId(), m.getContent());
        outputLock.writeLock().unlock();
    }

    public void writeBroadcast(Message m, Boolean firstBroadcast) {
        if (firstBroadcast) {
            outputLock.writeLock().lock();
            output = String.format("%sb %s\n", output, m.getContent());
            outputLock.writeLock().unlock();
        }
    }

    public String getOutput() {
        return this.output;
    }
}
