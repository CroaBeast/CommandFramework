package me.croabeast.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the base structure of a command, including its name, aliases, and executable action.
 * <p>
 * A {@code BaseCommand} defines the essential properties of a command:
 * <ul>
 *   <li>The command name, as returned by {@link #getName()}.</li>
 *   <li>A list of alternative names (aliases) via {@link #getAliases()}.</li>
 *   <li>An executable predicate defined by {@link CommandPredicate}, which is run when the command is invoked.</li>
 * </ul>
 * </p>
 * <p>
 * Implementations of this interface are expected to provide concrete logic for the command's behavior,
 * as well as its permission node via {@link Permissible#getPermission()}.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyCommand implements BaseCommand {
 *     @Override
 *     public String getName() {
 *         return "mycommand";
 *     }
 *
 *     @Override
 *     public List<String> getAliases() {
 *         return Arrays.asList("mc", "mycmd");
 *     }
 *
 *     @Override
 *     public boolean execute(CommandSender sender, String[] args) {
 *         // command execution logic
 *         return true;
 *     }
 *
 *     @Override
 *     public String getPermission() {
 *         return "myplugin.mycommand";
 *     }
 * }
 * }</pre></p>
 *
 * @see Permissible
 * @see CommandPredicate
 */
public interface BaseCommand extends Permissible {

    /**
     * Gets the primary name of the command.
     *
     * @return the command name as a {@link String}.
     */
    @NotNull
    String getName();

    /**
     * Gets the list of aliases for the command.
     * <p>
     * Aliases are alternative names that can be used to invoke the command.
     * </p>
     *
     * @return a {@link List} of alias strings.
     */
    @NotNull
    List<String> getAliases();

    /**
     * Executes the command with the given sender and arguments.
     *
     * @param sender the {@link CommandSender} who invoked the command; must not be {@code null}.
     * @param args   the arguments passed to the command; must not be {@code null}.
     *
     * @return {@code true} if the command was executed successfully; {@code false} otherwise.
     */
    boolean execute(@NotNull CommandSender sender, @NotNull String[] args);

    /**
     * Gets the {@link CommandPredicate} that encapsulates this command's execution logic.
     * <p>
     * By default, this returns a method reference to {@link #execute(CommandSender, String[])}.
     * </p>
     *
     * @return the command predicate for this command; never {@code null}.
     */
    @NotNull
    default CommandPredicate getPredicate() {
        return this::execute;
    }
}
