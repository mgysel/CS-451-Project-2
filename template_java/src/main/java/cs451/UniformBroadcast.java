package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UniformBroadcast extends Thread implements MyEventListener {
    private PerfectLinks pl;
    private String output;

    private List<Config> configs;
    
    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();
    
    public UniformBroadcast(PerfectLinks pl) {
        this.pl = pl;
        this.pl.setMyEventListener(this);
        this.configs = pl.configs;
    }

    // Broadcast
    public void broadcast() {
        pl.sendAll();
        // 
    }

    // // Run server
    // public void run() {
    //     // pl.start();

    //     // Receive Packet
    //     DatagramPacket packet = new DatagramPacket(inBuf, inBuf.length);
    //     try {
    //         packet.setLength(inBuf.length);
    //         socket.receive(packet);

    //         InetAddress address = packet.getAddress();
    //         int port = packet.getPort();
    //         packet = new DatagramPacket(inBuf, inBuf.length, address, port);
            
    //         String message = new String(packet.getData(), packet.getOffset(),  packet.getLength()).trim();
    //         // System.out.printf("PACKET LENGTH: %s\n", packet.getLength());
    //         // System.out.printf("MESSAGE LENGTH: %s\n", message.length());
    //         // Clear buffer after processing it

    //         int id = pl.getHostByAddress(address, port).getId();
    //         // System.out.printf("RECEIVED MESSAGE: %s\n", message);
    //         if (!message.contains("ACK/")) {
    //             // System.out.println("About to deliver message");
    //             deliver(id, message);

    //             // Send ack back, even if already delivered
    //             // System.out.printf("This is what I am sending back: %s\n", String.format("ACK/%s", message));
    //             pl.send(id, String.format("ACK/%s", message));
    //         } else {
    //             // Process ACK
    //             if (message.split("/").length > 1) {
    //                 if (!message.split("/")[1].equals("")) {
    //                     // System.out.printf("Message Length: %s\n", message.split("/").length);
    //                     // System.out.printf("This is what I am putting in ACK: %s\n", message.split("/")[1]);
    //                     String m = message.split("/")[1]
    //                     putMessageInMap(ack, id, m);
    //                     // Trigger receive ACK event
    //                     listener.ReceivedAck(m);

    //                 }
    //             }
    //         }
    //         inBuf = new byte[256];
    //         // Arrays.fill(inBuf,(byte)0);
    //     } catch (SocketTimeoutException e) {
    //         continue;
    //     } catch (IOException e) {
    //         System.err.println("Server Cannot Receive Packet: " + e);
    //     }
    // }

    // Return output
    public String close() {
        return pl.close();
    }

    @Override
    public void PerfectLinksDeliver(Host p, Message m) {
        // If we have acks for all peers, then deliver
        // deliver(p, m);
        // System.out.println("Caught the delivery");
    }

    private void deliver(int p, String m) {
        writeDeliver(p, m);
    }

    private void writeDeliver(int p, String m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, p, m);
        outputLock.writeLock().unlock();
    }

    @Override
    public void ReceivedAck(String m) {
        // If we have received all acks, then deliver message
        
    }
}
