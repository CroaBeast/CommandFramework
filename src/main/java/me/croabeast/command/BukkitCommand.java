package me.croabeast.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiPredicate;

/**
 * An abstract base class for Bukkit commands that integrates with the plugin command system.
 * <p>
 * This class manages sub-commands, permission checking, error handling during execution and tab-completion,
 * and integrates with the command system via a {@link SimpleCommandMap}.
 * It also supports dynamic registration and unregistration of commands and their permissions.
 * </p>
 * <p>
 * Key aspects:
 * <ul>
 *     <li>A unique {@link NamespacedKey} is generated for each command.</li>
 *     <li>Sub-commands are stored in a set and can be registered via {@link #registerSubCommand(BaseCommand)}.</li>
 *     <li>Error handling is provided via {@code executingError} and {@code completingError} predicates.</li>
 *     <li>The method {@link #execute(CommandSender, String, String[])} delegates execution to sub-commands if applicable,
 *         and otherwise executes this command's own executable action.</li>
 * </ul>
 * </p>
 *
 * @see Command
 * @see DefaultPermissible
 * @see PluginIdentifiableCommand
 */
@Accessors(chain = true)
public abstract class BukkitCommand extends org.bukkit.command.defaults.BukkitCommand implements Command, DefaultPermissible {

    /**
     * The unique key for this command.
     */
    @Getter
    private final NamespacedKey key;

    /**
     * The plugin that owns this command.
     */
    @Getter
    private final Plugin plugin;

    /**
     * Flag indicating whether this command is currently registered.
     */
    @Getter
    private boolean registered = false;

    /**
     * The set of sub-commands associated with this command.
     */
    final Set<BaseCommand> subCommands = new LinkedHashSet<>();

    /**
     * The executable action to be performed when this command is invoked.
     */
    @Setter
    private Executable executable = null;

    /**
     * Predicates used to handle errors during command execution and completion.
     * These predicates are called when an error occurs and can log the error and determine further behavior.
     */
    @Getter(AccessLevel.NONE)
    @Setter
    @NotNull
    BiPredicate<CommandSender, Throwable> executingError, completingError;

    /**
     * An internal holder for a previously loaded command, used when overriding commands.
     */
    @Getter(AccessLevel.NONE)
    private Entry loadedCommand;

    /**
     * Predicate for handling wrong arguments during tab completion.
     */
    @Getter(AccessLevel.NONE)
    @Setter
    @NotNull
    BiPredicate<CommandSender, String> wrongArgumentAction;

    /**
     * Private inner class used to hold a reference to a previously loaded command.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Entry {
        @NotNull
        private final Plugin plugin;
        @NotNull
        private final org.bukkit.command.Command command;
    }

    /**
     * Constructs a new {@code BukkitCommand} with the specified plugin and command name.
     * <p>
     * Generates a unique {@link NamespacedKey} for the command using a random UUID.
     * Also initializes default error handling for execution and tab completion,
     * and sets up a default wrong-argument action.
     * </p>
     *
     * @param plugin the plugin that owns this command (must not be {@code null}).
     */
    protected BukkitCommand(Plugin plugin, String name) {
        super(name);
        this.plugin = Objects.requireNonNull(plugin);

        final UUID uuid = UUID.randomUUID();
        key = new NamespacedKey(plugin, uuid.toString());

        executingError = (s, e) -> {
            s.sendMessage(plugin.getName() + " Error executing the command " + getName());
            e.printStackTrace();
            return true;
        };

        completingError = (s, e) -> {
            s.sendMessage(plugin.getName() + " Error completing the command " + getName());
            e.printStackTrace();
            return true;
        };

        wrongArgumentAction = (s, a) -> true;
    }

    /**
     * Checks if the wrong argument action should be triggered for the given argument.
     *
     * @param sender the command sender.
     * @param arg    the argument string to check.
     * @return {@code true} if the wrong argument action applies; {@code false} otherwise.
     */
    public boolean isWrongArgument(CommandSender sender, String arg) {
        return wrongArgumentAction.test(sender, arg);
    }

    /**
     * Tests the command permission for the given sender without sending any error messages.
     *
     * @param target the command sender.
     * @return {@code true} if the sender is permitted; {@code false} otherwise.
     */
    @Override
    public boolean testPermissionSilent(@NotNull CommandSender target) {
        return isPermitted(target, false);
    }

    /**
     * Tests the command permission for the given sender.
     *
     * @param target the command sender.
     * @return {@code true} if the sender is permitted; {@code false} otherwise.
     */
    @Override
    public boolean testPermission(@NotNull CommandSender target) {
        return isPermitted(target);
    }

    /**
     * Executes this command.
     * <p>
     * If sub-commands are registered and the first argument matches a sub-command, delegates execution
     * to the sub-command's executable action. Otherwise, executes this command's executable action.
     * Error handling is performed using the {@code executingError} predicate.
     * </p>
     *
     * @param sender the command sender.
     * @param label  the alias of the command used.
     * @param args   the arguments passed to the command.
     * @return {@code true} if the command executed successfully; {@code false} otherwise.
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        boolean success;
        if (!subCommands.isEmpty() && args.length > 0) {
            BaseCommand sub = getSubCommand(args[0]);
            if (sub == null)
                return wrongArgumentAction.test(sender, args[0]);

            int last = args.length - 1;
            if (sub.isPermitted(sender)) {
                final String[] newArgs = new String[last];
                if (args.length > 1)
                    System.arraycopy(args, 1, newArgs, 0, last);
                try {
                    success = sub.getExecutable().executeAction(sender, newArgs).asBoolean();
                } catch (Throwable e) {
                    success = executingError.test(sender, e);
                }
                return success;
            }
        }
        try {
            success = getExecutable().executeAction(sender, args).asBoolean();
        } catch (Throwable e) {
            success = executingError.test(sender, e);
        }
        return success;
    }

    /**
     * Provides tab completion suggestions for this command.
     * <p>
     * This method attempts to generate completions using {@link #generateCompletions(CommandSender, String[])}
     * and a custom {@link TabBuilder}. In case of an exception, it falls back to the superclass's tabComplete.
     * </p>
     *
     * @param sender the command sender.
     * @param alias  the alias of the command.
     * @param args   the current command arguments.
     * @return a list of suggestion strings for tab completion.
     */
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        try {
            Collection<String> before = generateCompletions(sender, args).get();
            final TabBuilder builder = getCompletionBuilder();
            List<String> comps = builder != null && !builder.isEmpty() ?
                    builder.build(sender, args) :
                    before == null ? null : new ArrayList<>(before);
            return Objects.requireNonNull(comps, "Completions are null");
        } catch (Exception e) {
            completingError.test(sender, e);
            return super.tabComplete(sender, alias, args);
        }
    }

    /**
     * Retrieves the executable action associated with this command.
     *
     * @return the {@link Executable} instance representing the command's action.
     * @throws NullPointerException if the executable action is not set.
     */
    @NotNull
    public Executable getExecutable() {
        return Objects.requireNonNull(executable, "Executable action is not set");
    }

    /**
     * Retrieves an unmodifiable set of sub-commands registered with this command.
     *
     * @return a set of sub-commands.
     */
    @NotNull
    public Set<BaseCommand> getSubCommands() {
        return Collections.unmodifiableSet(subCommands);
    }

    /**
     * Registers a sub-command with this command.
     * <p>
     * If a sub-command with the same name already exists, the new sub-command is not added.
     * </p>
     *
     * @param sub the sub-command to register (must not be {@code null}).
     */
    @Override
    public void registerSubCommand(@NotNull BaseCommand sub) {
        Objects.requireNonNull(sub);
        for (BaseCommand command : subCommands)
            if (command.getName().equals(sub.getName()))
                return;
        subCommands.add(sub);
    }

    /**
     * Adds a permission to the Bukkit permission system.
     * <p>
     * This method creates a new {@link Permission} with the specified permission node
     * and registers it with the plugin manager.
     * </p>
     *
     * @param perm the permission node to add.
     */
    private static void addPerm(String perm) {
        if (StringUtils.isBlank(perm)) return;
        try {
            Permission permission = new Permission(perm);
            Bukkit.getPluginManager().addPermission(permission);
        } catch (Exception ignored) {}
    }

    /**
     * Removes a permission from the Bukkit permission system.
     *
     * @param perm the permission node to remove.
     */
    private static void removePerm(String perm) {
        if (!StringUtils.isBlank(perm))
            Bukkit.getPluginManager().removePermission(perm);
    }

    /**
     * Loads or unloads command permissions based on the provided state.
     * <p>
     * When loading, this method registers the command's permission and those of its sub-commands.
     * When unloading, it removes these permissions from the Bukkit permission system.
     * </p>
     *
     * @param cmd    the command to load or unload permissions for.
     * @param loaded if {@code true}, permissions are removed; if {@code false}, they are added.
     */
    private static void loadCommandPermissions(BukkitCommand cmd, boolean loaded) {
        Set<BaseCommand> subCommands = cmd.subCommands;
        if (loaded) {
            removePerm(cmd.getPermission());
            if (!subCommands.isEmpty()) {
                subCommands.forEach(s -> removePerm(s.getPermission()));
                removePerm(cmd.getWildcardPermission());
            }
            return;
        }
        addPerm(cmd.getPermission());
        if (subCommands.isEmpty()) return;
        subCommands.forEach(s -> addPerm(s.getPermission()));
        addPerm(cmd.getWildcardPermission());
    }

    static SimpleCommandMap getCommandMap() throws Exception {
        final Server server = Bukkit.getServer();

        Field field = server.getClass().getDeclaredField("commandMap");
        field.setAccessible(true);

        return (SimpleCommandMap) field.get(server);
    }

    @SuppressWarnings("unchecked")
    static Map<String, org.bukkit.command.Command> getKnownCommands() throws Exception {
        final SimpleCommandMap map = getCommandMap();

        Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
        field.setAccessible(true);

        return (Map<String, org.bukkit.command.Command>) field.get(map);
    }

    /**
     * Registers this command with the Bukkit command map.
     * <p>
     * If the command is set to override an existing command, the existing command is unregistered and stored
     * for later re-registration upon unregistration of this command.
     * </p>
     *
     * @return {@code true} if the command was successfully registered; {@code false} otherwise.
     */
    @Override
    public boolean register() {
        if (registered || !isEnabled()) return false;

        SimpleCommandMap map;
        Map<String, org.bukkit.command.Command> commands;
        try {
            map = getCommandMap();
            commands = getKnownCommands();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        String p = plugin.getName().toLowerCase(Locale.ENGLISH);
        String name = getName();

        org.bukkit.command.Command c = map.getCommand(name);
        if (isOverriding() && c != null) {
            commands.values().removeIf(c1 -> Objects.equals(c1, c));
            c.unregister(map);

            Plugin pl = !(c instanceof PluginIdentifiableCommand) ?
                    null :
                    ((PluginIdentifiableCommand) c).getPlugin();
            if (pl != null)
                loadedCommand = new Entry(pl, c);
        }

        loadCommandPermissions(this, true);
        map.register(p, this);
        return registered = true;
    }

    /**
     * Unregisters this command from the Bukkit command map.
     * <p>
     * If a previous command was overridden, it is re-registered after this command is unregistered.
     * </p>
     *
     * @return {@code true} if the command was successfully unregistered; {@code false} otherwise.
     */
    @Override
    public boolean unregister() {
        if (!registered || isEnabled()) return false;

        SimpleCommandMap map;
        Map<String, org.bukkit.command.Command> commands;
        try {
            map = getCommandMap();
            commands = getKnownCommands();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        org.bukkit.command.Command c = map.getCommand(getName());
        if (!Objects.equals(c, this)) return false;

        commands.values().removeIf(c1 -> Objects.equals(c1, c));
        c.unregister(map);
        loadCommandPermissions(this, false);

        if (loadedCommand != null) {
            map.register(
                    loadedCommand.plugin.getName().toLowerCase(Locale.ENGLISH),
                    loadedCommand.command
            );
            loadedCommand = null;
        }
        registered = false;
        return true;
    }

    /**
     * Checks if this command is equal to another object based on its unique key.
     *
     * @param o the object to compare.
     * @return {@code true} if the other object is a {@code BukkitCommand} with the same key; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BukkitCommand)) return false;
        BukkitCommand that = (BukkitCommand) o;
        return Objects.equals(getKey(), that.getKey());
    }

    /**
     * Computes a hash code for this command based on its unique key.
     *
     * @return the hash code of the command.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getKey());
    }
}
