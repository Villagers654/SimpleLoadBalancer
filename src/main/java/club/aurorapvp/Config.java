package club.aurorapvp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class Config {
  private final ConfigurationNode config;

  public Config(Path configPath) throws IOException {
    File configFile = new File(configPath.toFile(), "config.conf");

    ConfigurationLoader<?> loader = HoconConfigurationLoader.builder()
        .setPath(configFile.toPath())
        .build();

    if (!configFile.exists()) {
      configFile.getParentFile().mkdirs();
      configFile.createNewFile();
    }

    if (Files.exists(configFile.toPath())) {
      config = loader.load();
    } else {
      config = loader.createEmptyNode();
    }
  }

  public boolean contains(String key) {
    return config.getNode((Object[]) key.split("\\.")).getValue() != null;
  }

  public List<String> getStringList(String path) {
    List<String> result = new ArrayList<>();

    ConfigurationNode listNode = config.getNode((Object[]) path.split("\\."));

    for (ConfigurationNode valueNode : listNode.getChildrenList()) {
      String value = valueNode.getString();
      if (value != null) {
        result.add(value);
      }
    }

    return result;
  }
}
