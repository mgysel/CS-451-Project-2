package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LCBConfigParser {

    private String path;
    private static final String SPACES_REGEX = "\\s+";

    private List<LCBConfig> configs = new ArrayList<>();

    public boolean populate(String filename) {
        File file = new File(filename);
        path = file.getPath();
        
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            int lineNum = 1;
            int M = 0;
            for(String line; (line = br.readLine()) != null; lineNum++) {
                if (line.isBlank()) {
                    continue;
                }

                // Split each line
                String[] splits = line.split(SPACES_REGEX);
                if (lineNum == 1) {
                    if (splits.length != 1) {
                        System.err.println("Problem with the line " + lineNum + " in the config file!");
                        return false;
                    } else {
                        try {
                            M = Integer.parseInt(splits[0]);
                        } catch (NumberFormatException e) {
                            System.err.println("Id and m in the hosts file must be an integer!");
                            return false;
                        } 
                    }
                } else {
                    int id;
                    try {
                        id = Integer.parseInt(splits[0]);
                    } catch (NumberFormatException e) {
                        System.err.println("Id and m in the hosts file must be an integer!");
                        return false;
                    } 
    
                    List<Integer> dependencies = new ArrayList<Integer>();
                    int i = 1;
                    while (i < splits.length) {
                        try {
                            int dependency = Integer.parseInt(splits[i]);
                            dependencies.add(dependency);
                        } catch (NumberFormatException e) {
                            System.err.println("Id and m in the hosts file must be an integer!");
                            return false;
                        } 
                        i += 1;
                    }
    
                    LCBConfig newConfig = new LCBConfig(M, id, dependencies);
                    configs.add(newConfig);
                }
            }
        } catch (IOException e) {
            System.err.println("Problem with the hosts file!");
            return false;
        }

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

    public List<LCBConfig> getConfigs() {
        return configs;
    }

    // Sort by id
    class ConfigsComparator implements Comparator<LCBConfig> {

        public int compare(LCBConfig a, LCBConfig b) {
            return a.getId() - b.getId();
        }
    }

}
