<p align="center">
    <a href="https://discord.com/invite/gzzhVqgy3b" alt="Support Server">
        <img alt="Discord" src="https://img.shields.io/discord/826555143398752286?style=for-the-badge&logo=discord&label=Support%20Server&color=635aea">
    </a>
</p>

# Command Framework

Command Framework is a flexible and extensible command framework designed for Bukkit/Spigot and especially Paper forks. It simplifies the process of creating, registering, and managing commands dynamically at runtime—a task that can be challenging due to inherent differences and limitations between Bukkit/Spigot and Paper.

---

## Overview

The `me.croabeast.command` package offers a modular command system that provides:

- **Robust Registration/Unregistration:**  
  Seamlessly register and unregister commands at runtime even on Paper servers, where many traditional Bukkit/Spigot implementations can break. Command Framework addresses the runtime registration challenges posed by Paper.

- **Sub-Commands Support:**  
  Easily implement command hierarchies with sub-commands. Each sub-command automatically derives its permission node from its parent and can be configured with aliases.

- **Dynamic Tab Completion:**  
  Build context-sensitive tab completions with the included `TabBuilder` class. You can define both static and dynamic suggestions, and even use predicates and functions to tailor completions based on the command sender or input.

- **Permission Handling:**  
  Leverage the `Permissible` and `DefaultPermissible` interfaces for robust permission checking. The framework supports wildcard permissions and integrates neatly with the Bukkit permission system.

- **Fluent Command Creation:**  
  Use the `CommandBuilder` class for a fluent API to quickly create and configure commands, setting properties like overriding behavior, error handling, and custom tab completion strategies.

- **Integration with Bukkit/Spigot/Paper:**  
  Built on top of Bukkit’s command system, Command Framework seamlessly integrates with the server’s command map and permission system. It is particularly tuned for Paper servers where runtime command management is notoriously challenging.

---

## Key Components

### Permissible & DefaultPermissible
- **Purpose:** Define basic permission requirements for commands.
- **Usage:** Implement these interfaces to ensure that only authorized users execute commands.

### Executable
- **Purpose:** Represent the command action that is executed.
- **Usage:** Return a result using a {@code State} enum to indicate success or failure.

### BaseCommand
- **Purpose:** Establish the core structure of commands, including their names, aliases, and executable actions.
- **Usage:** Use these interfaces as the foundation for building more complex commands with sub-commands and permissions.

### Command
- **Purpose:** Extend BaseCommand and Completable to provide a complete command interface with tab completion support.
- **Usage:** Manage sub-commands and wildcard permissions automatically.

### SubCommand
- **Purpose:** Create sub-commands that are automatically linked to a parent command.
- **Usage:** Easily extend commands with additional functionalities (e.g., a "reload" sub-command) while inheriting permission settings.

### TabBuilder
- **Purpose:** Build and manage tab-completion suggestions.
- **Usage:** Configure suggestions for specific command arguments with flexible filtering and predicate-based selection.

### BukkitCommand
- **Purpose:** An abstract base class that integrates with Bukkit’s command system.
- **Usage:** Extend this class for full control over command execution, including runtime registration/unregistration—a common challenge on Paper servers.

### CommandBuilder
- **Purpose:** Provide a fluent API for constructing commands.
- **Usage:** Use CommandBuilder to quickly create commands with tab completions and error handling, perfect for Paper forks with dynamic runtime command management.

---

## Why Command Framework Works on Paper Forks

Paper forks of Bukkit/Spigot often break traditional command registration methods, especially when attempting dynamic registration and unregistration at runtime. Command Framework was built with these challenges in mind. It:

- **Supports Runtime Changes:**  
  Enables commands to be registered and unregistered at runtime without issues on Paper.

- **Handles Compatibility Issues:**  
  Includes workarounds and specialized implementations (e.g., in `BukkitCommand` and `CommandBuilder`) that account for Paper’s modifications to the command system.

- **Ensures Stability:**  
  Provides robust error handling and permission management even when underlying APIs change between Bukkit/Spigot and Paper.

---

## Usage Examples

### Example 1: Custom Command by Extending BukkitCommand

Create a custom command by extending `BukkitCommand`:

```java
package com.example.myplugin.command;

import me.croabeast.command.BukkitCommand;
import me.croabeast.command.BaseCommand;
import me.croabeast.command.Executable;
import me.croabeast.command.SubCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.command.DefaultPermissible;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class GreetCommand extends BukkitCommand implements DefaultPermissible {

    /**
     * Constructs the GreetCommand with a reference to the owning plugin.
     *
     * @param plugin the plugin instance.
     */
    public GreetCommand(Plugin plugin) {
        super(plugin, "greet");
        
        // Set up the main command executable
        setExecutable((CommandSender sender, String[] args) -> {
            sender.sendMessage("Hello, " + sender.getName() + "!");
            return Executable.State.TRUE;
        });

        // Register a sub-command "reload" with an alias "r"
        SubCommand reloadSub = new SubCommand(this, "reload;r");
        reloadSub.setExecutable((sender, args) -> {
            // Reload logic here
            sender.sendMessage("GreetCommand configuration reloaded.");
            return Executable.State.TRUE;
        });
        registerSubCommand(reloadSub);
    }

    /**
     * Provides custom tab completion for the command.
     *
     * @param sender the command sender.
     * @param alias  the command alias used.
     * @param args   the arguments passed to the command.
     * @return a list of suggestions.
     */
    @NotNull
    @Override
    public java.util.List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        // Create a simple TabBuilder for suggestions
        TabBuilder builder = new TabBuilder();
        if (args.length == 1) {
            // Suggest "hello" and "hi" when no sub-command is specified
            builder.addArgument(1, "hello");
            builder.addArgument(1, "hi");
        } else if (args.length > 1) {
            // Optionally, provide additional suggestions for further arguments
            builder.addArgument(args.length, "option1");
            builder.addArgument(args.length, "option2");
        }
        return builder.build(sender, args);
    }
}
```

#### Registering the Command

In your plugin’s main class (extending `JavaPlugin`), register the command:

```java
package com.example.myplugin;

import com.example.myplugin.command.GreetCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Create an instance of GreetCommand and register it
        GreetCommand greetCommand = new GreetCommand(this);
        greetCommand.register();
        
        // To unregister the command later, call:
        // greetCommand.unregister();
    }
}
```

### Example 2: Creating a Command with CommandBuilder

Alternatively, use the fluent API provided by `CommandBuilder`:

```java
package com.example.myplugin;

import com.example.myplugin.command.CommandBuilder;
import com.example.myplugin.command.Executable;
import com.example.myplugin.command.TabBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Create a command "example" using CommandBuilder
        CommandBuilder builder = CommandBuilder.from(this, "example")
            .setOverriding(true)
            .setCompletions((sender, args) -> Arrays.asList("optionA", "optionB", "optionC"))
            .setCompletionBuilder(new TabBuilder().addArgument(1, "optionA"))
            .apply(cmd -> cmd.setExecutable((sender, args) -> {
                sender.sendMessage("Example command executed with arguments: " + String.join(" ", args));
                return Executable.State.TRUE;
            }));

        // Register the command at runtime (works seamlessly on Paper)
        builder.register();

        // To unregister later:
        // builder.unregister();
    }
}
```

---

## Maven / Gradle Installation

To include Command Framework to the project, add the following repository and dependency to your build configuration. Replace `${version}` with the desired version tag.

### Maven

Add the repository and dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>croabeast-repo</id>
        <url>https://croabeast.github.io/repo/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>me.croabeast</groupId>
        <artifactId>CommandFramework</artifactId>
        <version>${version}</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

### Gradle

Add the repository and dependency to your `build.gradle`:

```groovy
repositories {
    maven {
        url "https://croabeast.github.io/repo/"
    }
}

dependencies {
    implementation "me.croabeast:CommandFramework:${version}"
}
```

Replace `${version}` with the appropriate module version.

---

## Conclusion

**Command Framework** (the collection of classes in the `me.croabeast.command` package) is designed to streamline command development for Minecraft plugins, particularly on Paper forks where runtime registration can be challenging. Its modular design, support for sub-commands, dynamic tab completion, and robust permission checks make it an ideal choice for modern plugin development.

With Command Framework, you can build sophisticated command hierarchies, provide context-sensitive tab completions, and manage command registration and unregistration at runtime—ensuring compatibility and stability even on the latest Paper servers.

Happy coding and enjoy building powerful commands with Command Framework!

— *CroaBeast*  
