package net.runelite.client.plugins.projectx.templateplugin;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

/**
 * The plugin: lifecycle and wiring. The actual work lives in {@link TemplateScript}.
 *
 * Every field in the descriptor below matters:
 *   name             what the user sees in the plugin list
 *   version          bump it on every change; the store and the hub key off it
 *   minClientVersion the oldest plugin API your script needs, not a client release
 *   isExternal       true for anything not built into the client
 *
 * The class name is the script's identity: the jar is named after it, and a
 * marketplace listing joins to it by internalName. Renaming the class is a
 * breaking change.
 */
@PluginDescriptor(
        name = "Template Script",
        description = "Template script: idles, logs, and shows an overlay",
        tags = {"example", "template"},
        version = TemplatePlugin.version,
        // The plugin API level you need, which is not the client's release
        // number. The client publishes both: it is on 1.x, and provides plugin
        // API 2.6.22. Leave this alone unless you are using something added to
        // the API after that; setting it to a client release like "1.0.5" says
        // nothing useful, and setting it above the API level the client offers
        // stops your script loading at all.
        minClientVersion = "2.6.22",
        enabledByDefault = false,
        isExternal = true
)
@Slf4j
public class TemplatePlugin extends Plugin {
    /** Your mark in the plugin list. Pick your own colour and initials. */

    public static final String version = "1.0.0";

    @Inject
    private TemplateScript exampleScript;
    @Inject
    private TemplateConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private TemplateOverlay overlay;

    @Provides
    TemplateConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TemplateConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        // Start the loop here, never do the work itself: startUp runs on the
        // client thread and must return immediately.
        exampleScript.run(config);
    }

    @Override
    protected void shutDown() {
        exampleScript.shutdown();
        overlayManager.remove(overlay);
    }
}
