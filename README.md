<p align="center">
  <img src="https://raw.githubusercontent.com/iEasyScript/xclient-script-template/main/.github/crest.png" width="120" alt="Project X">
</p>

<h1 align="center">Project X script template</h1>

<p align="center">
  A working script you can clone and build in one command.<br>
  <a href="https://xclient.dev">xclient.dev</a>
</p>

---

Scripts are RuneLite plugins compiled against the [Project X client](https://github.com/iEasyScript/xclient).
This repository is a minimal, working example: clone it, rename it, write your logic.

Your script's source stays yours. Only the built jar is published, so scripts sold
on the marketplace stay closed source.

## Build it

```bash
git clone https://github.com/iEasyScript/xclient-script-template.git
cd xclient-script-template
./gradlew build
```

That produces `build/libs/ExamplePlugin.jar`. The client jar it compiles against is
downloaded from [releases](https://github.com/iEasyScript/xclient/releases) — you need
nothing else installed but a JDK.

Working on the client at the same time? Point the build at your local copy:

```bash
./gradlew build -PprojectxClientPath=../xclient/runelite-client/build/libs/projectx-2.6.22.jar
```

## Run it

Drop the jar into `~/.runelite/projectx-plugins/` and start the client. It appears in the
plugin list under the name in your `@PluginDescriptor`.

For a faster loop, the client can compile and hot-reload a script without restarting —
see [`docs/AGENT_SERVER.md`](https://github.com/iEasyScript/xclient/blob/main/docs/AGENT_SERVER.md).

## What is in here

| File | |
|---|---|
| `ExamplePlugin.java` | Lifecycle and wiring. The `@PluginDescriptor` is your script's identity |
| `ExampleScript.java` | The loop, on a background thread |
| `ExampleConfig.java` | Settings the user can change |
| `ExampleOverlay.java` | On-screen status |

Rename all four to your script's name, and set `archiveBaseName` in `build.gradle` to match
your plugin class — the jar must be named after it.

## The rules that matter

1. **Never block the client thread.** `startUp()` starts the loop and returns. Game state
   that must be read on the client thread goes through
   `ProjectX.getClientThread().runOnClientThreadOptional(...)`.
2. **Wait on conditions, not clocks.** `sleepUntil(() -> condition, timeout)` rather than a
   fixed sleep.
3. **Clean up in `shutdown()`.** Cancel timers, clear static state, unregister listeners. A
   script that leaves state behind breaks on its next start, not this one.
4. **Bump `version` on every change.** The store and the hub key off it.

The full guide, including the queryable API for game state and the anti-ban helpers, is in
the client repo:
[plugin guide](https://github.com/iEasyScript/xclient/blob/main/runelite-client/src/main/java/net/runelite/client/plugins/projectx/AGENTS.md)
·
[queryable API](https://github.com/iEasyScript/xclient/blob/main/runelite-client/src/main/java/net/runelite/client/plugins/projectx/api/QUERYABLE_API.md)

## Selling it

Paid scripts are listed on [xclient.dev](https://xclient.dev): you set a price for 1 week,
2 weeks and 1 month, and keep 70% of every sale. Buyers' access is checked by the client
while the script runs, so it stops working when their time is up.

## Licence

BSD 2-Clause, like the client. Do what you like with this example, including keeping your
own script closed source.
