package me.croabeast.command;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A builder for creating and configuring Bukkit commands with tab-completion support.
 * <p>
 * {@code CommandBuilder} extends {@link BukkitCommand} and allows fluent configuration of command
 * properties such as enabling/disabling the command, overriding existing commands, and setting up custom
 * tab-completion suggestions via a {@link TabBuilder}. It provides methods to set a custom completion
 * function or a static list of completions, and to supply a {@link TabBuilder} for advanced completion
 * configuration.
 * </p>
 * <p>
 * Example usage:
 * <pre><code>
 * CommandBuilder builder = CommandBuilder.from(plugin, "example")
 *     .setOverriding(true)
 *     .setCompletions((sender, args) -&gt; Arrays.asList("option1", "option2"))
 *     .setCompletionBuilder(new TabBuilder().addArgument(1, "option1"));
 *
 * // Register the command:
 * builder.register();
 *
 * // Later, to unregister:
 * builder.unregister();
 * </code></pre>
 * </p>
 *
 * @see BukkitCommand
 * @see TabBuilder
 */
@Getter
public final class CommandBuilder {

    /**
     * Flag indicating whether this command is enabled.
     */
    private boolean enabled = true;

    /**
     * Flag indicating whether this command should override an existing command.
     */
    private boolean overriding = true;

    /**
     * A function to generate tab-completion suggestions based on the command sender and arguments.
     */
    @Getter(AccessLevel.NONE)
    private BiFunction<CommandSender, String[], List<String>> completions;

    /**
     * A supplier for a {@link TabBuilder} that provides advanced tab-completion configuration.
     */
    @Getter(AccessLevel.NONE)
    private Supplier<TabBuilder> builder = null;

    private final BukkitCommand command;

    /**
     * Constructs a new {@code CommandBuilder} with the specified plugin and command name.
     *
     * @param plugin the plugin that owns this command.
     * @param name   the name of the command.
     */
    private CommandBuilder(Plugin plugin, String name) {
        command = new BukkitCommand(plugin, name) {
            @Override
            public boolean isEnabled() {
                return enabled;
            }

            @Override
            public boolean isOverriding() {
                return overriding;
            }

            @Override
            public @NotNull Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
                return () -> completions.apply(sender, arguments);
            }

            @Override
            public TabBuilder getCompletionBuilder() {
                return builder == null ? null : builder.get();
            }

            @Override
            public boolean register() {
                enabled = true;
                return super.register();
            }

            @Override
            public boolean unregister() {
                enabled = false;
                return super.unregister();
            }
        };
    }

    /**
     * Sets whether this command should override an existing command.
     *
     * @param override {@code true} to override; {@code false} otherwise.
     * @return this {@code CommandBuilder} instance for chaining.
     */
    @NotNull
    public CommandBuilder setOverriding(boolean override) {
        overriding = override;
        return this;
    }

    /**
     * Sets a custom function to generate tab-completion suggestions.
     *
     * @param function a {@link BiFunction} that takes a {@link CommandSender} and an array of arguments,
     *                 and returns a {@link List} of suggestion strings.
     * @return this {@code CommandBuilder} instance for chaining.
     */
    @NotNull
    public CommandBuilder setCompletions(BiFunction<CommandSender, String[], List<String>> function) {
        this.completions = function;
        return this;
    }

    /**
     * Sets a static list of tab-completion suggestions.
     *
     * @param completions a {@link List} of suggestion strings.
     * @return this {@code CommandBuilder} instance for chaining.
     */
    @NotNull
    public CommandBuilder setCompletions(List<String> completions) {
        this.completions = (s, a) -> completions;
        return this;
    }

    /**
     * Sets a supplier for a custom {@link TabBuilder} for advanced tab-completion configuration.
     *
     * @param builder a supplier that returns a {@link TabBuilder} instance.
     * @return this {@code CommandBuilder} instance for chaining.
     */
    @NotNull
    public CommandBuilder setCompletionBuilder(Supplier<TabBuilder> builder) {
        this.builder = builder;
        return this;
    }

    /**
     * Sets a custom {@link TabBuilder} for advanced tab-completion configuration.
     *
     * @param builder a {@link TabBuilder} instance.
     * @return this {@code CommandBuilder} instance for chaining.
     */
    @NotNull
    public CommandBuilder setCompletionBuilder(TabBuilder builder) {
        this.builder = () -> builder;
        return this;
    }

    /**
     * Applies a consumer to this {@code CommandBuilder} for further configuration.
     *
     * @param consumer a consumer that accepts a {@link BukkitCommand} for configuration.
     * @return this {@code CommandBuilder} instance for chaining.
     * @throws NullPointerException if the consumer is {@code null}.
     */
    @NotNull
    public CommandBuilder apply(@NotNull Consumer<BukkitCommand> consumer) {
        Objects.requireNonNull(consumer).accept(command);
        return this;
    }

    /**
     * Creates a new {@code CommandBuilder} for the specified plugin and command name.
     *
     * @param plugin the plugin that will own the command.
     * @param name   the name of the command.
     * @return a new {@code CommandBuilder} instance.
     */
    public static CommandBuilder from(Plugin plugin, String name) {
        return new CommandBuilder(plugin, name);
    }
}
