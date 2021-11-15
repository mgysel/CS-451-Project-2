package cs451;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

public class Hosts {
    public List<Host> hosts;
    public Host me;
    
    public Hosts(List<Host> hosts) {
        this.hosts = hosts;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public Host getHostById(int id) {
        for (Host host: hosts) {
            if (host.getId() == id) {
                return host;
            }
        }

        return null;
    }

    public InetSocketAddress getAddressById(int id) {
        Host host = getHostById(id);
        InetSocketAddress address = new InetSocketAddress(host.getIp(), host.getPort());
        return address;
    }

    public Host getHostByAddress(InetAddress ip, int port) {
        for (Host host: hosts) {
            // Create Socket Address for host and ip/port
            InetSocketAddress host1 = new InetSocketAddress(host.getIp(), host.getPort());
            InetSocketAddress host2 = new InetSocketAddress(ip, port);

            if (host1.equals(host2)) {
                return host;
            }
        }

        return null;
    }
}
