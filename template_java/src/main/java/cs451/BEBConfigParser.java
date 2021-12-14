package cs451;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

public class BEBConfigParser {

    private String path;
    private int m;

    public boolean populate(String filename) {
        File file = new File(filename);
        path = file.getPath();
        
        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine();

            if (line.isBlank()) {
                return false;
            }

            try {
                m = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.err.println("Cannot convert line to integer!");
                return false;
            }
        } catch (IOException e) {
            System.err.println("Problem with the hosts file!");
            return false;
        }

        return true;
        
    }

    public String getPath() {
        return path;
    }

    public int getM() {
        return m;
    }
}
