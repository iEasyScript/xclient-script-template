<p align="center">
  <img src="https://raw.githubusercontent.com/iEasyScript/xclient-script-template/main/.github/crest.png" width="120" alt="Project X">
</p>

<h1 align="center">Project X script template</h1>

<p align="center">
  A working script you can clone and build in one command.<br>
  <a href="https://xclient.dev/osrs/developer/docs">Developer docs</a> ·
  <a href="https://xclient.dev/osrs/developer">Developer portal</a>
</p>

---

Scripts are RuneLite plugins compiled against the [Project X client](https://github.com/iEasyScript/xclient).
You write them with one import, `net.runelite.client.plugins.projectx.bot.*`, which gives short names
like `Inventory`, `Bank`, `Walker`, `Objects` and `Npcs`:

```java
var rock = Objects.query().nameEquals("Copper rocks").nearestReachable();
if (rock != null) rock.interact("Mine");

Npcs.query("Cow").nearest().interact("Attack");
```

The full guide is [`docs/SCRIPTING.md`](https://github.com/iEasyScript/xclient/blob/main/docs/SCRIPTING.md)
in the client repo. This repository is a complete, small script you can rename and build on. It works on an object
(a tree, a rock) until the inventory is full, banks at the nearest bank, walks back and carries
on, and it shows the parts every store script should have:

- a state-machine loop that works out what to do from the game, so it can start anywhere;
- guarded steps, a recovery handler and checkpoints from `reliability()`, so it stops itself
  cleanly instead of clicking forever when something goes wrong;
- the shared stats panel (`ScriptPanel`, `SkillTracker`) every Project X script uses;
- an inventory-setup picker in its settings, loaded with `Loadout`.

Your source stays yours. The store needs your source to review a build, but only the jar is
ever given to players.

Full documentation, including an API cookbook and the common traps, is at
**[xclient.dev/osrs/developer/docs](https://xclient.dev/osrs/developer/docs)**.

## Quick start

You need **a JDK 11** installed (for example [Temurin 11](https://adoptium.net/temurin/releases/?version=11)).
Gradle uses it for the build whatever your default Java is, so it is fine to have newer ones too.

```bash
git clone https://github.com/iEasyScript/xclient-script-template.git
cd xclient-script-template
./gradlew build
```

That produces two files:

| File | What it is |
|---|---|
| `build/libs/TemplatePlugin.jar` | The script. This is what you upload |
| `build/distributions/TemplatePlugin-source.zip` | Its source, which the store asks for alongside the jar |

## Project layout

```
src/main/java/net/runelite/client/plugins/projectx/templateplugin/
  TemplatePlugin.java    lifecycle and wiring; the @PluginDescriptor is your script's identity
  TemplateScript.java    the loop, on a background thread: states, guarded steps, recovery
  TemplateConfig.java    the settings the player can change, including the inventory setup
  TemplateOverlay.java   the stats panel
build.gradle             finds the client jar, builds the jar and the source zip
gradle.properties        pluginName and the client version to compile against
```

To make it yours:

1. Rename the package and the four classes to your script's name, e.g. `MyCoolPlugin`.
2. Set `pluginName` in `gradle.properties` to the new plugin class name. The jar is named after
   it, and the client finds your script by it.
3. Change `TemplateConfig.GROUP` to something unique to your script.
4. Pick a name nothing else on the store uses. Check the
   [live catalogue](https://xclient.dev/api/v1/hub/plugins.json): if your plugin class shares a
   name with a published script, the client treats your jar as a stale copy of that one and
   replaces it.

## Building

The build compiles against the Project X client jar. It finds one in this order:

1. `-PprojectxClientPath=/path/to/projectx-1.0.79.jar`, if you pass it;
2. the copy the Project X launcher already downloaded, `~/.projectx/projectx-<version>.jar`
   (`%USERPROFILE%\.projectx\` on Windows);
3. otherwise it downloads `projectx-<version>.jar` from the
   [client's GitHub release](https://github.com/iEasyScript/xclient/releases).

`<version>` is `projectxClientVersion` in `gradle.properties`. The first line of the build says
which jar it used.

**Always build against the client players are running.** Item, object and widget ids are Java
constants, and the compiler copies their values into your classes. A jar built against an old
client keeps the old values even when it runs on a new one, which is how a script ends up
clicking the wrong widget after a game update. When a new client is released, bump
`projectxClientVersion`, rebuild, and upload a new version.

## Testing it: the dev folder

You do not have to publish to try your script. Copy the jar into

```
%USERPROFILE%\.runelite\projectx-plugins\dev\
```

(`~/.runelite/projectx-plugins/dev/` elsewhere) and restart the client. It loads from there and
stays loaded. Needs client 1.0.77 or later. Since 1.0.78 the **My Scripts** window has a Store tab
and a Local tab, and your dev-folder scripts show up under Local.

- A jar that reuses a store script's name, package or hash is refused, so give yours its own
  plugin class name.
- Restart after copying a new build over the old one.
- Do not drop test jars straight into `projectx-plugins/`: the client keeps only scripts the
  store knows about there, and switches the rest off shortly after start-up.

Also worth doing:

- **Compile against the live client.** Most mistakes in a script are calls that no longer exist
  or have a different signature, and the build catches every one of those.
- **Keep your logic easy to check.** Decide what to do from the game in one place (`decide()` in
  the template), keep each state's method short, and log state changes. The session log is in
  `~/.runelite/logs/projectx/`.
- **Submit it when it works.** Every build is reviewed before it goes live; once your first
  version is approved, each new version is a new upload on the same listing.

## Publishing to the store

1. `./gradlew build`.
2. Sign in on the [developer portal](https://xclient.dev/osrs/developer) and choose
   **Submit a script**. Upload `build/libs/<pluginName>.jar` and
   `build/distributions/<pluginName>-source.zip`.
3. **Internal name** must be your plugin class's simple name, exactly. It cannot be changed
   later.
4. **Version** must match `version` in your `@PluginDescriptor`. Each version can be uploaded
   once, so bump it for every build.
5. Set a price for 1 week, 2 weeks and 1 month in X Tokens (zero for free). You keep 70% of every
   sale.

A reviewer reads the source, then approves or rejects with a note. For later versions, use
**Upload new version** on your script's page. The portal explains pricing and payouts in full.

## Two version numbers, and which is which

They are easy to mix up, and mixing them up stops your script loading.

| | What it is | Now | Where it goes |
|---|---|---|---|
| **Client release** | What the client is called | `1.0.79` | `projectxClientVersion` in `gradle.properties` |
| **Plugin API** | What the client offers scripts | `2.7.0` | `minClientVersion` in your `@PluginDescriptor` |

`minClientVersion` is the oldest plugin API your script needs. A client that offers less will not
load the script (the log says `Plugin X needs plugin API 2.7.0 or higher`), and the plugin hub
will not install it there. Set it to the API level that added the newest thing you use:

| Plugin API | Client | Added |
|---|---|---|
| `2.6.25` | 1.0.31 | `ScriptPanel`, `ScriptPanelOverlay`, `SkillTracker` |
| `2.6.29` | 1.0.47 | `ProjectX.withDirectInput`, `Prayers.flick` / `flickQuickPrayer` |
| `2.6.30` | 1.0.49 | `BoatNavigator`; `Walker.walkTo` sails to targets at sea |
| `2.6.31` | 1.0.56 | `Script.reliability()` and the `util.reliability` package |
| `2.7.0` | 1.0.79 | The simple API: the `bot` package (`Inventory`, `Objects.query()`, `Loadout`, ...) |

The template uses the simple API, so it declares `2.7.0`. Putting a client release number such as
`1.0.79` there is wrong: as an API level it means "anything", so nothing is checked. `isExternal = true`
must be set for the check to apply at all.

## The rules that matter

**Threads.** Three threads matter, and each has one job.

| Thread | Runs | Rule |
|---|---|---|
| Your script's executor | the loop in `run()` | The only place to click, walk or wait |
| Client thread | overlays, `@Subscribe` event handlers, `clientThread.invoke(...)` | Read and return. Never click, sleep or wait |
| Swing UI thread | `startUp()`, `shutDown()`, the config panel | Start or stop the loop and return |

1. **Never click on the client thread.** A click made there is only queued and the call returns
   at once, so the `sleepUntil` after it is measuring the wrong moment and the action looks like
   it failed. Search and click from the script thread, as the loop does:
   ```java
   var tree = Objects.query("Tree").nearest();          // the search hops threads for you
   if (tree != null) tree.interact("Chop down");        // click from the script thread
   ```
2. **Read raw client state on the client thread**, with
   `ProjectX.getClientThread().runOnClientThreadOptional(() -> ...)`. The helpers (`Inventory`, `Player`,
   `Bank` and the rest) already do this for you. Avoid `clientThread.invoke(() -> someBoolean())`: a lambda that returns a
   `boolean` picks the overload that re-runs it every tick until it returns true, and returns
   nothing.
3. **Wait on conditions, not clocks:** `sleepUntil(() -> condition, timeoutMs)`.
4. **Catch everything in the loop.** An exception that escapes the scheduled lambda cancels it
   silently: the script looks like it is running and does nothing.
5. **Clean up in `shutdown()`.** Call `super.shutdown()`, then clear your own state, so the next
   start is clean.
6. **Keep `build()` and `render()` cheap.** They run every frame. No waiting, no clicking, no
   network calls (such as `GrandExchange.getPrice`).

## Common traps

- **`Bank.withdrawX(name, amount)` only matches a bank stack holding at least `amount`.** Ask
  for 28 when the bank has 20 and nothing happens. Use `Math.min(amount, Bank.count(name, true))`.
- **Name matching differs between methods.** `Inventory.hasItem("axe")` matches a pickaxe
  (partial), while `Inventory.contains("axe")` does not (exact). Most name methods have an
  overload with a `boolean exact` parameter: pass `true` unless you mean "contains".
- **`Inventory.count(name)` counts slots**, not a stack. Use `itemQuantity(name)` for coins,
  runes and other stackables.
- **The `SkillTracker` resets whenever the start time you pass changes.** Store
  `System.currentTimeMillis()` once when the script starts and pass that; never `now - runtime`.
- **Helpers set `ProjectX.status` themselves** (opening the bank, clicking an object). Keep your
  own status field for the stats panel, as the template does.
- **The old `Rs2*` names still work but are deprecated.** Use the short names: `Objects.query()`,
  `Npcs.query()`, `Inventory`, `Bank`. The full old-to-new table is in the client repo's
  `docs/api-migration.tsv`.
- **Use named ids**, such as `net.runelite.api.gameval.InterfaceID` and `ItemID`, never raw
  numbers. Game updates move widgets; the named ids move with them when you rebuild.

## Licence

BSD 2-Clause, like the client. Do what you like with this example, including keeping your own
script closed source.
