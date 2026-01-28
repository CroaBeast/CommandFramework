package me.croabeast.command;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Handles the synchronization of command changes with the Bukkit server.
 * <p>
 * When commands are dynamically registered or unregistered at runtime, the server's
 * internal command map needs to be synchronized so that changes are reflected for
 * all connected players (e.g., tab-completion updates). A {@code Synchronizer}
 * provides a mechanism to schedule and manage this synchronization.
 * </p>
 * <p>
 * The default Bukkit implementation, created via {@link #createBukkit(Plugin)}, uses
 * the {@link BukkitScheduler} to schedule synchronization on the main server thread
 * with a short delay, ensuring that multiple rapid command changes are batched into
 * a single sync operation.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * Synchronizer sync = Synchronizer.createBukkit(myPlugin);
 * // After registering or unregistering commands:
 * sync.sync();
 * // To cancel a pending synchronization:
 * sync.cancel();
 * }</pre></p>
 *
 * @see Command
 * @see BukkitCommand
 */
public interface Synchronizer {

    /**
     * Schedules a command synchronization with the server.
     * <p>
     * This method triggers the server to update its internal command state so that
     * any recently registered or unregistered commands are reflected for all players.
     * If a synchronization is already pending, implementations may cancel it before
     * scheduling a new one.
     * </p>
     */
    void sync();

    /**
     * Cancels any pending synchronization task.
     * <p>
     * If no synchronization is currently scheduled, this method has no effect.
     * </p>
     */
    void cancel();

    /**
     * Invokes the server's internal {@code syncCommands()} method via reflection.
     * <p>
     * This method uses reflection to access and call the {@code syncCommands()} method
     * on the server implementation, which updates the command dispatch tree for all
     * connected players. If the method is not available or an exception occurs,
     * the error is silently ignored.
     * </p>
     */
    static void syncCommands() {
        try {
            Server server = Bukkit.getServer();
            Method method = server.getClass().getDeclaredMethod("syncCommands");
            method.setAccessible(true);
            method.invoke(server);
        } catch (Exception ignored) {}
    }

    /**
     * Creates a new Bukkit-based {@code Synchronizer} for the specified plugin.
     * <p>
     * The returned implementation schedules synchronization on the main server thread
     * using the {@link BukkitScheduler}. It applies a 1-tick delay before executing
     * the sync, which allows multiple rapid command changes to be batched together.
     * If the plugin is disabled, any pending sync is cancelled automatically.
     * </p>
     *
     * @param plugin the {@link Plugin} to associate with the synchronizer; must not be {@code null}.
     *
     * @return a new {@code Synchronizer} instance; never {@code null}.
     *
     * @throws NullPointerException if {@code plugin} is {@code null}.
     */
    @NotNull
    static Synchronizer createBukkit(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return new Synchronizer() {

            private BukkitTask task = null;

            private void cancel0(boolean reassign) {
                if (task == null) return;

                task.cancel();
                if (reassign) task = null;
            }

            @Override
            public void sync() {
                if (!plugin.isEnabled()) {
                    cancel();
                    return;
                }

                BukkitScheduler scheduler = Bukkit.getScheduler();
                scheduler.runTask(plugin, () -> {
                    cancel0(false);

                    task = scheduler.runTaskLater(plugin, () -> {
                        task = null;
                        syncCommands();
                    }, 1L);
                });
            }

            @Override
            public void cancel() {
                cancel0(true);
            }
        };
    }
}
