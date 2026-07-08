# Sit

Sit is a Paper/Purpur plugin that adds sitting, laying, spinning, crawling, and player-sitting features.

## Features

* Sit anywhere with `/sit`
* Lay down with `/lay`
* Spin with `/spin`
* Crawl with `/crawl`
* Allow players to sit on other players
* Toggle player-sitting with `/playersit`
* Configurable feature toggles
* Configurable action bar hints
* Configurable messages
* PlaceholderAPI support

## Requirements

* Java 21
* Gradle
* Paper/Purpur 26.1.x
* Paperweight userdev

## Commands

| Command       | Description                                 | Permission      |
| ------------- | ------------------------------------------- | --------------- |
| `/sit`        | Sit down                                    | `sit.use`       |
| `/lay`        | Lay down                                    | `sit.lay`       |
| `/spin`       | Start spinning                              | `sit.spin`      |
| `/crawl`      | Start crawling                              | `sit.crawl`     |
| `/playersit`  | Toggle whether other players can sit on you | `sit.playersit` |
| `/sit reload` | Reload the plugin configuration             | `sit.reload`    |

## Permissions

| Permission      | Description                                               | Default |
| --------------- | --------------------------------------------------------- | ------- |
| `sit.use`       | Allows using `/sit`                                       | `true`  |
| `sit.lay`       | Allows using `/lay`                                       | `true`  |
| `sit.spin`      | Allows using `/spin`                                      | `true`  |
| `sit.crawl`     | Allows using `/crawl`                                     | `true`  |
| `sit.playersit` | Allows using `/playersit` and player-sitting interactions | `true`  |
| `sit.reload`    | Allows using `/sit reload`                                | `op`    |

## Building

To build the plugin, run:

```bash
./gradlew clean build
```

On Windows, run:

```bat
gradlew.bat clean build
```

Build outputs are created in:

```text
build/libs/
```

## Obfuscation

This project uses ProGuard for obfuscation.

The obfuscation task is defined in:

```text
build.gradle.kts
```

To create the obfuscated jar, run:

```bash
./gradlew obfuscateJar
```

On Windows, run:

```bat
gradlew.bat obfuscateJar
```

The obfuscated jar is created in:

```text
build/libs/
```

The ProGuard mapping file is created in:

```text
build/reports/
```

The mapping file is not included in the published plugin jar.

## Configuration

The default configuration is located in:

```text
src/main/resources/config.yml
```

Server owners can configure:

* Enabled/disabled pose features
* Action bar get-up hints
* Snoring sound for laying
* Plugin messages

## Plugin Metadata

The Bukkit plugin metadata is located in:

```text
src/main/resources/plugin.yml
```

Main class:

```text
me.meadow.Sit
```
