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

That produces `build/libs/TemplatePlugin.jar`. The client jar it compiles against is
downloaded from [releases](https://github.com/iEasyScript/xclient/releases) — you need
nothing else installed but a JDK.

Working on the client at the same time? Point the build at your local copy:

```bash
./gradlew build -PprojectxClientPath=../xclient/runelite-client/build/libs/projectx-1.0.5.jar
```

## Run it on your own machine

Nothing needs publishing to test a script. The client side-loads every jar in its plugin
folder at start-up, so your build is one copy away from running.

```bash
./gradlew build
cp build/libs/TemplatePlugin.jar ~/.runelite/projectx-plugins/
```

On Windows that folder is `%USERPROFILE%\.runelite\projectx-plugins\`.

Start the client and your script is in the plugin list under the name in its
`@PluginDescriptor`. Each rebuild is the same three steps: build, copy over the old jar,
restart the client.

**The jar's filename must be your plugin class's simple name.** The client works out what a
side-loaded jar contains from its filename, so `TemplatePlugin.jar` must hold
`TemplatePlugin`. That is what `archiveBaseName` in `build.gradle` sets, and getting it wrong
is the usual reason a script never appears.

A few things worth knowing while you iterate:

- **A jar that fails to load is not retried** until its bytes change. That is deliberate --
  it stops a broken build being fetched over and over -- and a rebuild changes the bytes, so
  it only bites if you restart the client without rebuilding.
- **Paid scripts are not gated locally.** Access is only checked for scripts the store
  knows about. Yours is not one until you publish it, so it simply runs.
- **Attach a debugger and the API version check is skipped**, which is convenient while the
  numbers are in flux. Without one, the check applies as it would for anyone else.
- **Do not leave jars in that folder.** It is managed: the client tidies away anything that
  is not in its catalogue after three days.
- **Pick a name nothing else uses.** If your plugin class shares its name with something
  already published, the client treats your jar as a stale copy of that one, and replaces it
  with the published build. Your code vanishes and the log talks about a hash mismatch.
  Check the [catalogue](https://xclient.dev/api/v1/hub/plugins.json) before settling on a
  name.

For a faster loop, the client can compile and hot-reload a script without restarting —
see [`docs/AGENT_SERVER.md`](https://github.com/iEasyScript/xclient/blob/main/docs/AGENT_SERVER.md).

## Two version numbers, and which is which

They are easy to mix up, and mixing them up stops your script loading.

| | What it is | Where it goes |
|---|---|---|
| **Client release** | What the client is called. Currently `1.0.5` | `projectxClientVersion` in `gradle.properties` |
| **Plugin API** | What the client offers scripts. Currently `2.6.22` | `minClientVersion` in your `@PluginDescriptor` |

They are separate on purpose. The client was renumbered to 1.x without the API changing, so
tying the two together would have declared every existing script incompatible overnight.

Set `projectxClientVersion` to the current release so you compile against the real thing.
Leave `minClientVersion` alone unless you use something added to the API after 2.6.22.
Putting a client release number there says nothing, and putting a number above the API the
client provides stops your script loading, with a line in the log saying so.

## What is in here

| File | |
|---|---|
| `TemplatePlugin.java` | Lifecycle and wiring. The `@PluginDescriptor` is your script's identity |
| `TemplateScript.java` | The loop, on a background thread |
| `TemplateConfig.java` | Settings the user can change |
| `TemplateOverlay.java` | On-screen status |

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
