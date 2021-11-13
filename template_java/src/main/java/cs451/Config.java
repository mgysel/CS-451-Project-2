package cs451;

public class Config {
    // How many messages the process should send
    private int m;
    // Index of process
    private int id;

    public Config(int m, int id) {
        this.m = m;
        this.id = id;
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

    public int getId() {
        return id;
    }

    public int getM() {
        return m;
    }
}
