package jupiterpi.vocabulum.discordbot;

import jupiterpi.tools.files.Path;
import jupiterpi.tools.files.TextFile;

import java.util.HashMap;
import java.util.Map;

public class ConfigFile {
    public static Path configFilePath = Path.getRunningDirectory().file("configuration");  // example-configuration

    private static Map<String, String> properties = new HashMap<>();

    static {
        for (String line : new TextFile(configFilePath).getFile()) {
            if (line.isEmpty()) continue;
            String[] parts = line.split(": ");
            properties.put(parts[0], parts[1]);
        }
    }

    public static String getProperty(String propertyName) {
        return properties.get(propertyName);
    }
}
