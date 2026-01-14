# VoteRewards

A Hytale plugin that implements the Votifier V2 protocol to reward players for voting on server lists.

## Features

- **Votifier V2 Protocol** - Full implementation with token-based authentication and HMAC-SHA256 signatures
- **Automatic Token Generation** - Secure 26-character alphanumeric token generated on first run
- **Configurable Rewards** - Execute any server commands as rewards with placeholder support
- **Vote Broadcasting** - Announce votes to all online players
- **Event System** - `VoteReceivedEvent` for other plugins to integrate with
- **Debug Mode** - Detailed logging for troubleshooting vote reception

## Installation

1. Download `VoteRewards-1.0.0.jar`
2. Place it in your Hytale server's `plugins` folder
3. Start/restart your server
4. Configure the plugin (see Configuration section)

## Configuration

On first run, the plugin creates `Hytalist_VoteRewards/config.json`:

```json
{
  "Port": 8192,
  "Host": "0.0.0.0",
  "Token": "b7tb4nal2sm91dt5uaeumt7d24",
  "RewardCommands": [
    "give {player} diamond 1",
    "give {player} emerald 5"
  ],
  "BroadcastVotes": true,
  "BroadcastMessage": "&a{player} &7has voted for the server! &e/vote",
  "DebugMode": false
}
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `Port` | Integer | 8192 | Port for Votifier server to listen on |
| `Host` | String | `"0.0.0.0"` | Host address to bind to (0.0.0.0 = all interfaces) |
| `Token` | String | Auto-generated | 26-character authentication token for vote sites |
| `RewardCommands` | Array | See config | Commands executed when a player votes |
| `BroadcastVotes` | Boolean | `true` | Whether to broadcast votes to all players |
| `BroadcastMessage` | String | See config | Message broadcast when someone votes |
| `DebugMode` | Boolean | `false` | Enable detailed logging for troubleshooting |

### Reward Command Placeholders

Use these placeholders in `RewardCommands`:

- `{player}` - Username of the player who voted
- `{service}` - Name of the voting service
- `{address}` - IP address of the voter
- `{timestamp}` - Unix timestamp of the vote

**Example:**
```json
"RewardCommands": [
  "give {player} diamond 1",
  "give {player} gold_ingot 5",
  "broadcast {player} voted on {service}!",
  "title {player} title &6Thank you for voting!"
]
```

## Setting Up Vote Sites

1. **Get your token** from `config.json` (auto-generated on first run)
2. **Configure your server list** with these settings:
   - **IP/Host:** Your server IP
   - **Port:** The port from config (default: server port + 1)
   - **Token/Key:** Copy the token from config.json
   - **Protocol Version:** Votifier V2

3. **Test the setup:**
   ```bash
   # Enable debug mode in config.json first
   "DebugMode": true
   ```
   Then vote on the server list and check your server logs for detailed debug output.

## For Developers

### VoteReceivedEvent

Other plugins can listen to vote events:

```java
@Override
protected void setup() {
    getEventRegistry().register(VoteReceivedEvent.class, this::onVote);
}

private void onVote(VoteReceivedEvent event) {
    Vote vote = event.getVote();

    // Get vote information
    String player = vote.getUsername();
    String service = vote.getServiceName();
    String address = vote.getAddress();
    String timestamp = vote.getTimestamp();

    // Cancel the vote (prevents default rewards)
    event.setCancelled(true);

    // Handle vote with custom logic
    // ...
}
```

### Vote Model

```java
class Vote {
    String getUsername()     // Player who voted
    String getServiceName()  // Voting service name
    String getAddress()      // Voter's IP address
    String getTimestamp()    // Vote timestamp
}
```

## Protocol Details

VoteRewards implements the **Votifier V2 Protocol** which uses:

- **Challenge-Response Authentication** - Server generates a random challenge for each connection
- **HMAC-SHA256 Signatures** - Votes are signed with the shared token
- **Binary Protocol** - Uses magic bytes `0x733a` with length-prefixed JSON messages
- **Nested JSON Structure** - Signature wraps a signed payload containing vote data

### Protocol Flow

1. Client connects to server
2. Server responds with: `VOTIFIER 2.0 <16-char-challenge>\n`
3. Client builds payload JSON with vote data + challenge
4. Client signs payload with HMAC-SHA256 using token
5. Client sends: `[magic:0x733a][length:2bytes][json]`
6. Server verifies signature and challenge
7. Server responds with: `{"status":"ok"}` or `{"status":"error"}`

## Troubleshooting

### Votes not being received

1. **Enable debug mode** in config.json
2. **Check firewall** - Ensure the Votifier port is open
3. **Verify token** - Make sure the token in config matches what you gave to vote sites
4. **Check logs** - Look for `VoteRewards` messages in server console
5. **Test connection** - Try sending a test vote using a Votifier testing tool

### Debug mode output

When `DebugMode: true`, you'll see:
```
[INFO] Sent challenge: ABC123...
[INFO] Received message: {"signature":"...","payload":"..."}
[INFO] Received vote: Vote(service=..., username=..., ...)
```

If you see warnings like:
- `Invalid signature` - Token mismatch between server and vote site
- `Challenge mismatch` - Clock sync issue or network problem
- `Invalid vote data` - Malformed vote packet

### Common Issues

**"Challenge mismatch"** - Usually means:
- Vote site is using Votifier V1 protocol (switch to V2)
- Network issues causing packet corruption
- Token is incorrect

**"Invalid signature"** - Usually means:
- Token doesn't match between config and vote site
- Token contains extra spaces or characters

**"No response from server"** - Usually means:
- Votifier port is blocked by firewall
- Server is not running
- Wrong port configured

## Building from Source

```bash
./gradlew clean jar
```

Output: `build/libs/VoteRewards-1.0.0.jar`

## Requirements

- Hytale Server v0.0.1+
- Java 17+

## License

This plugin is provided as-is for use with Hytale servers.

## Credits

- Protocol based on NuVotifier specification

## Support

For issues, questions, or contributions, please visit the GitHub repository.

---

**Note:** This plugin is compatible with standard Votifier V2 protocol used by Minecraft server lists, making it easy to integrate with existing voting platforms.
