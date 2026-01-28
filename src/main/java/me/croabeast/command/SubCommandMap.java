package me.croabeast.command;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manages a collection of {@link BaseCommand sub-commands} for a parent command.
 * <p>
 * A {@code SubCommandMap} provides methods for adding, removing, and retrieving sub-commands
 * by name or alias. It also supports console compatibility filtering, allowing certain
 * sub-command arguments to be restricted or allowed when executed from the server console.
 * </p>
 * <p>
 * Instances of this interface can be created using the {@link #create(String...)} factory method,
 * which returns a default implementation backed by a {@link java.util.LinkedHashMap}.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * SubCommandMap map = SubCommandMap.create("reload", "status");
 * map.add(mySubCommand);
 *
 * BaseCommand found = map.get("reload");
 * boolean console = map.isConsoleCompatible("reload");
 * }</pre></p>
 *
 * @see BaseCommand
 * @see Command
 */
public interface SubCommandMap {

    /**
     * Gets all sub-commands currently registered in this map.
     *
     * @return a {@link Set} containing all registered {@link BaseCommand sub-commands}.
     */
    @NotNull
    Set<BaseCommand> getSubCommands();

    /**
     * Adds a sub-command to this map.
     * <p>
     * If a sub-command with the same name already exists, it will be replaced.
     * </p>
     *
     * @param command the {@link BaseCommand} to add; must not be {@code null}.
     *
     * @throws NullPointerException if {@code command} is {@code null}.
     */
    void add(BaseCommand command);

    /**
     * Removes a sub-command from this map by its name.
     *
     * @param name the name of the sub-command to remove; must not be empty.
     *
     * @throws IllegalArgumentException if {@code name} is empty.
     */
    void remove(String name);

    /**
     * Retrieves a sub-command by its name or alias.
     * <p>
     * This method searches through all registered sub-commands and checks both
     * the primary name and aliases for a match.
     * </p>
     *
     * @param name the name or alias of the sub-command to find.
     *
     * @return the matching {@link BaseCommand}, or {@code null} if no match is found.
     */
    @Nullable
    default BaseCommand get(String name) {
        if (name == null || name.isEmpty()) return null;

        for (BaseCommand command : getSubCommands()) {
            Set<String> names = new HashSet<>(command.getAliases());
            names.add(command.getName());
            if (names.contains(name)) return command;
        }

        return null;
    }

    /**
     * Gets the array of argument names that are allowed when the command is executed from the console.
     *
     * @return an array of console-compatible argument names; never {@code null}.
     */
    @NotNull
    String[] getConsoleArguments();

    /**
     * Sets the array of argument names that are allowed when the command is executed from the console.
     *
     * @param arguments the console-compatible argument names to set; must not be {@code null}.
     */
    void setConsoleArguments(@NotNull String... arguments);

    /**
     * Checks whether the given argument is compatible with console execution.
     * <p>
     * An argument is considered console-compatible if it is not blank and is present
     * in the array returned by {@link #getConsoleArguments()}.
     * </p>
     *
     * @param argument the argument to check.
     *
     * @return {@code true} if the argument is console-compatible; {@code false} otherwise.
     */
    default boolean isConsoleCompatible(String argument) {
        return StringUtils.isNotBlank(argument) && ArrayUtils.contains(getConsoleArguments(), argument);
    }

    /**
     * Checks whether this map contains no sub-commands.
     *
     * @return {@code true} if there are no sub-commands registered; {@code false} otherwise.
     */
    default boolean isEmpty() {
        return this.getSubCommands().isEmpty();
    }

    /**
     * Creates a new {@code SubCommandMap} instance with the specified console-compatible arguments.
     * <p>
     * The returned implementation uses a {@link java.util.LinkedHashMap} to maintain insertion
     * order of sub-commands.
     * </p>
     *
     * @param consoleArguments optional argument names that should be allowed for console execution;
     *                         may be {@code null} or empty.
     *
     * @return a new {@code SubCommandMap} instance; never {@code null}.
     */
    @NotNull
    static SubCommandMap create(@Nullable String... consoleArguments) {
        return new SubCommandMap() {

            private String[] checkArray(String... args) {
                return args != null && args.length > 0 ? args : new String[0];
            }

            private final Map<String, BaseCommand> subCommands = new LinkedHashMap<>();
            private String[] arguments = checkArray(consoleArguments);

            @NotNull
            public Set<BaseCommand> getSubCommands() {
                return new HashSet<>(subCommands.values());
            }

            @Override
            public void add(BaseCommand command) {
                Objects.requireNonNull(command, "Command cannot be null");
                subCommands.put(command.getName(), command);
            }

            @Override
            public void remove(String name) {
                Preconditions.checkArgument(StringUtils.isNotEmpty(name), "Command cannot be empty");
                subCommands.remove(name);
            }

            @NotNull
            public String[] getConsoleArguments() {
                return checkArray(arguments);
            }

            @Override
            public void setConsoleArguments(@NotNull String... arguments) {
                this.arguments = checkArray(arguments);
            }
        };
    }
}
