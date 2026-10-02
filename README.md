<div align="center">

# Crine Client

### A lightweight Minecraft Forge client focused on performance, optimization, customization, and quality-of-life improvements.

**Website:** [crine.github.io](https://crine.github.io)  
**Discord:** [Crine Community](https://dsc.gg/crinecommunity)

</div>

---

## About Crine

**Crine Client** is a Minecraft Forge-based client designed to provide a smoother and more customizable Minecraft experience.

The project focuses primarily on:

- FPS optimization and reduced frame-time spikes
- Memory and resource usage improvements
- Rendering optimizations
- Faster and cleaner user interface
- Quality-of-life features
- HUD customization
- Client-side utilities
- Improved responsiveness
- Modular Forge integration

Crine is designed as a **legit client** and does not focus on providing unfair gameplay advantages.

The goal of the project is to feel closer to a modern **modded Minecraft Forge client** rather than a traditional hacked-client framework.

---

## Performance

Crine includes and experiments with optimizations across different areas of the Minecraft client, including:

- Entity rendering
- Particle rendering
- World rendering
- GUI rendering
- Texture and resource handling
- Memory allocation
- Garbage collection pressure
- Event processing
- Client tick processing

Optimization features can be independently configured depending on the user's system and preferences.

### High FPS profile and OptiFine

Enable **FPSBoost** with **LowEnd-Mode**, **No-VSync**, and **Unlimited-FPS**.
The profile uses fast graphics, VBOs, 4 chunks, minimal particles, no clouds,
and no entity shadows. Renderers rebuild only when graphics, VBOs, lighting, or render
distance actually changes. LowEnd-Mode also disables the Interface blur/bloom.
Smooth Lighting already set to Off stays off.
Turn off individual FPSBoost options to manage those video settings yourself.

These optimizations use Forge/Minecraft APIs without requiring OptiFine classes
or replacing its chunk renderer. For a packaged client, install an OptiFine build
for **Minecraft 1.8.9** compatible with your Forge version in the launcher's
`mods` directory. Do not put OptiFine in this project's `libs` directory, which
is bundled into the Crine jar. OptiFine runtime compatibility still needs to be
checked with the exact build you use; compiling Crine alone does not verify it.

To compare performance, use the same world/server, position, camera direction,
window size, resource pack, render distance, and modules. Let chunks finish
loading, then record FPS for 60 seconds on both versions. Repeat with OptiFine
enabled, including an area with many entities. Check block/entity targeting,
creative reach, riding, FreeLook, and StreamerMode text replacement as well.
400 FPS is 2.5 ms per frame; 500 FPS requires 2.0 ms. The changes reduce CPU work
and allocations, but no 500 FPS result has been measured or guaranteed.

Build with Java 8 using `./gradlew build` (Windows: `.\gradlew.bat build`).
To keep Gradle's cache inside the workspace, add `-g .gradle-user-home`.

---

## Features

Crine's features are organized into several categories:

### Performance

Features intended to improve FPS, frame consistency, loading times, and resource usage.

### Visual

Client-side visual customization without modifying gameplay mechanics.

### Interface

HUD elements, menus, overlays, notifications, and other interface improvements.

### Utility

Small client-side quality-of-life features intended to improve the overall Minecraft experience.

### Integration

Features that improve compatibility or integration with Minecraft Forge and other supported mods.

---

## Project Structure

Crine uses its own client architecture designed around Minecraft Forge rather than the traditional hacked-client module structure.

```text
src/main/java/
└── crine/
    ├── Crine.java
    │
    ├── client/
    │   ├── CrineClient.java
    │   ├── ClientBootstrap.java
    │   └── ClientLifecycle.java
    │
    ├── config/
    │   ├── ClientConfig.java
    │   ├── ConfigManager.java
    │   └── Setting.java
    │
    ├── event/
    │   ├── EventBus.java
    │   ├── EventListener.java
    │   └── events/
    │
    ├── feature/
    │   ├── Feature.java
    │   ├── FeatureRegistry.java
    │   ├── FeatureCategory.java
    │   │
    │   ├── performance/
    │   ├── visual/
    │   ├── interface/
    │   ├── utility/
    │   └── integration/
    │
    ├── optimization/
    │   ├── render/
    │   ├── memory/
    │   ├── world/
    │   ├── entity/
    │   └── resource/
    │
    ├── ui/
    │   ├── screen/
    │   ├── hud/
    │   ├── component/
    │   └── notification/
    │
    ├── mixin/
    │
    ├── forge/
    │   ├── ForgeEventHandler.java
    │   └── ForgeIntegration.java
    │
    ├── compatibility/
    │
    └── util/
        ├── render/
        ├── math/
        ├── minecraft/
        └── system/
```

This architecture separates Crine's optimization systems, Forge integration, interface, configuration, and client features from each other to make the project easier to maintain and extend.

---

## Development

Crine is currently being rebuilt and refactored with a stronger focus on performance and maintainability.

Some parts of the project may originate from or have been inspired by previous open-source projects. These components are gradually being rewritten or reorganized into Crine's own architecture while respecting their respective licenses.

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

You are allowed to:

- Use
- Study
- Modify
- Share
- Distribute

the source code, including for commercial purposes, subject to the terms of the GPL.

If you distribute a modified version that contains GPL-licensed Crine code, you must comply with the GPL requirements, including making the corresponding source code available under the same compatible license terms.

See the [LICENSE](LICENSE) file for the complete license text.

---

<div align="center">

**Crine Client**

Performance. Customization. Simplicity.

</div>
