package cs451;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static void writeOutput(String output, String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            System.err.println("Cannot Write to Output File: " + e);
        }
    }

    private static void handleSignal(PerfectLinks pl, BestEffortBroadcast beb, String filename) {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        String output = pl.close();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        writeOutput(output, filename);
    }

    private static void initSignalHandlers(PerfectLinks pl, BestEffortBroadcast beb, String filename) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal(pl, beb, filename);
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        // initSignalHandlers();

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

        // *************************************************************
        // PerfectLinks Configuration
        // *************************************************************
        // System.out.println("Path to config:");
        // System.out.println("===============");
        // System.out.println(parser.plConfigPath() + "\n");
        // System.out.println("List of configs is:");
        // System.out.println("==========================");
        // List<Config> configs = parser.plConfigConfigs();
        // for (Config config: configs) {
        //     System.out.println(config.getId());
        //     System.out.println("M: " + config.getM());
        //     System.out.println();
        // }
        // System.out.println();

        // *************************************************************
        // BestEfforBroadcast Configuration
        // *************************************************************
        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.bebConfigPath() + "\n");
        System.out.println("List of configs is:");
        System.out.println("==========================");
        
        // Loop through hosts and build a configuration file
        List<Config> configs = new ArrayList<Config>();
        int m = parser.bebConfigM();
        for (Host host: hosts) {
            Config newConfig = new Config(m, host.getId());
            configs.add(newConfig);
        }
        // Print output of configuration
        for (Config config: configs) {
            System.out.println(config.getId());
            System.out.println("M: " + config.getM());
            System.out.println();
        }
        System.out.println();


        System.out.println("Doing some initialization\n");
        PerfectLinks pl = new PerfectLinks(me, configs, hosts);
        // initSignalHandlers(pl, parser.output());
        BestEffortBroadcast beb = new BestEffortBroadcast(pl);
        initSignalHandlers(pl, beb, parser.output());

        System.out.println("Broadcasting and delivering messages...\n");

        // *********************************************************************
        // SEND AND RECEIVE MESSAGES
        // *********************************************************************
        // pl.start();
        // pl.sendAll();
        beb.start();
        beb.broadcastAll();

        // *********************************************************************

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }

    }
}
