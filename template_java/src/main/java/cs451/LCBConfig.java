package cs451;

import java.util.List;

public class LCBConfig {
    // How many messages the process should send
    private int m;
    // Index of process
    private int id;
    // dependencies
    private List<Integer> dependencies;

    public LCBConfig(int m, int id, List<Integer> dependencies) {
        this.m = m;
        this.id = id;
        this.dependencies = dependencies;
    }

    public boolean populate(String mString, String idString) {
        try {
            m = Integer.parseInt(mString);
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            System.err.println("Id and m in the hosts file must be an integer!");
            return false;
        } 
        return true;
    }

    public int getM() {
        return m;
    }

    public int getId() {
        return id;
    }

    public List<Integer> getDependencies() {
        return dependencies;
    }
}
