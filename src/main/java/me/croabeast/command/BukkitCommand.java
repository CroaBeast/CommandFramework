package me.croabeast.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.croabeast.common.Registrable;
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

/**
 * An abstract base class for commands that integrates with Bukkit’s command system.
 * <p>
 * {@code BukkitCommand} extends the default {@code BukkitCommand} class and implements both
 * {@link Command} and {@link DefaultPermissible} to provide a robust framework for creating,
 * registering, and unregistering commands at runtime. It supports sub-commands, dynamic permission
 * management, custom tab-completion, and error handling. This implementation is particularly designed
 * to work seamlessly on Paper forks, where runtime command registration/unregistration can be challenging.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Generating a unique {@link NamespacedKey} for each command.</li>
 *   <li>Managing sub-commands through an internal set.</li>
 *   <li>Customizable error handling via {@code executingError} and {@code completingError} predicates.</li>
 *   <li>Dynamic addition and removal of aliases.</li>
 *   <li>Robust permission management integrated with Bukkit's permission system.</li>
 *   <li>Support for runtime registration/unregistration with synchronization of command permissions.</li>
 * </ul>
 * </p>
 *
 * @see Command
 * @see DefaultPermissible
 * @see PluginIdentifiableCommand
 */
@Getter @Setter
public abstract class BukkitCommand extends org.bukkit.command.defaults.BukkitCommand implements Command, DefaultPermissible {

    /**
     * The unique key associated with this command.
     */
    private final NamespacedKey key;

    /**
     * The plugin that owns this command.
     */
    private final Plugin plugin;

    /**
     * Flag indicating whether this command is currently registered.
     */
    @Setter(AccessLevel.NONE)
    private boolean registered = false;

    /**
     * The predicate invoked when an exception occurs during command execution.
     * <p>
     * Receives the {@link CommandSender} and the thrown {@link Throwable}, and returns
     * {@code true} if the error was handled.
     * </p>
     */
    @NotNull
    SenderPredicate<Throwable> executeCheck;

    /**
     * The predicate invoked when an exception occurs during tab completion.
     * <p>
     * Receives the {@link CommandSender} and the thrown {@link Throwable}, and returns
     * {@code true} if the error was handled.
     * </p>
     */
    @NotNull
    SenderPredicate<Throwable> completeCheck;

    /**
     * The predicate invoked when a sub-command argument is not recognized.
     * <p>
     * Receives the {@link CommandSender} and the unrecognized argument string, and returns
     * {@code true} if the situation was handled.
     * </p>
     */
    @NotNull
    SenderPredicate<String> argumentCheck;

    /**
     * The predicate invoked when the console attempts to use a non-console-compatible sub-command.
     * <p>
     * Receives the {@link CommandSender} and the argument string, and returns
     * {@code true} if the situation was handled.
     * </p>
     */
    @NotNull
    SenderPredicate<String> consoleCheck;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private Entry loadedCommand;

    /**
     * The map that manages this command's sub-commands.
     */
    private SubCommandMap subCommandMap;

    /**
     * The synchronizer responsible for syncing command changes with the server.
     */
    private Synchronizer synchronizer;

    /**
     * Private helper class for storing a reference to a previously overridden command.
     */
    private static class Entry implements Registrable {

        private final org.bukkit.command.Command command;
        private final Plugin plugin;
        private final String fallbackPrefix;

        /**
         * Retrieves the Bukkit {@link SimpleCommandMap} using reflection from the server.
         *
         * @return the {@link SimpleCommandMap} instance.
         */
        @SneakyThrows
        static SimpleCommandMap getMap() {
            final Server server = Bukkit.getServer();

            Field field = server.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);

            return (SimpleCommandMap) field.get(server);
        }

        /**
         * Retrieves a map of known commands from the Bukkit command system using reflection.
         *
         * @return a map of command names to {@link org.bukkit.command.Command} instances.
         */
        @SuppressWarnings("unchecked")
        @SneakyThrows
        static Map<String, org.bukkit.command.Command> knownCommands() {
            final SimpleCommandMap map = getMap();

            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);

            return (Map<String, org.bukkit.command.Command>) field.get(map);
        }

        /**
         * Constructs an {@code Entry} for the specified command.
         *
         * @param command the command to store; must not be {@code null}
         */
        private Entry(org.bukkit.command.@NotNull Command command) {
            this.command = command;
            this.plugin = command instanceof PluginIdentifiableCommand ?
                    ((PluginIdentifiableCommand) command).getPlugin() : null;

            if (plugin != null) {
                fallbackPrefix = pluginName(plugin);
                return;
            }

            // If no plugin is associated, derive a fallback prefix from known command names.
            Set<String> names = new HashSet<>();
            knownCommands().forEach((k, v) -> { if (command.equals(v)) names.add(k); });

            String prefix = "";
            for (String name : names)
                if (name.contains(":")) prefix = name.split(":")[0];

            fallbackPrefix = prefix;
        }

        /**
         * Converts the plugin's name to lower case.
         *
         * @param plugin the plugin instance
         * @return the lower-case plugin name
         */
        static String pluginName(Plugin plugin) {
            return plugin.getName().toLowerCase(Locale.ENGLISH);
        }

        /**
         * Checks if the stored command is currently registered in the known commands map.
         *
         * @return {@code true} if the command is registered; {@code false} otherwise.
         */
        @Override
        public boolean isRegistered() {
            return knownCommands().containsValue(command);
        }

        /**
         * Registers the stored command with the command map using the fallback prefix.
         *
         * @return {@code true} if the registration was successful
         */
        public boolean register() {
            return getMap().register(fallbackPrefix, command);
        }

        /**
         * Unregisters the stored command from the command map.
         *
         * @return {@code true} if the command was successfully unregistered
         */
        public boolean unregister() {
            knownCommands().values().removeIf(c -> c.equals(command));
            return command.unregister(getMap());
        }

        /**
         * Returns a string representation of this entry, including the command,
         * plugin, and fallback prefix.
         *
         * @return a string representation of this {@code Entry}.
         */
        @Override
        public String toString() {
            return "Entry{command=" + command + ", plugin=" + plugin + ", prefix='" + fallbackPrefix + "'}";
        }
    }

    /**
     * Constructs a new {@code BukkitCommand} with the specified plugin, command name, and permission node.
     * <p>
     * This constructor initializes the command with default error handlers, a default
     * {@link SubCommandMap}, and a Bukkit-based {@link Synchronizer}. The error handlers log the
     * exception and inform the sender; the argument check defaults to always returning {@code true};
     * and the console check logs a message indicating console usage is not allowed.
     * </p>
     *
     * @param plugin     the plugin that owns this command; must not be {@code null}.
     * @param name       the primary name of the command.
     * @param permission the permission node required to execute this command.
     *
     * @throws NullPointerException if {@code plugin} is {@code null}.
     */
    public BukkitCommand(Plugin plugin, String name, String permission) {
        super(name);
        this.plugin = Objects.requireNonNull(plugin);

        key = new NamespacedKey(plugin, UUID.randomUUID().toString());
        setPermission(permission);

        subCommandMap = SubCommandMap.create();
        synchronizer = Synchronizer.createBukkit(plugin);

        executeCheck = (s, e) -> {
            s.sendMessage(plugin.getName() + " Error executing the command " + getName());
            e.printStackTrace();
            return true;
        };

        completeCheck = (s, e) -> {
            s.sendMessage(plugin.getName() + " Error completing the command " + getName());
            e.printStackTrace();
            return true;
        };

        argumentCheck = (s, a) -> true;
        consoleCheck = (s, a) -> {
            plugin.getLogger().info("Console cannot use this command!");
            return true;
        };
    }

    /**
     * Constructs a new {@code BukkitCommand} with the specified plugin and command name.
     * <p>
     * The permission node is generated by concatenating the plugin's name (in lower-case)
     * with the command name (e.g., "pluginname.commandname").
     * </p>
     *
     * @param plugin the plugin that owns this command.
     * @param name   the name of the command.
     */
    protected BukkitCommand(Plugin plugin, String name) {
        this(plugin, name, Entry.pluginName(plugin) + '.' + name);
    }

    /**
     * Tests the command permission for the given sender without logging.
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
     * Adds additional aliases to this command.
     * <p>
     * New aliases are appended to the current list of aliases,
     * ensuring that the primary name is not duplicated.
     * </p>
     *
     * @param aliases the aliases to add.
     */
    public void addAliases(String... aliases) {
        List<String> list = getAliases();
        for (String alias : aliases)
            if (!alias.equals(getName())) list.add(alias);
        setAliases(list);
    }

    /**
     * Removes specified aliases from this command.
     *
     * @param aliases the aliases to remove.
     */
    public void removeAliases(String... aliases) {
        List<String> list = getAliases();
        list.removeAll(Arrays.asList(aliases));
        setAliases(list);
    }

    /**
     * Sets the aliases for this command.
     *
     * @param aliases the new aliases.
     */
    public void setAliases(String... aliases) {
        super.setAliases(Arrays.asList(aliases));
    }

    /**
     * Executes this command.
     * <p>
     * If sub-commands exist and the first argument matches one, execution is delegated to the corresponding
     * sub-command. Otherwise, the command's own executable action is invoked.
     * Any exceptions during execution are handled via the {@code executingError} predicate.
     * </p>
     *
     * @param sender the command sender.
     * @param label  the alias of the command used.
     * @param args   the command arguments.
     * @return {@code true} if the command executed successfully; {@code false} otherwise.
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        boolean success;

        if (!subCommandMap.isEmpty() && args.length > 0) {
            BaseCommand sub = subCommandMap.get(args[0]);
            if (sub == null)
                return argumentCheck.test(sender, args[0]);

            if (!subCommandMap.isConsoleCompatible(args[0]))
                return consoleCheck.test(sender, args[0]);

            int last = args.length - 1;
            if (sub.isPermitted(sender)) {
                String[] newArgs = new String[last];
                if (args.length > 1)
                    System.arraycopy(args, 1, newArgs, 0, last);

                try {
                    success = sub.execute(sender, newArgs);
                } catch (Throwable e) {
                    success = executeCheck.test(sender, e);
                }
                return success;
            }
        }

        if (!isPermitted(sender)) return false;

        try {
            success = execute(sender, args);
        } catch (Throwable e) {
            success = executeCheck.test(sender, e);
        }

        return success;
    }

    /**
     * Provides tab completion suggestions for this command.
     * <p>
     * If a custom completion builder is defined, it is used to generate completions;
     * otherwise, the superclass' tab completion is used as a fallback. Any exceptions encountered
     * are handled by the {@code completingError} predicate.
     * </p>
     *
     * @param sender the command sender.
     * @param alias  the alias of the command.
     * @param args   the command arguments.
     * @return a list of suggestion strings for tab completion.
     */
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        try {
            Collection<String> before = generateCompletions(sender, args).get();
            TabBuilder builder = getCompletionBuilder();

            List<String> comps = builder != null && !builder.isEmpty() ?
                    builder.build(sender, args) :
                    (before == null ? null : new ArrayList<>(before));
            return Objects.requireNonNull(comps, "Completions are null");
        }
        catch (Exception e) {
            completeCheck.test(sender, e);
            return super.tabComplete(sender, alias, args);
        }
    }

    /**
     * Adds a permission to the Bukkit permission system.
     * <p>
     * Creates and registers a new {@link Permission} using the specified permission node.
     * </p>
     *
     * @param perm the permission node.
     */
    private void addPerm(String perm) {
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
    private void removePerm(String perm) {
        if (!StringUtils.isBlank(perm))
            Bukkit.getPluginManager().removePermission(perm);
    }

    /**
     * Loads or unloads command permissions from the Bukkit permission system based on the given flag.
     * <p>
     * When {@code loaded} is {@code true}, the current command's permission and its sub-commands' permissions
     * (as well as its wildcard permission) are removed from the system. When {@code loaded} is {@code false},
     * they are re-added.
     * </p>
     *
     * @param loaded if {@code true}, permissions are removed; if {@code false}, they are added back.
     */
    private void loadCommandPermissions(boolean loaded) {
        Set<BaseCommand> subs = subCommandMap.getSubCommands();

        if (loaded) {
            removePerm(getPermission());

            if (!subCommandMap.isEmpty()) {
                subs.forEach(s -> removePerm(s.getPermission()));
                removePerm(getPermission(true));
            }
            return;
        }

        addPerm(getPermission());
        if (subCommandMap.isEmpty()) return;

        subs.forEach(s -> addPerm(s.getPermission()));
        addPerm(getPermission(true));
    }

    /**
     * Registers this command with the Bukkit command map.
     * <p>
     * If a previous command is overridden, it is unregistered and stored for later re-registration.
     * Command permissions are managed accordingly. Optionally, the command system is synchronized after registration.
     * </p>
     *
     * @param sync if {@code true}, the command system is synchronized after registration.
     * @return {@code true} if the command was successfully registered; {@code false} otherwise.
     */
    public boolean register(boolean sync) {
        if (registered) return false;

        org.bukkit.command.Command c = Entry.knownCommands().get(getName());
        if (isOverriding() && c != null)
            (loadedCommand = new Entry(c)).unregister();

        loadCommandPermissions(true);
        Entry.getMap().register(Entry.pluginName(plugin), this);

        if (sync) synchronizer.sync();
        return registered = true;
    }

    /**
     * Registers this command with the Bukkit command map and synchronizes the command system.
     *
     * @return {@code true} if the command was successfully registered; {@code false} otherwise.
     */
    @Override
    public boolean register() {
        return register(true);
    }

    /**
     * Unregisters this command from the Bukkit command map.
     * <p>
     * If a previously overridden command exists, it is re-registered after this command is unregistered.
     * The command system is synchronized after unregistration.
     * </p>
     *
     * @param sync if {@code true}, the command system is synchronized after unregistration.
     * @return {@code true} if the command was successfully unregistered; {@code false} otherwise.
     */
    @SuppressWarnings("all")
    public boolean unregister(boolean sync) {
        if (!registered) return false;

        org.bukkit.command.Command c = Entry.knownCommands().get(getName());
        if (!Objects.equals(c, this)) return false;

        Entry.knownCommands().values().removeIf(c1 -> Objects.equals(c1, c));
        c.unregister(Entry.getMap());

        loadCommandPermissions(false);

        if (loadedCommand != null) {
            loadedCommand.register();
            loadedCommand = null;
        }

        if (sync) synchronizer.sync();
        return !(registered = false);
    }

    /**
     * Unregisters this command from the Bukkit command map and synchronizes the command system.
     *
     * @return {@code true} if the command was successfully unregistered; {@code false} otherwise.
     */
    @Override
    public boolean unregister() {
        return unregister(true);
    }

    /**
     * Checks whether this command is equal to another object based on its unique key.
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
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getKey());
    }
}
