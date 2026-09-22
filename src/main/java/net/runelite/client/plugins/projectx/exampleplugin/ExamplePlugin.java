package net.runelite.client.plugins.projectx.exampleplugin;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

/**
 * The plugin: lifecycle and wiring. The actual work lives in {@link ExampleScript}.
 *
 * Every field in the descriptor below matters:
 *   name             what the user sees in the plugin list
 *   version          bump it on every change; the store and the hub key off it
 *   minClientVersion the oldest client this script is known to work on
 *   isExternal       true for anything not built into the client
 *
 * The class name is the script's identity: the jar is named after it, and a
 * marketplace listing joins to it by internalName. Renaming the class is a
 * breaking change.
 */
@PluginDescriptor(
        name = ExamplePlugin.PREFIX + "Example",
        description = "Template script: idles, logs, and shows an overlay",
        tags = {"example", "template"},
        version = ExamplePlugin.version,
        minClientVersion = "2.6.22",
        enabledByDefault = false,
        isExternal = true
)
@Slf4j
public class ExamplePlugin extends Plugin {
    /** Your mark in the plugin list. Pick your own colour and initials. */
    static final String PREFIX = "<html>[<font color=#e8c97e>PX</font>] ";

    public static final String version = "1.0.0";

    @Inject
    private ExampleScript exampleScript;
    @Inject
    private ExampleConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ExampleOverlay overlay;

    @Provides
    ExampleConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ExampleConfig.class);
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
