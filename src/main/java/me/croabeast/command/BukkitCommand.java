package me.croabeast.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
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
import java.lang.reflect.Method;
import java.util.*;

/**
 * An abstract base class for commands that integrates with Bukkit’s command system.
 * <p>
 * {@code BukkitCommand} extends the default Bukkit {@code Command} class and implements
 * {@link Command} and {@link DefaultPermissible} to provide a robust framework for creating,
 * registering, and unregistering commands at runtime. It supports sub-commands, dynamic permission
 * management, custom tab-completion, and error handling. This implementation is particularly designed
 * to work seamlessly on Paper forks, where dynamic command registration and unregistration can be challenging.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Generating a unique {@link NamespacedKey} for each command.</li>
 *   <li>Managing sub-commands through an internal set.</li>
 *   <li>Customizable error handling via {@code executingError} and {@code completingError} predicates.</li>
 *   <li>Dynamic addition and removal of aliases.</li>
 *   <li>Robust permission management and integration with Bukkit's permission system.</li>
 *   <li>Support for runtime registration and unregistration with proper synchronization of command permissions.</li>
 * </ul>
 * </p>
 *
 * @see Command
 * @see DefaultPermissible
 * @see PluginIdentifiableCommand
 */
public abstract class BukkitCommand extends org.bukkit.command.Command implements Command, DefaultPermissible {

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
     * The set of sub-commands associated with this command.
     */
    final Set<BaseCommand> subCommands = new LinkedHashSet<>();

    /**
     * Indicates whether this command is currently registered.
     */
    @Getter
    private boolean registered = false;

    /**
     * The executable action that is performed when the command is executed.
     */
    private Executable executable = null;

    /**
     * Predicate for handling errors during command execution.
     * <p>
     * When an exception occurs during execution, this predicate is invoked to handle the error,
     * such as logging the error and sending a message to the command sender.
     * </p>
     */
    @Getter(AccessLevel.NONE)
    @Setter @NotNull
    SenderPredicate<Throwable> executingError, completingError;

    /**
     * An internal holder for a previously loaded command that was overridden.
     */
    @Getter(AccessLevel.NONE)
    private Entry loadedCommand;

    /**
     * Predicate to handle wrong argument cases during command execution.
     * <p>
     * It is used to determine whether a provided argument is incorrect, triggering a fallback behavior.
     * </p>
     */
    @Getter(AccessLevel.NONE)
    @Setter @NotNull
    SenderPredicate<String> wrongArgumentAction;

    /**
     * Private helper class to store a reference to a previously loaded command.
     */
    private static class Entry {

        private final org.bukkit.command.Command command;
        private final Plugin plugin;
        private final String fallbackPrefix;

        /**
         * Constructs an Entry for a given command.
         *
         * @param command the command to store.
         */
        private Entry(org.bukkit.command.@NotNull Command command) {
            this.command = command;
            this.plugin = command instanceof PluginIdentifiableCommand ?
                    ((PluginIdentifiableCommand) command).getPlugin() : null;

            if (plugin != null) {
                fallbackPrefix = pluginName(plugin);
                return;
            }

            Set<String> names = new HashSet<>();
            knownCommands().forEach((k, v) -> {
                if (command.equals(v)) names.add(k);
            });
            String prefix = "";
            for (final String name : names)
                if (name.contains(":"))
                    prefix = name.split(":")[0];
            fallbackPrefix = prefix;
        }

        /**
         * Converts the plugin's name to lower case.
         *
         * @param plugin the plugin.
         * @return the plugin name in lower case.
         */
        static String pluginName(Plugin plugin) {
            return plugin.getName().toLowerCase(Locale.ENGLISH);
        }

        /**
         * Registers the stored command with the command map using the fallback prefix.
         */
        void register() {
            getMap().register(fallbackPrefix, command);
        }

        /**
         * Unregisters the stored command from the command map.
         */
        void unregister() {
            knownCommands().values().removeIf(c -> c.equals(command));
            command.unregister(getMap());
        }

        @Override
        public String toString() {
            return "Entry{command=" + command + ", plugin=" + plugin + ", prefix='" + fallbackPrefix + "'}";
        }
    }

    /**
     * Constructs a new {@code BukkitCommand} with the specified plugin, name, and permission.
     * <p>
     * Generates a unique {@link NamespacedKey} and sets up the default permission along with error handling
     * for execution and tab-completion.
     * </p>
     *
     * @param plugin     the plugin that owns this command (must not be {@code null}).
     * @param name       the name of the command.
     * @param permission the permission node for this command.
     */
    protected BukkitCommand(Plugin plugin, String name, String permission) {
        super(name);
        this.plugin = Objects.requireNonNull(plugin);
        final UUID uuid = UUID.randomUUID();
        key = new NamespacedKey(plugin, uuid.toString());
        setPermission(permission);

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
     * Constructs a new {@code BukkitCommand} with the specified plugin and command name.
     * <p>
     * The permission node is generated by concatenating the plugin's name (in lower case) with the command name.
     * </p>
     *
     * @param plugin the plugin that owns this command.
     * @param name   the name of the command.
     */
    protected BukkitCommand(Plugin plugin, String name) {
        this(plugin, name, Entry.pluginName(plugin) + '.' + name);
    }

    /**
     * Determines if the provided argument triggers the wrong argument action.
     *
     * @param sender the command sender.
     * @param arg    the argument to test.
     * @return {@code true} if the wrong argument action applies; {@code false} otherwise.
     */
    public boolean isWrongArgument(CommandSender sender, String arg) {
        return wrongArgumentAction.test(sender, arg);
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
     * New aliases are appended to the current list of aliases, ensuring that they do not duplicate the primary name.
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
     * If sub-commands are present and the first argument matches one, execution is delegated to the corresponding
     * sub-command. Otherwise, this command's executable action is invoked.
     * Any exceptions during execution are handled by the executingError predicate.
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
     * Attempts to generate completions using the custom completion builder; if an error occurs,
     * falls back to the superclass' tabComplete method.
     * </p>
     *
     * @param sender the command sender.
     * @param alias  the alias of the command.
     * @param args   the command arguments.
     * @return a list of tab completion suggestion strings.
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
     * @return the {@link Executable} representing the command's action.
     * @throws NullPointerException if the executable action is not set.
     */
    @NotNull
    public Executable getExecutable() {
        return Objects.requireNonNull(executable, "Executable action is not set");
    }

    /**
     * Sets the executable action for this command.
     *
     * @param executable the executable action.
     */
    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Sets the executable action based on a given {@link CommandPredicate}.
     *
     * @param predicate the command predicate used to generate the executable action.
     */
    public void setExecutable(CommandPredicate predicate) {
        this.executable = Executable.from(predicate);
    }

    /**
     * Sets the executable action to a constant boolean value.
     *
     * @param value the boolean value.
     */
    public void setExecutable(boolean value) {
        this.executable = Executable.from(value);
    }

    /**
     * Retrieves an unmodifiable set of sub-commands for this command.
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
     * If a sub-command with the same name already exists, the new sub-command is ignored.
     * </p>
     *
     * @param sub the sub-command to register.
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
     * Creates a new {@link Permission} using the given permission node and registers it with the plugin manager.
     * </p>
     *
     * @param perm the permission node.
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
     * @param perm the permission node.
     */
    private static void removePerm(String perm) {
        if (!StringUtils.isBlank(perm))
            Bukkit.getPluginManager().removePermission(perm);
    }

    /**
     * Loads or unloads command permissions based on the provided state.
     * <p>
     * When loading, this method removes the current command's permission and those of its sub-commands
     * (if any) from the Bukkit permission system. When unloading, it adds them back.
     * </p>
     *
     * @param loaded if {@code true}, permissions are removed; if {@code false}, they are added.
     */
    private void loadCommandPermissions(boolean loaded) {
        if (loaded) {
            removePerm(getPermission());
            if (!subCommands.isEmpty()) {
                subCommands.forEach(s -> removePerm(s.getPermission()));
                removePerm(getWildcardPermission());
            }
            return;
        }
        addPerm(getPermission());
        if (subCommands.isEmpty()) return;
        subCommands.forEach(s -> addPerm(s.getPermission()));
        addPerm(getWildcardPermission());
    }

    /**
     * Retrieves the Bukkit {@link SimpleCommandMap} from the Craft wrapper.
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
     * Retrieves the known commands from the Bukkit command system.
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

    @SneakyThrows
    static void syncCommands() {
        Server server = Bukkit.getServer();
        Method method = server
                .getClass()
                .getDeclaredMethod("syncCommands");

        method.setAccessible(true);
        method.invoke(server);
    }

    public boolean register(boolean sync) {
        if (registered || !isEnabled()) return false;

        org.bukkit.command.Command c = knownCommands().get(getName());
        if (isOverriding() && c != null)
            (loadedCommand = new Entry(c)).unregister();

        loadCommandPermissions(true);
        getMap().register(Entry.pluginName(plugin), this);

        if (sync) syncCommands();
        return registered = true;
    }

    /**
     * Registers this command with the Bukkit command map.
     * <p>
     * If the command is set to override an existing command, the existing command is unregistered and stored
     * for later re-registration upon unregistration of this command. Permissions for this command and its sub-commands
     * are also managed accordingly.
     * </p>
     *
     * @return {@code true} if the command was successfully registered; {@code false} otherwise.
     */
    @Override
    public boolean register() {
        return register(true);
    }

    @SuppressWarnings("all")
    public boolean unregister(boolean sync) {
        if (!registered || isEnabled()) return false;

        org.bukkit.command.Command c = knownCommands().get(getName());
        if (!Objects.equals(c, this)) return false;

        knownCommands().values().removeIf(c1 -> Objects.equals(c1, c));
        c.unregister(getMap());

        loadCommandPermissions(false);

        if (loadedCommand != null) {
            loadedCommand.register();
            loadedCommand = null;
        }

        if (sync) syncCommands();
        return !(registered = false);
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
        return unregister(true);
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
     * Computes the hash code for this command based on its unique key.
     *
     * @return the hash code of the command.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getKey());
    }
}
