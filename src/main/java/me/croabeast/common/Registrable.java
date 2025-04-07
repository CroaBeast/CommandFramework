package me.croabeast.common;

/**
 * Represents a component that can be registered and unregistered within a plugin.
 * <p>
 * This interface provides methods to check if the component is registered, to register the component,
 * and to unregister the component.
 */
public interface Registrable {

    /**
     * Checks if the component is currently registered.
     * <p>
     * This method should return {@code true} if the component is active and functioning within the plugin.
     *
     * @return {@code true} if the component is enabled, {@code false} otherwise.
     */
    boolean isRegistered();

    /**
     * Registers the component.
     * <p>
     * The registration process may involve adding the component to some registry, initializing necessary components,
     * or performing any setup required for the component to function properly.
     *
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     */
    boolean register();

    /**
     * Unregisters the component.
     * <p>
     * The unregistration process may involve removing the component from some registry, cleaning up resources,
     * or performing any tear down required for the component to be properly disabled.
     *
     * @return {@code true} if the unregistration was successful, {@code false} otherwise.
     */
    boolean unregister();
}
