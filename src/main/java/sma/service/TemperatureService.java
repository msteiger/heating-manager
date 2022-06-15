package sma.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemperatureService {

    private Pattern tempRegex = Pattern.compile("t=(\\d+)");

    private String devicename;
    private String root;

    public TemperatureService(String devicename, String root) {
        this.devicename = devicename;
        this.root = root;
    }

    public float getTemperature() throws IOException{
        Path slaveFile = Paths.get(root, "devices", devicename, "w1_slave");

        if (!Files.exists(slaveFile)) {
            throw new FileNotFoundException(slaveFile.toString());
        }

        float temp = parseTemperature(slaveFile);
        return temp;
    }

    private float parseTemperature(Path slaveFile) throws IOException {
        List<String> lines = Files.readAllLines(slaveFile, StandardCharsets.UTF_8);
        if (lines.size() >= 2) {
            String lineTwo = lines.get(1);
            Matcher m = tempRegex.matcher(lineTwo);
            if (m.find()) {
                Integer milliDeg = Integer.valueOf(m.group(1));
                return milliDeg * 0.001f;
            }
        }
        throw new IOException("Could not read temperature in " + lines);
    }
}
