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
public final class CommandBuilder extends BukkitCommand {

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

    /**
     * Constructs a new {@code CommandBuilder} with the specified plugin and command name.
     *
     * @param plugin the plugin that owns this command.
     * @param name   the name of the command.
     */
    private CommandBuilder(Plugin plugin, String name) {
        super(plugin, name);
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
        Objects.requireNonNull(consumer).accept(this);
        return this;
    }

    /**
     * Setting the command name is not supported.
     *
     * @param name the new name.
     * @return never returns normally.
     * @throws UnsupportedOperationException always thrown.
     */
    @Override
    public boolean setName(@NotNull String name) {
        throw new UnsupportedOperationException("Name can not be changed");
    }

    /**
     * Setting the command label is not supported.
     *
     * @param label the new label.
     * @return never returns normally.
     * @throws UnsupportedOperationException always thrown.
     */
    @Override
    public boolean setLabel(@NotNull String label) {
        throw new UnsupportedOperationException("Label can not be changed");
    }

    /**
     * Generates tab-completion suggestions.
     * <p>
     * This method uses the configured completions function to generate suggestions based on the sender and arguments.
     * </p>
     *
     * @param sender the command sender.
     * @param args   the command arguments.
     * @return a supplier that provides a collection of suggestion strings.
     */
    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] args) {
        return () -> completions.apply(sender, args);
    }

    /**
     * Retrieves the {@link TabBuilder} for advanced tab-completion configuration.
     *
     * @return the {@link TabBuilder} instance if set; {@code null} otherwise.
     */
    @Override
    public TabBuilder getCompletionBuilder() {
        return builder == null ? null : builder.get();
    }

    /**
     * Ensures the command is enabled and then registers it.
     * <p>
     * If the command is disabled, it is enabled before registration.
     * </p>
     *
     * @return {@code true} if registration was successful; {@code false} otherwise.
     */
    @Override
    public boolean register() {
        if (!enabled) enabled = true;
        return super.register();
    }

    /**
     * Unregisters the command and marks it as disabled.
     *
     * @return {@code true} if unregistration was successful; {@code false} otherwise.
     */
    @Override
    public boolean unregister() {
        if (enabled) enabled = false;
        return super.unregister();
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
