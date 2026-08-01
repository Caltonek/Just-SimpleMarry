package pl.caltonek.simpleMarry.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pl.caltonek.simpleMarry.config.ConfigManager;

public class ReloadCommand implements CommandExecutor {

    private final @NotNull ConfigManager configManager;

    public ReloadCommand(final @NotNull ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String[] args
    ) {
        if (!sender.hasPermission("simple-marry.command.admin.reload")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        configManager.loadConfigs();
        sender.sendMessage(configManager.getMessage("reload-success"));
        return true;
    }
}