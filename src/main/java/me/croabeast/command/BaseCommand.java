package me.croabeast.command;

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
 * <pre><code>
 * public class MyCommand implements BaseCommand {
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
 *     public CommandPredicate getPredicate() {
 *         return (sender, args) -&gt; {
 *             // command execution logic
 *             return true;
 *         };
 *     }
 *
 *     {@literal @}Override
 *     public String getPermission() {
 *         return "myplugin.mycommand";
 *     }
 * }</code></pre></p>
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
     * Gets the executable predicate associated with this command.
     * <p>
     * The returned {@link CommandPredicate} defines the logic that is run when the command is invoked.
     * </p>
     *
     * @return the {@link CommandPredicate} instance representing the command's behavior.
     */
    @NotNull
    CommandPredicate getPredicate();
}
