# SimpleTPA Plugin
**SimpleTPA** is a Minecraft Paper 26.2 plugin that implements teleport request functionality. Although it was custom built for [minecraftoffline.net](https://www.minecraftoffline.net), any server can use it.

## Features
- Send teleport requests to other players with `/tpa <player>`
- Accept specific player teleport requests with `/tpaccept <player>`
- Ask other players to teleport to you with `/tpahere <player>`
- Deny teleport requests with `/tpdeny <player>`
- Cancel your own requests with `/tpacancel <player>` or `/tpacancel all`
- Requests expire automatically after a configurable timeout
- Configurable cooldown between sending requests to prevent spam
- Clear player messaging with request timers
- Configurable cross-world teleportation support

## Installation
1. Download the latest release [here](https://github.com/Jelly-Pudding/simpletpa/releases/latest).
2. Place the `.jar` file in your Minecraft server's `plugins` folder.
3. Restart your server.

## Configuration
In `config.yml`, you can configure:
```yaml
# Time in seconds before a teleport request expires
request-timeout: 120

# Cooldown in seconds between sending teleport requests
request-cooldown: 10

# Whether to allow cross-world teleportation
allow-cross-world: false
```

## Commands
- `/tpa <player>`: Requests to teleport to the specified player
- `/tpaccept <player>`: Accepts a pending teleport request from the specified player
- `/tpahere <player>`: Requests that the specified player teleports to you
- `/tpdeny <player>`: Denies a pending teleport request from the specified player
- `/tpacancel <player>`: Cancels your teleport request to the specified player
- `/tpacancel all`: Cancels all your outgoing teleport requests

## Permissions
- `simpletpa.tpa`: Allows use of the `/tpa` command (default: true)
- `simpletpa.tpaccept`: Allows use of the `/tpaccept` command (default: true)
- `simpletpa.tpahere`: Allows use of the `/tpahere` command (default: true)
- `simpletpa.tpdeny`: Allows use of the `/tpdeny` command (default: true)
- `simpletpa.tpacancel`: Allows use of the `/tpacancel` command (default: true)

## Example Usage
1. Player A sends a teleport request: `/tpa PlayerB`
2. Player B receives a notification and can accept with: `/tpaccept PlayerA`
3. Player A is teleported to Player B
4. If Player B doesn't respond within the configured timeout, the request expires automatically

## Support Me
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/K3K715TC1R)
