package me.croabeast.command;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * A builder for constructing tab-completion suggestions for commands.
 * <p>
 * {@code TabBuilder} maintains a mapping from argument indices to a set of possible tab completion
 * objects (instances of {@link Suggestion}). It allows you to add single arguments or collections of
 * arguments with associated permission and predicate checks. When the {@link #build(CommandSender, String[])}
 * method is called, it aggregates and filters the suggestions based on the current command sender and
 * input arguments.
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * TabBuilder builder = new TabBuilder();
 * builder.addArgument(1, "myplugin.command.tab", "suggestion1")
 *        .addArgument(1, "suggestion2")
 *        .addArguments(2, Arrays.asList("optionA", "optionB"));
 *
 * List<String> completions = builder.build(sender, new String[] {"firstArg", "sug"});
 * }</pre>
 * </p>
 */
public final class TabBuilder {

    /**
     * Mapping from argument index to a set of tab objects containing possible suggestions.
     */
    private final Map<Integer, Set<Suggestion<?>>> map = new LinkedHashMap<>();

    /**
     * The permission predicate used to check if a {@link CommandSender} has the required permission.
     * <p>
     * By default, this predicate uses the {@code Permissible::hasPermission} method.
     * </p>
     */
    private SenderPredicate<String> permPredicate = Permissible::hasPermission;

    /**
     * Retrieves the set of {@link Suggestion} instances stored for the given argument index.
     *
     * @param index the argument index.
     * @return the set of tab objects for the specified index, or an empty set if none exist.
     */
    private Set<Suggestion<?>> fromIndex(int index) {
        return map.getOrDefault(index, new LinkedHashSet<>());
    }

    /**
     * Sets the permission predicate used for checking if a command sender is allowed to see
     * certain tab completion suggestions.
     *
     * @param predicate the new permission predicate.
     * @return this {@code TabBuilder} instance for method chaining.
     * @throws NullPointerException if the predicate is {@code null}.
     */
    public TabBuilder setPermissionPredicate(SenderPredicate<String> predicate) {
        permPredicate = Objects.requireNonNull(predicate);
        return this;
    }

    /**
     * Adds a single argument suggestion to the builder at the specified index.
     * <p>
     * This method associates a {@link CommandPredicate} that checks conditions on the command sender and arguments,
     * and a {@link BiFunction} that generates a suggestion string.
     * </p>
     *
     * @param index     the argument index.
     * @param predicate the predicate used to validate whether the suggestion should be applied.
     * @param arg       the function that produces the suggestion string.
     * @return this {@code TabBuilder} instance for chaining.
     * @throws NullPointerException if {@code predicate} or {@code arg} is {@code null}.
     */
    private TabBuilder addArg0(int index, CommandPredicate predicate, BiFunction<CommandSender, String[], String> arg) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(arg);
        Set<Suggestion<?>> args = fromIndex(index);
        args.add(new Suggestion<String>(predicate, arg) {
            @Override
            Class<String> getType() {
                return String.class;
            }
        });
        map.put(index, args);
        return this;
    }

    /**
     * Adds a collection of argument suggestions to the builder at the specified index.
     * <p>
     * This method associates a predicate with a function that generates a collection of suggestion strings.
     * </p>
     *
     * @param index     the argument index.
     * @param predicate the predicate used to validate whether the suggestions should be applied.
     * @param arg       the function that produces a collection of suggestion strings.
     * @return this {@code TabBuilder} instance for chaining.
     * @throws NullPointerException if {@code predicate} or {@code arg} is {@code null}.
     */
    private TabBuilder addCollectionArg0(int index, CommandPredicate predicate, BiFunction<CommandSender, String[], Collection<String>> arg) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(arg);
        Set<Suggestion<?>> args = fromIndex(index);
        args.add(new Suggestion<Collection<String>>(predicate, arg) {
            @Override
            Class<Collection<String>> getType() {
                return null;
            }
        });
        map.put(index, args);
        return this;
    }

    /**
     * Adds an argument suggestion to the builder at the specified index.
     *
     * @param index     the argument index.
     * @param predicate the predicate to validate the suggestion.
     * @param argument  the function to generate the suggestion.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArgument(int index, CommandPredicate predicate, BiFunction<CommandSender, String[], String> argument) {
        return addArg0(index, predicate, argument);
    }

    /**
     * Adds a constant argument suggestion to the builder at the specified index.
     *
     * @param index     the argument index.
     * @param predicate the predicate to validate the suggestion.
     * @param argument  the constant suggestion string.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArgument(int index, CommandPredicate predicate, String argument) {
        return addArg0(index, predicate, (s, a) -> argument);
    }

    /**
     * Adds an argument suggestion with a permission check at the specified index.
     *
     * @param index      the argument index.
     * @param permission the permission node to check.
     * @param argument   the function to generate the suggestion.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArgument(int index, String permission, BiFunction<CommandSender, String[], String> argument) {
        return addArg0(index, (s, a) -> permPredicate.test(s, permission), argument);
    }

    /**
     * Adds a constant argument suggestion with a permission check at the specified index.
     *
     * @param index      the argument index.
     * @param permission the permission node to check.
     * @param argument   the constant suggestion string.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArgument(int index, String permission, String argument) {
        return addArgument(index, permission, (s, a) -> argument);
    }

    /**
     * Adds an argument suggestion without any predicate check at the specified index.
     *
     * @param index    the argument index.
     * @param argument the function to generate the suggestion.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArgument(int index, BiFunction<CommandSender, String[], String> argument) {
        return addArgument(index, (s, a) -> true, argument);
    }

    /**
     * Adds a constant argument suggestion without any predicate check at the specified index.
     *
     * @param index    the argument index.
     * @param argument the constant suggestion string.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArgument(int index, String argument) {
        return addArgument(index, (s, a) -> argument);
    }

    /**
     * Adds multiple argument suggestions (as a collection) to the builder at the specified index.
     *
     * @param index     the argument index.
     * @param predicate the predicate to validate the suggestions.
     * @param function  the function that produces a collection of suggestion strings.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArguments(int index, CommandPredicate predicate, BiFunction<CommandSender, String[], Collection<String>> function) {
        return addCollectionArg0(index, predicate, function);
    }

    /**
     * Adds multiple constant argument suggestions to the builder at the specified index.
     *
     * @param index     the argument index.
     * @param predicate the predicate to validate the suggestions.
     * @param arguments a collection of suggestion strings.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArguments(int index, CommandPredicate predicate, Collection<String> arguments) {
        return addCollectionArg0(index, predicate, (s, a) -> Objects.requireNonNull(arguments));
    }

    /**
     * Adds multiple constant argument suggestions to the builder at the specified index.
     *
     * @param index     the argument index.
     * @param predicate the predicate to validate the suggestions.
     * @param arguments an array of suggestion strings.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArguments(int index, CommandPredicate predicate, String... arguments) {
        return addArguments(index, predicate, Arrays.asList(arguments));
    }

    /**
     * Adds multiple argument suggestions without any predicate check at the specified index.
     *
     * @param index    the argument index.
     * @param function the function that produces a collection of suggestion strings.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArguments(int index, BiFunction<CommandSender, String[], Collection<String>> function) {
        return addArguments(index, (s, a) -> true, function);
    }

    /**
     * Adds multiple constant argument suggestions without any predicate check at the specified index.
     *
     * @param index     the argument index.
     * @param arguments a collection of suggestion strings.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArguments(int index, Collection<String> arguments) {
        return addArguments(index, (s, a) -> true, arguments);
    }

    /**
     * Adds multiple constant argument suggestions without any predicate check at the specified index.
     *
     * @param index     the argument index.
     * @param arguments an array of suggestion strings.
     * @return this {@code TabBuilder} instance for chaining.
     */
    public TabBuilder addArguments(int index, String... arguments) {
        return addArguments(index, (s, a) -> true, arguments);
    }

    /**
     * Checks if the builder currently has no suggestions.
     *
     * @return {@code true} if there are no suggestions; {@code false} otherwise.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Builds the list of tab completion suggestions based on the provided command sender and arguments.
     * <p>
     * This method collects suggestions from the set of {@link Suggestion} instances associated with the
     * argument index corresponding to the last argument in the provided array. It then filters the suggestions,
     * returning only those that start with the current input.
     * </p>
     *
     * @param sender the command sender for which to build the suggestions.
     * @param args   the array of command arguments.
     * @return a list of suggestion strings.
     */
    @SuppressWarnings("unchecked")
    public List<String> build(CommandSender sender, String[] args) {
        List<String> list = new LinkedList<>();

        fromIndex(args.length - 1)
                .stream()
                .filter(o -> o.predicate.test(sender, args))
                .forEach(o -> {
                    if (Objects.equals(String.class, o.getType()))
                    {
                        list.add((String)
                                o.function.apply(sender, args));
                        return;
                    }

                    list.addAll(((Collection<String>)
                            o.function.apply(sender, args)));
                });

        final String t = args[args.length - 1];
        return list.stream()
                .filter(s -> s.regionMatches(true, 0, t, 0, t.length()))
                .collect(Collectors.toList());
    }

    /**
     * An abstract representation of a tab-completion suggestion.
     * <p>
     * A {@code Suggestion} encapsulates a suggestion, along with a predicate to determine whether it should
     * be applied and a function to generate the suggestion based on the command sender and arguments.
     * </p>
     *
     * @param <T> the type of the suggestion; can be a single String or a Collection of Strings.
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static abstract class Suggestion<T> {
        final CommandPredicate predicate;
        final BiFunction<CommandSender, String[], T> function;
        abstract Class<T> getType();
    }
}
