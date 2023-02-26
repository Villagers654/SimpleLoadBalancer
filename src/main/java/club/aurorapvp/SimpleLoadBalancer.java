package club.aurorapvp;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

@Plugin(
    id = "simpleloadbalancer",
    name = "Simple Load Balancer",
    version = "1.0-SNAPSHOT",
    authors = "Villagers654"
)
public class SimpleLoadBalancer {
  private final ProxyServer server;
  private final Config config;
  public static Logger LOGGER;
  public static Path DATA_FOLDER;
  private static final Map<RegisteredServer, Integer> connectedPlayers = new HashMap<>();

  @Inject
  public SimpleLoadBalancer(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory)
      throws IOException {
    this.server = server;
    this.config = new Config(dataDirectory);
    LOGGER = logger;
    DATA_FOLDER = dataDirectory;
  }

  @Subscribe
  public void onProxyInitializeEvent(ProxyInitializeEvent event) {
    long startTime = System.currentTimeMillis();

    try {
      Files.createDirectories(DATA_FOLDER);
    } catch (IOException e) {
      LOGGER.error("Failed to load config", e);
      return;
    }

    LOGGER.info("SimpleLoadBalancer loaded in " + (System.currentTimeMillis() - startTime) + "ms");
  }

  @Subscribe
  public void onProxyShutdownEvent(ProxyShutdownEvent event) {
    LOGGER.info("SimpleLoadBalancer unloaded");
  }

  @Subscribe
  public void onServerPreConnectEvent(ServerPreConnectEvent event) {
    String preServerName = event.getOriginalServer().getServerInfo().getName();

    if (config.contains(preServerName)) {
      List<String> serverNames = config.getStringList(preServerName);
      connectedPlayers.clear();

      for (String serverName : serverNames) {
        server.getServer(serverName).ifPresent(childServer -> connectedPlayers.put(childServer,
            childServer.getPlayersConnected().size()));
      }

      RegisteredServer smallestServer = null;
      int smallestKey = Integer.MAX_VALUE;

      for (Map.Entry<RegisteredServer, Integer> entry : connectedPlayers.entrySet()) {
        RegisteredServer server = entry.getKey();
        int key = entry.getValue();
        if (key < smallestKey) {
          smallestKey = key;
          smallestServer = server;
        }
      }

      if (smallestServer != null) {
        LOGGER.info("Attempting to redirect {} <{}> to {}",
            event.getPlayer().getGameProfile().getName(),
            event.getPlayer().getUniqueId().toString(),
            smallestServer.getServerInfo().getName());
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        event.getPlayer().createConnectionRequest(smallestServer).fireAndForget();
      } else {
        LOGGER.warn("No available servers to connect player to");
      }
    }
  }
}