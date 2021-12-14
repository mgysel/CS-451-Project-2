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

    private static void handleSignal(BestEffortBroadcast beb, String filename) {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        String output = beb.close();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        writeOutput(output, filename);
    }

    private static void handleSignal(UniformBroadcast ub, String filename) {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        String output = ub.close();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        writeOutput(output, filename);
    }

    private static void handleSignal(LocalizedCausalBroadcast lcb, String filename) {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        String output = lcb.close();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        writeOutput(output, filename);
    }

    private static void initSignalHandlers(BestEffortBroadcast beb, String filename) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal(beb, filename);
            }
        });
    }

    private static void initSignalHandlers(UniformBroadcast ub, String filename) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal(ub, filename);
            }
        });
    }

    private static void initSignalHandlers(LocalizedCausalBroadcast lcv, String filename) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal(lcb, filename);
            }
        });
    }

    private static Hosts getHosts(Parser parser) {
        List<Host> hostsList = parser.hosts();
        Hosts hosts = new Hosts(hostsList);
        
        return hosts;
    }

    private static Host getMe(Parser parser, Hosts hosts) {
        Host me = null;
        for (Host host: hosts.getHosts()) {
            if (host.getId() == parser.myId()) {
                me = host;
                return me;
            }
        }

        return me;
    }

    private static List<UBConfig> getBEBBroadcastConfigs(Parser parser, Hosts hosts) {
        int m = parser.bebConfigM();
        System.out.println("Get Broadcast Configs");
        System.out.println(m);
        List<UBConfig> configs = new ArrayList<UBConfig>();
        for (Host host: hosts.getHosts()) {
            UBConfig config = new UBConfig(m, host.getId());
            configs.add(config);
        }

        return configs;
    }

    private static List<LCBConfig> getLCBConfigs(Parser parser, Hosts hosts) {
        System.out.println("Get CO Broadcast Configs");
        List<LCBConfig> configs = parser.lcbConfigConfigs();
        
        for (Host host: hosts.getHosts()) {
            for (LCBConfig lcbConfig: configs) {
                if (host.getId() == lcbConfig.getId()) {
                    host.setDependencies(lcbConfig.getDependencies());
                }
            }
        }

        return configs;
    }

    private static void printInit(Parser parser) {
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");
    }

    private static void printHosts(Hosts hosts) {
        for (Host host: hosts.getHosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();
    }

    private static void printConfigs(Parser parser, List<LCBConfig> configs) {
        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.lcbConfigPath() + "\n");
        System.out.println("List of configs is:");
        System.out.println("==========================");
        for (LCBConfig config: configs) {
            System.out.printf("%d\n", config.getId());
            System.out.println("M: " + config.getM());
            List<Integer> dependencies = config.getDependencies();
            System.out.printf("Dependencies: ");
            if (dependencies.size() == 0) {
                System.out.print("None");
            } else {
                for (int dependency: dependencies) {
                    System.out.printf("%d ", dependency);
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        // initSignalHandlers();
        printInit(parser);

        // example
        Hosts hosts = getHosts(parser);
        Host me = getMe(parser, hosts);
        printHosts(hosts);


        // *************************************************************
        // PerfectLinks Configuration
        // *************************************************************
        // List<Config> configs = parser.plConfigConfigs();
        // printConfigs(parser, configs);

        // *************************************************************
        // UniformBroadcast Configuration
        // *************************************************************
        // int M = parser.bebConfigM();
        // List<UBConfig> configs = getBEBBroadcastConfigs(parser, hosts);
        // BroadcastConfig bConfig = new BroadcastConfig(M, me, configs, hosts);
        // printConfigs(parser, configs);

        // *************************************************************
        // Localized Causal Broadcast Configuration
        // *************************************************************
        List<LCBConfig> configs = getLCBConfigs(parser, hosts);

        int M = configs.get(0).getM();
        BroadcastConfig bConfig = new BroadcastConfig(M, me, configs, hosts);
        printConfigs(parser, configs);

        // System.out.println("Correct hosts?");
        // for (Host host: hosts.getHosts()) {
        //     System.out.println(host.toString());
        // }


        System.out.println("Doing some initialization\n");
        PerfectLinks pl = new PerfectLinks(bConfig);
        BestEffortBroadcast beb = new BestEffortBroadcast(pl, bConfig);
        UniformBroadcast ub = new UniformBroadcast(beb, bConfig);
        // initSignalHandlers(pl, parser.output());
        // initSignalHandlers(beb, parser.output());
        initSignalHandlers(ub, parser.output());

        System.out.println("Broadcasting and delivering messages...\n");

        // *********************************************************************
        // SEND AND RECEIVE MESSAGES
        // *********************************************************************
        // pl.start();
        // pl.sendAll();
        // fifo.start();
        // fifo.broadcast();
        pl.start();
        // beb.broadcastAll();
        ub.broadcastAll();

        // *********************************************************************

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }

    }
}
