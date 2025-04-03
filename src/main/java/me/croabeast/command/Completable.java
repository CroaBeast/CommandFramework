package me.croabeast.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Represents a completable action for generating tab-completion suggestions.
 * <p>
 * Implementations of {@code Completable} are responsible for providing a supplier that returns a collection
 * of suggestion strings based on the command sender and the current command arguments. This interface allows
 * for dynamic tab completion in commands.
 * </p>
 * <p>
 * Additionally, a default method {@link #getCompletionBuilder()} is provided to allow returning a custom
 * {@link TabBuilder} for more advanced completion configuration. The default implementation returns {@code null},
 * indicating that no specialized builder is used.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre><code>
 * public class MyCompletableCommand implements Completable {
 *     {@literal @}Override
 *     public Supplier&lt;Collection&lt;String&gt;&gt; generateCompletions(CommandSender sender, String[] arguments) {
 *         return () -&gt; Arrays.asList("option1", "option2", "option3");
 *     }
 *
 *     {@literal @}Override
 *     public TabBuilder getCompletionBuilder() {
 *         // Optionally return a custom TabBuilder for more advanced suggestions.
 *         return new TabBuilder();
 *     }
 * }</code></pre></p>
 *
 * @see CommandSender
 * @see TabBuilder
 */
@FunctionalInterface
public interface Completable {

    /**
     * Generates tab-completion suggestions based on the command sender and the provided arguments.
     * <p>
     * The returned {@link Supplier} should produce a collection of strings representing the possible
     * completions for the current command context.
     * </p>
     *
     * @param sender    the command sender requesting completions.
     * @param arguments the current array of command arguments.
     * @return a supplier that, when invoked, returns a collection of completion suggestions.
     */
    @NotNull
    Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments);

    /**
     * Returns a {@link TabBuilder} instance for advanced configuration of tab completions.
     * <p>
     * This method is optional and may return {@code null} if no specialized tab builder is provided.
     * </p>
     *
     * @return a {@link TabBuilder} for configuring completions, or {@code null} if not applicable.
     */
    @Nullable
    default TabBuilder getCompletionBuilder() {
        return null;
    }
}
