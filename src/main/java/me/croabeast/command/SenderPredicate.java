package me.croabeast.command;

import org.bukkit.command.CommandSender;

import java.util.function.BiPredicate;

/**
 * A predicate interface that tests a {@link CommandSender} against a value of type {@code T}.
 * <p>
 * This interface extends {@link BiPredicate} and is used to define conditions or filters based on
 * the properties of a {@code CommandSender} along with an additional value of type {@code T}.
 * It can be used, for example, to check if a sender meets specific criteria or to validate command arguments.
 * </p>
 *
 * @param <T> the type of the additional value to test against the {@code CommandSender}.
 * @see BiPredicate
 */
public interface SenderPredicate<T> extends BiPredicate<CommandSender, T> {}
