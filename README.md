<p align="center"><img src=".github/assets/nyalkan_wordmark.svg" width="520" alt="Nyalkan"></p>

Nyalkan swaps out Minecraft's aging OpenGL renderer for one built on **Vulkan**, made for **NeoForge** and heavily modded packs. It is a fork of [VulkanMod](https://github.com/xCollateral/VulkanMod), the Fabric mod by xCollateral.
Running Minecraft on Vulkan opens up graphics hardware features that OpenGL either can't use or barely supports. On top of that, Nyalkan adds a long list of its own optimizations aimed at higher frame rates, steadier frame times and fewer micro stutters.

> [!IMPORTANT]
> **To players:** please do not ask other mod developers to add Nyalkan support. If a mod breaks or looks wrong with Nyalkan, [open an issue](https://github.com/Silliana/Nyalkan/issues) here and I will look into it myself.
>
> **To mod developers:** hi :3 If you want to add Nyalkan support to your mod, go for it! But I am planning a proper API, so I would suggest waiting a bit for it rather than losing your sanity over internals that will change.

## Features

Some of the main Nyalkan features are:
- Reduced CPU and GPU overhead
- Terrain rendering optimizations, ranging from optimized draw loop to section and occlusion culling
- Chunk building optimizations
- Entity rendering optimizations, including model reuse and back-face culling
- Block entity rendering optimizations (baked chests, beds and more)
- Particle rendering optimizations (GPU particles)
- GUI and HUD rendering optimizations (batching and caching)
- Tick spreading to reduce frame time spikes
- Animated texture optimizations
- Native Wayland support (Linux specific)
- Windowed fullscreen mode
- NeoForge mod compatibility (works with ATM10 and ATMons, and Create works too with the [VulkanWheel](https://modrinth.com/mod/vulkanwheel) addon)

## Installation

### Download Links

<p style="text-align: left;">
    <a href="https://modrinth.com/mod/nyalkan"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg" alt="Modrinth" style="margin: 5px 5px;"></a>
    <a href="https://www.curseforge.com/projects/1747833"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg" alt="CurseForge" style="margin: 5px 5px;"></a>
</p>

You can also grab it from the [Releases](https://github.com/Silliana/Nyalkan/releases) page.

### Install guide

![NeoForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg)

1. Install the [NeoForge modloader](https://neoforged.net/) for Minecraft 1.21.1.
2. Download `Nyalkan-NeoForge-1.21.1-<version>.jar` from one of the links above.
3. Put the jar into your `.minecraft/mods` folder.

## What about shaders?

Currently they are not supported. I am looking into it, but it may take many months for shaders to be supported properly.

## Known issues

Nyalkan is still in alpha, so there are quite a few issues and visual bugs left, and you may run into crashes as well. If you do, it would be genuinely useful if you report it so I can look into it. Include your crash report or `latest.log` and your mod list if you can.

## Issues

If you found an issue that hasn't already been reported, then open a new issue in the [issue tab](https://github.com/Silliana/Nyalkan/issues).

## Building

Requirements:
- JDK 21
- Git
- Windows, Linux or macOS

Gradle 9.2.1 comes with the wrapper, so you don't need to install it.

```
git clone https://github.com/Silliana/Nyalkan.git
cd Nyalkan
./gradlew build
```

The jar ends up in `build/libs`.

## Credits

Nyalkan is based on [VulkanMod](https://github.com/xCollateral/VulkanMod) by xCollateral and its contributors, and is licensed under LGPL-3.0, like the original.
If you want to support xCollateral too, you can do that on [Ko-fi](https://ko-fi.com/xcollateral).

Thanks as well to these projects, which inspired some of Nyalkan's ideas:
- [Sodium](https://github.com/CaffeineMC/sodium) and [Sodium Extra](https://github.com/FlashyReese/sodium-extra)
- [ImmediatelyFast](https://github.com/RaphiMC/ImmediatelyFast)
- [BadOptimizations](https://github.com/imthosea/BadOptimizations)
- [Gnetum](https://github.com/decce6/Gnetum)
- [Flywheel](https://github.com/Engine-Room/Flywheel)
- [AsyncParticles](https://github.com/Harveykang/AsyncParticles)
- [stable-fps](https://github.com/lunaticbonnie/stable-fps)

---

Nyalkan is a passion project I work on in my free time. If you enjoy it and want to help out, you can support me on [Ko-fi](https://ko-fi.com/silliana) :3
