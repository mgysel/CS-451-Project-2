package cs451;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Output {
    private static final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();;

    public static void writeDeliver(String output, Host h, Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, h.getId(), m.getContent());
        outputLock.writeLock().unlock();
    }

    public static void writeBroadcast(String output, Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sb %s\n", output, m.getContent());
        outputLock.writeLock().unlock();
    }
}
