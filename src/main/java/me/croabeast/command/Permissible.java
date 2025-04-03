package me.croabeast.command;

import org.bukkit.command.CommandSender;

/**
 * Represents an action or command that is subject to permission checks.
 * <p>
 * Implementations of this interface provide a permission node that is required for executing
 * the associated action or command. It also defines methods to verify whether a given
 * {@link CommandSender} has the appropriate permissions.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre><code>
 * public class MyCommand implements Permissible {
 *    {@literal @}Override
 *     public String getPermission() {
 *         return "myplugin.mycommand";
 *     }
 *
 *    {@literal @}Override
 *     public boolean isPermitted(CommandSender sender, boolean log) {
 *         boolean permitted = sender.hasPermission(getPermission());
 *         if (log && !permitted) {
 *             sender.sendMessage("You do not have permission to perform this action.");
 *         }
 *         return permitted;
 *     }
 * }
 * </code></pre>
 * </p>
 *
 * @see CommandSender
 */
public interface Permissible {

    /**
     * Gets the permission node associated with this action or command.
     *
     * @return the permission node as a {@link String}.
     */
    String getPermission();

    /**
     * Checks if the provided {@link CommandSender} has permission to execute the action or command.
     * <p>
     * This method should perform the permission check using the permission node returned by {@link #getPermission()},
     * and optionally log the result.
     * </p>
     *
     * @param sender the command sender to check permissions for.
     * @param log    whether to log the permission check result.
     * @return {@code true} if the sender has permission; {@code false} otherwise.
     */
    boolean isPermitted(CommandSender sender, boolean log);

    /**
     * Checks if the provided {@link CommandSender} has permission to execute the action or command.
     * <p>
     * This default implementation calls {@link #isPermitted(CommandSender, boolean)} with logging enabled.
     * </p>
     *
     * @param sender the command sender to check permissions for.
     * @return {@code true} if the sender has permission; {@code false} otherwise.
     */
    default boolean isPermitted(CommandSender sender) {
        return isPermitted(sender, true);
    }
}
