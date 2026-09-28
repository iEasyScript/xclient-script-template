package net.runelite.client.plugins.projectx.templateplugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.util.concurrent.TimeUnit;

/**
 * The loop. Runs on a background executor, not the client thread.
 *
 * Three rules keep scripts out of trouble:
 *   1. Never block the client thread. Game state reads that need it go through
 *      ProjectX.getClientThread().runOnClientThreadOptional(...).
 *   2. Wait on conditions with sleepUntil(...), not fixed sleeps.
 *   3. Leave no trace in shutdown(): cancel timers, clear state, unregister.
 */
@Slf4j
public class TemplateScript extends Script {
    public boolean run(TemplateConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // super.run() honours pause/stop and the blocking-event system
                // (login, level-up, random events), so always check it first.
                if (!super.run()) return;
                if (!ProjectX.isLoggedIn()) return;

                ProjectX.status = "Idling as " + Rs2Player.getLocalPlayer().getName();

                // ... your logic here.

            } catch (Exception e) {
                // Never let an exception escape into the executor: it kills the
                // loop silently.
                log.error("Template script failed", e);
            }
        }, 0, 600, TimeUnit.MILLISECONDS); // 600ms is one game tick
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        ProjectX.status = "";
    }
}
