package me.croabeast.command;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;

import java.util.function.BiPredicate;

/**
 * Represents a permissible action or command with a default implementation for permission checking.
 * <p>
 * {@code DefaultPermissible} extends the {@link Permissible} interface and provides a default
 * permission check using a {@link BiPredicate} as the checker. This default checker verifies that the
 * permission string is not blank and that the {@link CommandSender} has the specified permission.
 * </p>
 * <p>
 * Note: The {@code log} parameter in {@link #isPermitted(CommandSender, boolean)} is ignored in this implementation.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre><code>
 * public class MyDefaultCommand implements DefaultPermissible {
 *     {@literal @}Override
 *     public String getPermission() {
 *         return "myplugin.command.execute";
 *     }
 *
 *     // Optionally, override isPermitted(CommandSender, boolean) if custom behavior is needed.
 * }
 * </code></pre>
 * </p>
 *
 * @see Permissible
 * @see CommandSender
 */
@FunctionalInterface
public interface DefaultPermissible extends Permissible {

    /**
     * The default permission checker that verifies if a {@link CommandSender} has the specified permission.
     * <p>
     * This {@code BiPredicate} checks that the permission string is not blank and that the sender has the permission.
     * </p>
     */
    BiPredicate<CommandSender, String> DEFAULT_CHECKER =
            (sender, permission) -> StringUtils.isBlank(permission) && sender.hasPermission(permission);

    /**
     * Checks if the provided {@link CommandSender} has permission to execute the action or command.
     * <p>
     * This default implementation uses the {@link #DEFAULT_CHECKER} to verify whether the sender has the required permission.
     * The {@code log} parameter is ignored in this implementation.
     * </p>
     *
     * @param sender the command sender to check permissions for.
     * @param log    whether to log the permission check result (this parameter is ignored).
     * @return {@code true} if the sender has permission; {@code false} otherwise.
     */
    @Override
    default boolean isPermitted(CommandSender sender, boolean log) {
        return DEFAULT_CHECKER.test(sender, getPermission());
    }
}
