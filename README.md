# SimpleTPA Plugin
**SimpleTPA** is a Minecraft Paper 1.21.7 plugin that implements teleport request functionality. Players can request to teleport to other players, and those requests must be explicitly accepted. Requests automatically expire after a configurable amount of time (default 2 minutes).

## Features
- Send teleport requests to other players with `/tpa <player>`
- Accept specific player teleport requests with `/tpaccept <player>`
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
- `/tpa <player>`: Sends a teleport request to the specified player
- `/tpaccept <player>`: Accepts a pending teleport request from the specified player
- `/tpdeny <player>`: Denies a pending teleport request from the specified player  
- `/tpacancel <player>`: Cancels your teleport request to the specified player
- `/tpacancel all`: Cancels all your outgoing teleport requests

## Permissions
- `simpletpa.tpa`: Allows use of the `/tpa` command (default: true)
- `simpletpa.tpaccept`: Allows use of the `/tpaccept` command (default: true)
- `simpletpa.tpdeny`: Allows use of the `/tpdeny` command (default: true)
- `simpletpa.tpacancel`: Allows use of the `/tpacancel` command (default: true)

## Usage
1. Player A sends a teleport request: `/tpa PlayerB`
2. Player B receives a notification and can accept with: `/tpaccept PlayerA`
3. If Player B doesn't respond within the configured timeout, the request expires automatically
4. Both players must be in the same dimension (Overworld, Nether, or End)

## Support Me
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/K3K715TC1R)