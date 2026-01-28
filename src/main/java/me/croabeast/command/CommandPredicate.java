package me.croabeast.command;

/**
 * A predicate interface for testing command arguments.
 * <p>
 * This interface specializes {@link SenderPredicate} for the case where the additional value is an array
 * of {@code String} objects. It is typically used to verify conditions or constraints on the arguments of a command
 * as provided by a {@link org.bukkit.command.CommandSender}.
 * </p>
 *
 * @see SenderPredicate
 */
public interface CommandPredicate extends SenderPredicate<String[]> {}
