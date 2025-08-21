<p align="center">
    <a href="https://discord.com/invite/gzzhVqgy3b" alt="Support Server">
        <img alt="Discord" src="https://img.shields.io/discord/826555143398752286?style=for-the-badge&logo=discord&label=Support%20Server&color=635aea">
    </a>
</p>

# ⚡ Command Framework

A modern, flexible command framework for Bukkit/Spigot and especially Paper Minecraft servers.
Easily create, register, and manage commands at runtime—even on Paper, where this is usually difficult.
Easily create, register, and manage commands at runtime—even on Paper, where this is usually difficult.

---

## ✨ Features

---
- 🔄 **Dynamic Command Registration:** Register/unregister commands at runtime, even on Paper.
- 🌳 **Sub-Commands:** Build command hierarchies with permissions and aliases.
- ✨ **Tab Completion:** Context-aware suggestions with `TabBuilder`.
- 🔐 **Permissions:** Wildcard and custom permission checks.
- 🧑‍💻 **Fluent API:** Quickly build commands with `CommandBuilder`.
import me.croabeast.command.DefaultPermissible;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
## 🛠️ Quick Example

Create and register a simple command with a sub-command:

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

---

## ⚙️ Installation

**Maven:**
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

**Gradle:**
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

Replace `${version}` with the latest version.

---

## ❓ Why Use This?

- Works reliably on Paper forks where other frameworks fail.
- Makes complex command trees and tab completions easy.
- Handles permissions and registration for you.

---

## 🚀 Get Started

1. Add the dependency.
2. Extend `BukkitCommand` or create your own command class.
3. Register your command in your plugin's `onEnable()`.

---

## 🎉 Happy Coding!

Build powerful, modern commands for your Minecraft plugin with ease!
Questions? Join our [Discord](https://discord.com/invite/gzzhVqgy3b) 💬

— *CroaBeast*
