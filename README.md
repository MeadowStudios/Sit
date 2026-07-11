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

* Java 25 or newer
* Gradle
* Paper/Purpur 26.2
* Paperweight userdev

Older Paper/Purpur 26.1.2 servers should use Sit v1.4.

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

## NMS

This plugin uses Paperweight userdev because it contains version-specific NMS code.

Sit v1.5 targets Paper/Purpur 26.2 and uses the NMS bridge located at:

```text
src/main/java/me/meadow/nms/v26_2/
```

## Release Jar

The production jar is created by the normal Gradle build and can be found in:

```text
build/libs/
```

For Sit v1.5, use:

```text
build/libs/Sit-1.5.jar
```

This project does not use ProGuard obfuscation.

## Plugin Metadata

The Bukkit plugin metadata is located in:

```text
src/main/resources/plugin.yml
```

Main class:

```text
me.meadow.Sit
```
