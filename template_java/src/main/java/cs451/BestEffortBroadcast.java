package cs451;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BestEffortBroadcast extends Thread implements MyEventListener {
    private PerfectLinks pl;
    static ConcurrentHashMap<Host, ArrayList<Message>> messages;
    static ConcurrentHashMap<Host, ArrayList<Message>> delivered;

    private static String output;
    private int M;
    
    public BestEffortBroadcast(PerfectLinks pl, int M) {
        this.pl = pl;
        this.pl.setMyEventListener(this);
        this.M = M;
    }

    /**
     * Broadcast all messages
     */
    public void broadcastAll() {
        int i = 1;
        while (i <= M) {
            Message m = new Message(MessageType.BROADCAST, i, pl.getMe(), Integer.toString(i));
            broadcast(m);
            i += 1;
        }
    }

    // Broadcast
    public void broadcast(Message m) {
        // For all peers, pl.send(pi, m)
        List<Host> hosts = pl.getHosts().getHosts();
        for (Host dest: hosts) {
            pl.send(dest, m);
            // Add message to messages
            if (m.getType() == MessageType.BROADCAST) {
                Messages.addMessageToMap(dest, m, messages);
            }
        }
        Output.writeBroadcast(output, m);
    }

    /**
     * If m is not delivered, deliver m
     * @param src
     * @param m
     */
    private void deliver(Host src, Message m) {
        if (Messages.isMessageInMap(src, m, delivered) == null) {
            Output.writeDeliver(output, src, m);
        }
    }

    public String close() {
        pl.close();
        return output;
    }

    @Override
    public void PerfectLinksDeliver(Host src, Message m) {
        deliver(src, m);
        System.out.println("Caught the delivery");
    }

    // @Override
    // public void ReceivedAck(String m) {
        
    // }
}
