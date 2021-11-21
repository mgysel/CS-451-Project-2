package cs451;

import java.util.List;

public class BestEffortBroadcast extends Thread implements MyEventListener {
    private PerfectLinks pl;
    
    public BestEffortBroadcast(PerfectLinks pl) {
        this.pl = pl;
        // this.pl.setMyEventListener(this);
    }

    // Broadcast
    public void broadcast(Message m) {
        // For all peers, pl.send(pi, m)
        List<Host> hosts = pl.getHosts().getHosts();
        for (Host host: hosts) {
            pl.send(host, m);
        }
    }

    @Override
    public void PerfectLinksDeliver(Host p, Message m) {
        // deliver(p, m);
        // System.out.println("Caught the delivery");
    }

    @Override
    public void ReceivedAck(String m) {
        
    }
}
