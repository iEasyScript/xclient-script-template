package net.runelite.client.plugins.projectx.templateplugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.bot.Bank;
import net.runelite.client.plugins.projectx.bot.Inventory;
import net.runelite.client.plugins.projectx.bot.Loadout;
import net.runelite.client.plugins.projectx.bot.Objects;
import net.runelite.client.plugins.projectx.bot.Player;
import net.runelite.client.plugins.projectx.bot.Walker;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import net.runelite.client.plugins.projectx.util.reliability.ActionResult;
import net.runelite.client.plugins.projectx.util.reliability.Checkpoint;
import net.runelite.client.plugins.projectx.util.reliability.Preconditions;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * The loop: work on an object until the inventory is full, bank, walk back, repeat.
 *
 * <p>It runs on the script's own background executor, never on the client thread, so it is
 * allowed to wait: {@code sleepUntil}, walking and guarded steps all block this thread and
 * nothing else.
 *
 * <p>The shape to copy:
 * <ol>
 *   <li>A small {@link State} enum and one method per state. Each pass of the loop does one
 *       state's worth of work and returns; the next pass picks up from there.</li>
 *   <li>{@link #decide()} works out the state from the game, not from memory, so the script can
 *       be started anywhere and can recover from anything by asking again.</li>
 *   <li>Every action that can fail goes through {@code reliability().step(...)}: it is retried,
 *       then recovered, and if it keeps failing the script stops itself and tells the player
 *       why, instead of clicking forever.</li>
 * </ol>
 */
@Slf4j
public class TemplateScript extends Script {

    /** How far from the start tile still counts as "at the work spot". */
    private static final int WORK_RADIUS = 12;

    enum State {
        /** Loading the chosen inventory setup from the bank, once, before starting. */
        PREPARING,
        /** At the work spot, working on the object. */
        WORKING,
        /** Inventory full: banking at the nearest bank. */
        BANKING,
        /** Walking back to where the run started. */
        RETURNING
    }

    // Read by the overlay on the client thread every frame, written here on the script thread:
    // volatile so the overlay always sees the latest value.
    private volatile State state = State.PREPARING;
    private volatile String status = "Starting";
    private volatile long startedAt;
    private volatile int trips;

    private TemplateConfig config;
    private WorldPoint workSpot;
    private boolean prepared;

    public boolean run(TemplateConfig config) {
        this.config = config;
        startedAt = System.currentTimeMillis();
        trips = 0;
        workSpot = null;
        prepared = false;
        state = State.PREPARING;

        // Set up reliability once, before the loop. Both settings survive shutdown(), so a
        // script that is stopped and started again keeps them.
        reliability()
                // The name the player sees if the script ever has to stop itself.
                .named("Template Script")
                // What "getting somewhere" looks like. If this has not changed for the player's
                // no-progress timeout (Project X settings > Script reliability), the recovery
                // handler below runs once, and the script stops if that does not help.
                .trackProgress(Inventory::fullSlotCount)
                // What to do when a step has used all its retries, or nothing is progressing:
                // put the game back into a known state. Return true if that worked.
                .onRecover(ctx -> {
                    log.info("Recovering after '{}': {}", ctx.getStep(), ctx.getReason());
                    if (Bank.isOpen()) {
                        Bank.closeBank();
                    }
                    Checkpoint last = ctx.getCheckpoint();
                    WorldPoint back = last != null && last.getLocation() != null ? last.getLocation() : workSpot;
                    boolean ok = back == null || Walker.walkTo(back, 3);
                    state = decide();
                    return ok;
                });

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // 1. super.run() handles pausing, breaks, login and level-up screens, random
                //    events and run energy. When it says no, do nothing this pass.
                // 2. reliability().tick() counts time without progress and returns false once
                //    the script has been stopped. Call it once per pass, after super.run().
                if (!super.run() || !ProjectX.isLoggedIn() || !reliability().tick()) {
                    return;
                }

                if (workSpot == null) {
                    // The first pass in game: remember where the player started, as both the
                    // work spot and the first checkpoint a recovery can walk back to.
                    workSpot = Player.getWorldLocation();
                    reliability().checkpoint("Started", workSpot);
                }

                switch (state) {
                    case PREPARING:
                        prepare();
                        break;
                    case WORKING:
                        work();
                        break;
                    case BANKING:
                        bank();
                        break;
                    case RETURNING:
                        walkBack();
                        break;
                }
            } catch (Exception e) {
                // An exception that escapes this lambda cancels the schedule silently: the
                // script looks like it is running and does nothing. Always catch and log.
                log.error("Template script loop failed", e);
            }
        }, 0, 600, TimeUnit.MILLISECONDS); // 600 ms is one game tick
        return true;
    }

    /** Works out what to do next from what is actually on screen. */
    private State decide() {
        if (!prepared && config.inventorySetup() != null) return State.PREPARING;
        if (Inventory.isFull()) return State.BANKING;
        if (workSpot != null && !Player.isInArea(workSpot, WORK_RADIUS)) return State.RETURNING;
        return State.WORKING;
    }

    private void prepare() {
        InventorySetup chosen = config.inventorySetup();
        if (chosen == null) {
            prepared = true;
            state = decide();
            return;
        }

        status = "Loading " + chosen.getName();
        // Loadout wraps a saved inventory setup. Passing mainScheduledFuture lets it stop
        // part-way if the script is switched off while it is withdrawing.
        Loadout setup = new Loadout(chosen, mainScheduledFuture);

        if (!setup.doesEquipmentMatch() || !setup.doesInventoryMatch()) {
            ActionResult loaded = reliability().step("Load inventory setup")
                    // Opens the nearest bank, then withdraws and wears what the setup needs.
                    .action(() -> Bank.walkToBankAndUseBank() && setup.loadEquipment() && setup.loadInventory())
                    .verify(() -> setup.doesEquipmentMatch() && setup.doesInventoryMatch(), 5_000)
                    .run();
            if (!loaded.isSuccess()) {
                // Retried, recovered or stopped already. If the script is stopping, return
                // straight away; otherwise try again next pass.
                return;
            }
            Bank.closeBank();
        }

        prepared = true;
        reliability().checkpointAtPlayer("Inventory setup loaded");
        state = decide();
    }

    private void work() {
        if (Inventory.isFull()) {
            state = State.BANKING;
            return;
        }
        // Still busy with the last click: let it finish. Clicking again would reset it.
        if (Player.isAnimating() || Player.isMoving()) {
            return;
        }

        String name = config.objectName().trim();
        String action = config.action().trim();
        if (name.isEmpty() || action.isEmpty()) {
            // A safe stop: the player gets a message saying why, and shutdown() runs.
            reliability().stop("Set the object and the action in the script's settings first.");
            return;
        }

        // Search, then click, both from this script thread. Search again every pass rather
        // than keeping a tree or rock from an earlier one: the scene changes every tick.
        var target = Objects.query()
                .nameEquals(name)
                .within(workSpot, WORK_RADIUS)
                .nearestReachable();
        if (target == null) {
            // Nothing there right now: trees and rocks respawn, so wait rather than count it
            // as a failure. If it never comes back, the no-progress timeout notices.
            status = "Waiting for " + name;
            return;
        }

        status = action + " " + name;
        reliability().step(action + " " + name)
                // Checked before every attempt. An unmet precondition fails the step without
                // running the action; add .fatal() to one to stop the script instead.
                .require(Preconditions.inventoryNotFull())
                // Return false when the click could not even be made.
                .action(() -> target.interact(action))
                // Worked if the player starts moving to it or doing it within 3 seconds.
                .verify(() -> Player.isAnimating() || Player.isMoving(), 3_000)
                .maxRetries(2)
                .run();
    }

    private void bank() {
        status = "Banking";

        ActionResult open = reliability().step("Open the bank")
                .action(Bank::walkToBankAndUseBank)
                .verify(Bank::isOpen, 5_000)
                .run();
        if (!open.isSuccess()) {
            return;
        }

        InventorySetup chosen = config.inventorySetup();
        String[] keep = keepItems();
        ActionResult deposit = reliability().step("Deposit")
                .require(Preconditions.bankOpen())
                .action(() -> chosen != null
                        // Keep everything the setup holds, matched the way the setup says.
                        ? Bank.depositAllExcept(new Loadout(chosen, mainScheduledFuture).itemsToNotDeposit())
                        : keep.length == 0 ? Bank.depositAll() : Bank.depositAllExcept(keep))
                .verify(() -> !Inventory.isFull(), 3_000)
                .run();
        if (!deposit.isSuccess()) {
            return;
        }

        trips++;
        // A checkpoint marks the last step known to have worked. It counts as progress, and
        // the recovery handler reads it to know where to resume from.
        reliability().checkpoint("Banked");
        Bank.closeBank();
        state = State.RETURNING;
    }

    private void walkBack() {
        if (workSpot == null || Player.isInArea(workSpot, WORK_RADIUS)) {
            state = State.WORKING;
            return;
        }
        status = "Walking back";

        // walkTo blocks until it arrives or gives up, handling doors, stairs and transports.
        ActionResult walked = reliability().step("Walk back to the work spot")
                .action(() -> Walker.walkTo(workSpot, 3))
                .run();
        if (walked.isSuccess()) {
            reliability().checkpointAtPlayer("Back at the work spot");
            state = State.WORKING;
        }
    }

    /** The "Keep items" setting as a list, without the blanks a stray comma leaves. */
    private String[] keepItems() {
        return Arrays.stream(config.keepItems().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void shutdown() {
        // Script.shutdown() cancels the loop, resets the reliability service and clears the
        // shared helpers. Call it first, then clear your own state, so a restart is clean.
        super.shutdown();
        state = State.PREPARING;
        status = "Stopped";
        workSpot = null;
    }

    // Read by the overlay.

    public State getState() {
        return state;
    }

    public String getStatus() {
        return status;
    }

    /** When this run started, in epoch millis. The stats panel resets its numbers when this changes. */
    public long getStartedAt() {
        return startedAt;
    }

    public int getTrips() {
        return trips;
    }
}
