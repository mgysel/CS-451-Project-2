package cs451;

import java.util.List;

public class LocalizedCausalBroadcast implements MyEventListener {
    UniformBroadcast ub;
    BroadcastConfig bConfig;

    private static String output;

    List<Integer> vectorClock;
    
    public LocalizedCausalBroadcast(UniformBroadcast ub, BroadcastConfig bConfig)  {
        this.ub = ub;
        this.bConfig = bConfig;
        this.ub.setMyEventListener(this);

        // Initialize vectorClock
        for (int i=0; i < bConfig.getHosts().getHosts().size(); i++) {
            vectorClock.add(1);
        }
    }

    public String close() {
        ub.close();
        return output;
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
        // TODO Auto-generated method stub
    }
}
