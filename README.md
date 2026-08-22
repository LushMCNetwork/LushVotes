# LushVotes

Vote-reward plugin for the LushMC network. Hooks NuVotifier-Velocity's own
event bus directly on the proxy - vote forwarding is deliberately off, since
NuVotifier's plugin-messaging forwarding drops a vote outright if no one's
online anywhere to relay it.

Builds **two independent artifacts**, same split as LushRelay/LushRelayBridge:

- **`LushVotes`** (root `pom.xml`) - the Velocity proxy plugin. Owns
  config, storage (SQLite), username->UUID resolution (Mojang, cached),
  the 20h dedupe window, the vote party counter, and `/lushvotes admin`.
- **`LushVotesBridge`** (`bridge/`) - Folia/Paper backend companion. Owns
  no policy of its own for rewards (runs whatever LushVotes syncs down);
  also owns `/vote` itself, since it's a real inventory GUI
  (`menus/vote_menu.yml`) and only a backend can show one. Install on any
  backend that should offer `/vote`, deliver rewards/effects, or answer
  `%lushvotes_*%` placeholders.

## Vote party

A network-wide counter (any player, any site) - once it reaches
`vote-party.target` in `config.yml`, every currently online player
(network-wide, not just wherever the triggering vote landed) is rewarded
with `vote-party.commands`, a separate list from the per-vote
`reward.commands` so a milestone can be bigger/different. The counter
resets to 0 once triggered. Offline players get nothing from a vote party -
only whoever's online the instant the target is hit.

## Offline rewards - `/vote claim`

A vote credited while the player is offline no longer delivers
automatically on their next login. It queues, and they see a one-line
reminder on join if anything's waiting - actually collecting it requires
running `/vote claim`, which pulls everything queued and runs it in one go.
A vote credited while already online still delivers (and celebrates)
immediately - only the offline case goes through `/vote claim`.

## Building

```bash
mvn package          # proxy jar -> target/LushVotes-1.0.0.jar
cd bridge && mvn package  # bridge jar -> bridge/target/LushVotesBridge-1.0.0.jar
```

Drop `LushVotes-1.0.0.jar` in the proxy's `plugins/` folder (next to
NuVotifier-Velocity), and `LushVotesBridge-1.0.0.jar` in `plugins/` on any
backend server that should deliver rewards/effects/placeholders.

**Before building**, confirm the pinned NuVotifier tag in the root
`pom.xml` still has a working jitpack build - see the comment next to
`nuvotifier.version` there. Newer NuVotifier tags have periodically failed
to build on jitpack due to an unrelated upstream Velocity-snapshot-repo
issue; the pom is pinned to the last tag confirmed to build successfully.

## Setup

1. Make sure NuVotifier-Velocity is installed and `forwarding-method` is
   `none` in its config - LushVotes reads votes straight off Velocity's
   event bus instead.
2. Edit `config.yml` in the proxy's `plugins/LushVotes/` folder:
   `reward.commands` and `vote-party.commands` should point at your primary
   currency (not gems - see the comments in `config.yml`).
3. Edit `menus/vote_menu.yml` in `plugins/LushVotesBridge/` on any backend
   offering `/vote` - the masked links inside the `vote` item's `[message]`
   actions use `[url](text)`; only `text` is ever shown to the client. The
   URL you put there doesn't need to match anything - what actually gets a
   vote recognized is the NuVotifier `[tokens]` name that site posts under,
   configured on NuVotifier itself, not in this menu.
4. `/lushvotes admin reload` after editing LushVotes' own `config.yml` - no
   restart needed, connected backends are re-synced automatically. There's
   no reload command for `vote_menu.yml` yet - restart the backend (or
   reload/replace the plugin) after editing it.

## Commands

| Command | Where | Description |
|---|---|---|
| `/vote` | Backend (LushVotesBridge) | Opens the voting menu GUI |
| `/vote claim` | Backend (LushVotesBridge) | Claims every reward queued while you were offline |
| `/lushvotes admin reload` | Proxy | Reload config, re-push to connected backends |
| `/lushvotes admin credit <player> <service>` | Proxy | Manually credit a vote, bypassing the dedupe window - the recovery path if Mojang resolution ever fails a real vote |
| `/lushvotes admin testvote <player> <service> <true\|false>` | Proxy | Simulate a vote for testing; the flag controls whether it counts toward the vote party. Bypasses dedupe (so it can be fired repeatedly), which also means it can't test dedupe itself - use two real votes for that |
| `/lushvotes admin check <player>` | Proxy | Show a player's total votes and last vote time |
| `/lushvotes admin party status` | Proxy | Show current/target vote party progress |
| `/lushvotes admin party reset` | Proxy | Reset vote party progress to 0 |
| `/lushvotes admin party set <value>` | Proxy | Set vote party progress to a specific value |

`/lushvotes admin` requires `lushvotes.admin`. Everything admin-facing lives
on the proxy - there's no separate `/vote admin`, since all the actual data
(vote history, the party counter) lives in LushVotes' own store, not on any
backend.

## Placeholders (PlaceholderAPI, backend-side only)

- `%lushvotes_total%` - all-time vote count
- `%lushvotes_total_formatted%` - comma-grouped (`1,233`)
- `%lushvotes_last%` - last vote time (UTC), or `never`
- `%lushvotes_party_current%` / `%lushvotes_party_target%` / `%lushvotes_party_remaining%` - vote party progress (network-wide, works without a player context)

## Folia safety rules

Folia runs each loaded region of the world on its own thread instead of one
global tick loop. These are the rules that keep a plugin from crashing or
corrupting state under that model - learned the hard way migrating LushRaft.

- **No `BukkitRunnable` / `Bukkit.getScheduler()`.** Use one of Folia's three
  scheduler categories instead:
  - `Bukkit.getGlobalRegionScheduler()` - pure bookkeeping only (config
    reload, DB loads, console commands). **Never touch a block or entity from
    here** - it isn't pinned to any region.
  - `Bukkit.getRegionScheduler().run(plugin, location, task -> ...)` -
    location-pinned work (block edits, area scans) at a *known* coordinate.
  - `entity.getScheduler().run(plugin, task -> ...)` - anything bound to an
    already-spawned entity (AI ticks, per-player timers). Prefer this
    whenever you already hold a live entity reference; it auto-cancels if the
    entity dies. LushVotesBridge's reward/celebration dispatch uses this.
- **Never call `Bukkit.getEntity(uuid)` (or `World#getEntities()`) from an
  arbitrary thread**, including `Bukkit.getAsyncScheduler()` timers directly.
- **`Bukkit.getPlayer(uuid)` and `Bukkit.getOnlinePlayers()` are safe reads
  from any thread** - but once you have the `Player` object, still hop onto
  `player.getScheduler().run(...)` before touching their inventory, location,
  or sending messages/sounds.
- **All Folia scheduler delay/period values must be `>= 1`.**
- **Shared mutable state needs concurrent-safe collections**
  (`ConcurrentHashMap`, etc) or `volatile`/synchronized fields.
- **`onEnable`/`onDisable` are safe for direct entity/world access.**
- **Don't relocate native-backed dependencies** (sqlite-jdbc, in the proxy
  module) in the shade plugin.
