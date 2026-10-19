package net.runelite.client.plugins.projectx.templateplugin;

import net.runelite.api.Client;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.overlay.ScriptPanel;
import net.runelite.client.plugins.projectx.util.overlay.ScriptPanelOverlay;
import net.runelite.client.plugins.projectx.util.overlay.SkillTracker;

import javax.inject.Inject;
import java.util.Locale;

/**
 * The on-screen stats panel, in the same style as every other Project X script.
 *
 * <p>{@link ScriptPanelOverlay} owns the overlay boilerplate: position, layering, and catching a
 * mistake in {@link #build()} so it shows as an error line instead of taking the overlay down.
 * All this class does is describe what to show.
 *
 * <p>{@code build()} runs on the client thread every frame. Read state and return: never wait,
 * never click, never walk, and never make a network call from here.
 */
public class TemplateOverlay extends ScriptPanelOverlay {

    private final TemplatePlugin plugin;
    private final Client client;

    /** One per overlay. Fed every frame; works out XP gained, XP an hour and time to level. */
    private final SkillTracker skills = new SkillTracker();

    @Inject
    TemplateOverlay(TemplatePlugin plugin, Client client) {
        super(plugin);
        this.plugin = plugin;
        this.client = client;
    }

    @Override
    protected ScriptPanel build() {
        TemplateScript script = plugin.getScript();

        // Pass the stored start time, never "now - runtime": the tracker starts again from zero
        // whenever this number changes, which is how a restarted script gets fresh numbers.
        long startedAt = script.getStartedAt();
        skills.observe(client, startedAt);

        boolean running = script.isRunning();
        boolean paused = ProjectX.pauseAllScripts.get();
        String status = !running ? "Stopped" : paused ? "Paused" : script.getStatus();

        return ScriptPanel.create("Template Script", TemplatePlugin.version)
                .theme(ScriptPanel.Theme.BLUE)
                .startedAt(running ? startedAt : 0)
                .status(status)
                .section("EXPERIENCE")
                // Every skill that has gained XP this run, most first, up to three.
                .trainedSkills(skills, 3)
                .section("SESSION")
                .row("State", pretty(script.getState()), !running || paused ? ScriptPanel.WARN : null)
                .row("Trips", ScriptPanel.withRate(script.getTrips(), skills.elapsedMs()));
    }

    private static String pretty(TemplateScript.State state) {
        if (state == null) return null;
        String raw = state.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
