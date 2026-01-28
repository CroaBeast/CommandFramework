package me.croabeast.command;

import me.croabeast.common.Registrable;
import org.bukkit.Keyed;
import org.bukkit.command.PluginIdentifiableCommand;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a command that can be executed by players or the console,
 * supporting sub-commands, tab completions, and plugin identification.
 * <p>
 * A {@code Command} extends multiple interfaces to provide a comprehensive structure for commands:
 * <ul>
 *   <li>{@link BaseCommand} for basic command properties (name, aliases, and executable predicate).</li>
 *   <li>{@link Completable} for generating tab-completion suggestions.</li>
 *   <li>{@link PluginIdentifiableCommand} and {@link Keyed} for associating the command with a plugin.</li>
 *   <li>{@link Registrable} for handling command registration.</li>
 * </ul>
 * </p>
 * <p>
 * In addition, this interface supports enabling/disabling the command, overriding existing commands,
 * and managing sub-commands.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyCommand implements Command {
 *
 *     private final Set<BaseCommand> subCommands = new HashSet<>();
 *
 *     @Override
 *     public String getName() {
 *         return "my-command";
 *     }
 *
 *     @Override
 *     public List<String> getAliases() {
 *         return Arrays.asList("mc", "my-cmd");
 *     }
 *
 *     @Override
 *     public boolean execure(CommandSender sender, String[] args) {
 *         // command logic
 *     }
 *
 *     @Override
 *     public boolean isEnabled() {
 *         return true;
 *     }
 *
 *     @Override
 *     public boolean isOverriding() {
 *         return false;
 *     }
 *
 *     @Override
 *     public Set<BaseCommand> getSubCommands() {
 *         return subCommands;
 *     }
 *
 *     @Override
 *     public void addSubCommand(@NotNull BaseCommand sub) {
 *         subCommands.add(sub);
 *     }
 *
 *     @Override
 *     public void removeSubCommand(@NotNull String name) {
 *         for (BaseCommand sub : subCommands) {
 *             if (!sub.getName().equalsIgnoreCase(name)) return;
 *
 *             subCommands.remove(sub);
 *             break;
 *         }
 *     }
 *
 *     // Other methods from Completable, PluginIdentifiableCommand, Keyed, and Registrable...
 * }
 * }</pre></p>
 *
 * @see BaseCommand
 * @see Completable
 * @see PluginIdentifiableCommand
 * @see Keyed
 * @see Registrable
 */
public interface Command extends BaseCommand, Completable, PluginIdentifiableCommand, Keyed, Registrable {

    /**
     * Checks if the command is currently enabled.
     *
     * @return {@code true} if the command is enabled; {@code false} otherwise.
     */
    boolean isEnabled();

    /**
     * Checks if this command is intended to override an existing command.
     *
     * @return {@code true} if the command is overriding; {@code false} otherwise.
     */
    boolean isOverriding();

    /**
     * Gets the {@link SubCommandMap} that manages this command's sub-commands.
     *
     * @return the sub-command map for this command; never {@code null}.
     */
    @NotNull
    SubCommandMap getSubCommandMap();

    /**
     * Sets the {@link SubCommandMap} that manages this command's sub-commands.
     *
     * @param subCommandMap the sub-command map to set; must not be {@code null}.
     */
    void setSubCommandMap(@NotNull SubCommandMap subCommandMap);

    /**
     * Gets the permission node for this command, optionally with a wildcard suffix.
     * <p>
     * When {@code wildcard} is {@code true} and this command has sub-commands, the returned
     * permission will have {@code ".*"} appended (e.g., {@code "myplugin.command.*"}).
     * This is useful for granting access to all sub-commands at once.
     * </p>
     *
     * @param wildcard whether to append a wildcard suffix if sub-commands exist.
     *
     * @return the permission string, optionally suffixed with {@code ".*"}; never {@code null}.
     */
    @NotNull
    default String getPermission(boolean wildcard) {
        return getPermission() + (wildcard && !getSubCommandMap().isEmpty() ? ".*" : "");
    }

    /**
     * Gets the {@link Synchronizer} responsible for synchronizing this command's changes
     * with the server.
     *
     * @return the synchronizer for this command; never {@code null}.
     */
    @NotNull
    Synchronizer getSynchronizer();
}
