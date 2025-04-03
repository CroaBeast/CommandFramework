package me.croabeast.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Functional interface representing an executable command action.
 * <p>
 * An {@code Executable} defines a command action that can be executed with a {@link CommandSender} and an array
 * of arguments. The result of execution is represented by the {@link State} enum, which can be converted to a boolean.
 * </p>
 * <p>
 * This interface also provides helper methods to convert an {@code Executable} into a standard Bukkit
 * {@link CommandExecutor} and to create an {@code Executable} from a {@link SenderPredicate} or a constant boolean value.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre><code>
 * // Create an executable command using a SenderPredicate
 * Executable exec = Executable.from((sender, args) -&gt; sender.hasPermission("myplugin.command"));
 *
 * // Convert to a Bukkit CommandExecutor
 * CommandExecutor executor = exec.asBukkit();
 * </code></pre>
 * </p>
 *
 * @see CommandSender
 * @see CommandExecutor
 */
@FunctionalInterface
public interface Executable {

    /**
     * Executes an action based on the provided command sender and arguments.
     *
     * @param sender    the command sender executing the action.
     * @param arguments the arguments passed to the command.
     * @return the result of the action execution as a {@link State}.
     */
    @NotNull
    Executable.State executeAction(CommandSender sender, String[] arguments);

    /**
     * Converts this {@code Executable} into a Bukkit {@link CommandExecutor}.
     * <p>
     * The returned {@link CommandExecutor} will execute the action defined by this {@code Executable}
     * and return its boolean result.
     * </p>
     *
     * @return a {@link CommandExecutor} that executes the action defined by this {@code Executable}.
     */
    @NotNull
    default CommandExecutor asBukkit() {
        return (sender, command, label, args) -> executeAction(sender, args).asBoolean();
    }

    /**
     * Creates an {@code Executable} from the given {@link CommandPredicate}.
     * <p>
     * The provided predicate will be used to determine the execution result. If the predicate test returns
     * {@code true}, the executable state is {@link State#TRUE}; otherwise, it is {@link State#FALSE}.
     * </p>
     *
     * @param predicate a {@link CommandPredicate} that tests a {@link CommandSender} and an array of arguments.
     * @return an {@code Executable} instance based on the provided predicate.
     * @throws NullPointerException if the predicate is {@code null}.
     */
    @NotNull
    static Executable from(CommandPredicate predicate) {
        Objects.requireNonNull(predicate);
        return (s, a) -> predicate.test(s, a) ? State.TRUE : State.FALSE;
    }

    /**
     * Creates an {@code Executable} that always returns the specified boolean value.
     *
     * @param value the boolean value to be returned on execution.
     * @return an {@code Executable} that always returns {@link State#TRUE} if the value is true,
     *         or {@link State#FALSE} otherwise.
     */
    @NotNull
    static Executable from(boolean value) {
        return (sender, args) -> value ? State.TRUE : State.FALSE;
    }

    /**
     * Enum representing the result state of an executable command.
     */
    enum State {
        /**
         * Indicates an undefined state.
         */
        UNDEFINED,
        /**
         * Indicates a false state.
         */
        FALSE,
        /**
         * Indicates a true state.
         */
        TRUE;

        /**
         * Converts the state to a boolean value.
         *
         * @return {@code true} if the state is {@code TRUE}; {@code false} otherwise.
         */
        public boolean asBoolean() {
            return this == TRUE;
        }
    }
}
