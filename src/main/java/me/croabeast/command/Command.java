package me.croabeast.command;

import org.bukkit.Keyed;
import org.bukkit.command.PluginIdentifiableCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a command that can be executed by players or the console,
 * supporting sub-commands, tab completions, and plugin identification.
 * <p>
 * A {@code Command} extends multiple interfaces to provide a comprehensive structure for commands:
 * <ul>
 *   <li>{@link BaseCommand} for basic command properties (name, aliases, and executable action).</li>
 *   <li>{@link Completable} for generating tab-completion suggestions.</li>
 *   <li>{@link PluginIdentifiableCommand} and {@link Keyed} for associating the command with a plugin.</li>
 * </ul>
 * </p>
 * <p>
 * In addition, this interface supports enabling/disabling the command, overriding existing commands,
 * and managing sub-commands.
 * </p>
 * <p>
 * Example usage:
 * <pre><code>
 * public class MyCommand implements Command {
 *     private final Set<BaseCommand> subCommands = new HashSet<>();
 *
 *     {@literal @}Override
 *     public String getName() {
 *         return "mycommand";
 *     }
 *
 *     {@literal @}Override
 *     public List&lt;String&gt; getAliases() {
 *         return Arrays.asList("mc", "mycmd");
 *     }
 *
 *     {@literal @}Override
 *     public Executable getExecutable() {
 *         return (sender, args) -&gt; {
 *             // command logic
 *             return Executable.State.TRUE;
 *         };
 *     }
 *
 *     {@literal @}Override
 *     public boolean isEnabled() {
 *         return true;
 *     }
 *
 *     {@literal @}Override
 *     public boolean isOverriding() {
 *         return false;
 *     }
 *
 *     {@literal @}Override
 *     public Set&lt;BaseCommand&gt; getSubCommands() {
 *         return subCommands;
 *     }
 *
 *     {@literal @}Override
 *     public void registerSubCommand(@NotNull BaseCommand sub) {
 *         subCommands.add(sub);
 *     }
 *
 *     // Other methods from Completable, PluginIdentifiableCommand, Keyed, and Registrable...
 * }</code></pre></p>
 *
 * @see BaseCommand
 * @see Completable
 * @see PluginIdentifiableCommand
 * @see Keyed
 */
public interface Command extends BaseCommand, Completable, PluginIdentifiableCommand, Keyed {

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

    boolean isRegistered();

    boolean register();

    boolean unregister();

    /**
     * Retrieves the set of sub-commands associated with this command.
     *
     * @return a {@link Set} of {@link BaseCommand} representing sub-commands.
     */
    @NotNull
    Set<BaseCommand> getSubCommands();

    /**
     * Registers a sub-command under this command.
     *
     * @param sub the sub-command to register (must not be {@code null}).
     */
    void registerSubCommand(@NotNull BaseCommand sub);

    /**
     * Retrieves a sub-command matching the given name.
     * <p>
     * The method checks both the primary name and aliases of each registered sub-command.
     * </p>
     *
     * @param name the name or alias of the sub-command.
     * @return the matching {@link BaseCommand}, or {@code null} if no match is found.
     */
    @Nullable
    default BaseCommand getSubCommand(String name) {
        if (name == null || name.isEmpty())
            return null;
        for (BaseCommand command : getSubCommands()) {
            Set<String> names = new HashSet<>(command.getAliases());
            names.add(command.getName());
            if (names.contains(name)) return command;
        }
        return null;
    }

    /**
     * Retrieves the wildcard permission for this command.
     * <p>
     * If there are sub-commands registered, the wildcard permission is formed by appending ".*" to
     * this command's permission. Otherwise, it returns {@code null}.
     * </p>
     *
     * @return the wildcard permission string, or {@code null} if there are no sub-commands.
     */
    @Nullable
    default String getWildcardPermission() {
        return getSubCommands().isEmpty() ? null : getPermission() + ".*";
    }
}
