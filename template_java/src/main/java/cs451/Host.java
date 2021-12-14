package cs451;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

public class Host {

    private static final String IP_START_REGEX = "/";

    private int id;
    private String ip;
    private int port = -1;
    InetSocketAddress address;
    private List<Integer> dependencies;

    public boolean populate(String idString, String ipString, String portString) {
        try {
            id = Integer.parseInt(idString);

            String ipTest = InetAddress.getByName(ipString).toString();
            if (ipTest.startsWith(IP_START_REGEX)) {
                ip = ipTest.substring(1);
            } else {
                ip = InetAddress.getByName(ipTest.split(IP_START_REGEX)[0]).getHostAddress();
            }

            port = Integer.parseInt(portString);
            if (port <= 0) {
                System.err.println("Port in the hosts file must be a positive number!");
                return false;
            }

            address = new InetSocketAddress(ip, port);
        } catch (NumberFormatException e) {
            if (port == -1) {
                System.err.println("Id in the hosts file must be a number!");
            } else {
                System.err.println("Port in the hosts file must be a number!");
            }
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return true;
    }

    public int getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setDependencies(List<Integer> dependencies) {
        this.dependencies = dependencies;
    }

    public List<Integer> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        String output = "";
        if (this != null) {
            // id, address, dependencies
            output += String.format("ID: %d\n", this.id);
            output += String.format("Address: %s\n", this.address.toString());
            output += String.format("Dependencies: ");
            for (int d: this.dependencies) {
                output += String.format("%d ", d);
            }
            output += String.format("\n");
        }

        return output;
    }

}
