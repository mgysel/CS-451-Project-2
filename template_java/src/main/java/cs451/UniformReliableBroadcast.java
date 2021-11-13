package cs451;

public class UniformReliableBroadcast extends Thread {
    private PerfectLinks pl;
    
    public UniformReliableBroadcast(PerfectLinks pl) {
        this.pl = pl;
    }

    // Broadcast messages based on configuration
    public void broadcast() {
        // Keep sending until receive an ack for each message from everyone
        pl.sendAll();
    }

    // Run server
    public void run() {
        pl.start();
    }

    // Return output
    public String close() {
        return pl.close();
    }
}