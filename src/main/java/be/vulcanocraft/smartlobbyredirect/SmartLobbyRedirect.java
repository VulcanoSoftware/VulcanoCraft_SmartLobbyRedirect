package be.vulcanocraft.smartlobbyredirect;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Plugin(
    id = "smartlobbyredirect",
    name = "SmartLobbyRedirect",
    version = "1.4",
    authors = {"vulcanocraft"}
)
public class SmartLobbyRedirect {

    private final ProxyServer server;
    private final Path dataDirectory;
    private String lobbyServerName = "lobby";

    @Inject
    public SmartLobbyRedirect(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
        loadConfig();
    }

    private void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            Path configFile = dataDirectory.resolve("config.yml");
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                    }
                }
            }

            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configFile)) {
                props.load(in);
                this.lobbyServerName = props.getProperty("lobby-server", "lobby");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;

        String command = event.getCommand().toLowerCase().replaceFirst("^/", "");
        if (!command.equals("lobby") && !command.equals("hub")) return;

        String currentServer = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("");

        boolean isLobby = currentServer.equalsIgnoreCase(lobbyServerName);

        if (command.equals("lobby")) {
            if (!isLobby) {
                // Niet op de lobby → teleporteren
                event.setResult(CommandExecuteEvent.CommandResult.denied());

                server.getServer(lobbyServerName).ifPresentOrElse(
                        target -> player.createConnectionRequest(target).connect(),
                        () -> player.sendMessage(Component.text("§cDe lobby server kon niet gevonden worden."))
                );
            } else {
                // Al op de lobby → voer backend commando uit
                event.setResult(CommandExecuteEvent.CommandResult.denied());
                player.spoofChatInput("/akropolis:lobby");
            }
        } else if (command.equals("hub") && isLobby) {
            // Alleen op lobby behandelen, anders negeren
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            player.spoofChatInput("/akropolis:lobby");
        }
    }
}