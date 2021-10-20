package cs451;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class Client {
    private Host me;
    private DatagramSocket socket;
    private List<Config> configs;
    private List<Host> hosts;
    private String output;

    private byte[] buf;

    public Client(Host me, DatagramSocket socket, List<Config> configs, List<Host> hosts, String output) {
        this.me = me;
        this.socket = socket;
        this.configs = configs;
        this.hosts = hosts;
        this.output = output;
    }

    public boolean send(InetSocketAddress address, String message) {
        System.out.println("INSIDE SEND\n");
        buf = message.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, address);

        try {
            socket.send(packet);
            output = String.format("%sb %s\n", output, message);
            System.out.println("OUTPUT");
            System.out.printf("%s\n", output);
        } catch(IOException e) {
            System.err.println("Client.Send Error: " + e);
            return false;
        }
        
        return true;
    }

    /**
     * Send messages per configuration
     * Do not send messages to self
     */
    public void sendAll() {
        System.out.println("INSIDE CLIENT.SENDALL");

        // Loop through configs, get receiver address
        for (Config config: configs) {
            if (config.getId() != me.getId()) {
                Host receiver = getHostById(config.getId());
                InetSocketAddress address = new InetSocketAddress(receiver.getIp(), receiver.getPort());
    
                // Send number of messages
                int i = 1;
                while (i <= config.getM()) {
                    System.out.printf("Sending %d\n", i);
                    send(address, Integer.toString(i));
                    i++;
                }
            }
        }
    }

    public void close() {
        socket.close();
    }

    private Host getHostById(int id) {
        for (Host host: hosts) {
            if (host.getId() == id) {
                return host;
            }
        }

        return null;
    }
}
