package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PLConfigParser {

    private String path;
    private static final String SPACES_REGEX = "\\s+";

    private List<UBConfig> configs = new ArrayList<>();

    public boolean populate(String filename) {
        File file = new File(filename);
        path = file.getPath();
        
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            int lineNum = 1;
            for(String line; (line = br.readLine()) != null; lineNum++) {
                if (line.isBlank()) {
                    continue;
                }

                // Split each line
                String[] splits = line.split(SPACES_REGEX);
                if (splits.length != 2) {
                    System.err.println("Problem with the line " + lineNum + " in the config file!");
                    return false;
                } 

                int m, id;
                try {
                    m = Integer.parseInt(splits[0]);
                    id = Integer.parseInt(splits[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Id and m in the hosts file must be an integer!");
                    return false;
                } 

                UBConfig newConfig = new UBConfig(m, id);
                configs.add(newConfig);
            }
        } catch (IOException e) {
            System.err.println("Problem with the hosts file!");
            return false;
        }

        // // Check ids in configs file are correct
        // if (!checkIdRange()) {
        //     System.err.println("Hosts ids in config file are not within the range!");
        //     return false;
        // }

        // sort by id
        Collections.sort(configs, new ConfigsComparator());
        
        return true;
    }

    // private boolean checkIdRange() {
    //     int num = configs.size();
    //     for (Config config : configs) {
    //         if (config.getId() < 1 || config.getId() > num) {
    //             System.err.println("Id of a host in config file is not in the right range!");
    //             return false;
    //         }
    //     }

    //     return true;
    // }

    public String getPath() {
        return path;
    }

    public List<UBConfig> getConfigs() {
        return configs;
    }

    // Sort by id
    class ConfigsComparator implements Comparator<UBConfig> {

        public int compare(UBConfig a, UBConfig b) {
            return a.getId() - b.getId();
        }

    }

}
