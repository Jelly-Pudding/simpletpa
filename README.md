# SimpleTPA Plugin
**SimpleTPA** is a Minecraft Paper 1.21.4 plugin that implements teleport request functionality. Players can request to teleport to other players, and those requests must be explicitly accepted. Requests automatically expire after a configurable amount of time (default 2 minutes).

## Features
- Send teleport requests to other players with `/tpa <player>`
- Accept specific player teleport requests with `/tpaccept <player>`
- Requests expire automatically after a configurable timeout
- Configurable cooldown between sending requests to prevent spam
- Tab completion for both commands
- Clear player messaging with request timers
- Permissions-based access control (op-only by default)

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
```

## Commands
- `/tpa <player>`: Sends a teleport request to the specified player
- `/tpaccept <player>`: Accepts a pending teleport request from the specified player

## Permissions
- `simpletpa.tpa`: Allows use of the `/tpa` command (default: op)
- `simpletpa.tpaccept`: Allows use of the `/tpaccept` command (default: op)

## Usage
1. Player A sends a teleport request: `/tpa PlayerB`
2. Player B receives a notification and can accept with: `/tpaccept PlayerA`
3. If Player B doesn't respond within the configured timeout, the request expires automatically

## Support Me
Donations will help me with the development of this project.

One-off donation: https://ko-fi.com/lolwhatyesme

Patreon: https://www.patreon.com/lolwhatyesme 