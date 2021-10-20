package cs451;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class Main {
    private static String output = ""; 

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //write/flush output file if necessary
        System.out.println("Writing output.");
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        Host me = null;
        List<Host> hosts = parser.hosts();
        for (Host host: hosts) {
            if (host.getId() == parser.myId()) {
                me = host;
            }
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");
        System.out.println("List of configs is:");
        System.out.println("==========================");
        List<Config> configs = parser.configs();
        for (Config config: configs) {
            System.out.println(config.getId());
            System.out.println("M: " + config.getM());
            System.out.println();
        }
        System.out.println();

        System.out.println("Doing some initialization\n");
        // TODO: Need initialization here?
        // STEPS
        // ----> 1. DONE: Determine n/i from parser.config()
        // ----> 2. Determine from Hosts the ip/port (kinda already done)

        System.out.println("Broadcasting and delivering messages...\n");
        
        // TODO: SEND MESSAGES WITH UDP
        // Send(address, number of messages)?
        // HOW DEAL WITH PACKET LOSS?

        // *********************************************************************
        // SEND MESSAGES
        // *********************************************************************
        // Step 1:Create the socket object for carrying the data.

        // CREATE MY SOCKET
        // Create datagram socket with ip and port
        DatagramSocket socket = null;
        InetSocketAddress address = new InetSocketAddress(me.getIp(), me.getPort());
        try {
            socket = new DatagramSocket(address);
            socket.setSoTimeout(1000); 
        } catch(SocketException e) {
            System.err.println("Main - Cannot Create Socket: " + e);
        }

        // *********************************************************************
        // SERVER
        // *********************************************************************
        Server server = new Server(socket, output);
        server.start();

        // *********************************************************************
        // CLIENT
        // *********************************************************************
        Client client = new Client(me, socket, configs, hosts, output);
        client.sendAll();

        System.out.println("AFTER CLIENT");
        System.out.println("OUTPUT");
        System.out.printf("%s\n", output);
        // *********************************************************************

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }

    }
}
