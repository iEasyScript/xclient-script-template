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
 * <p>The class name is the script's identity. The jar is named after it, the store listing
 * joins to it by "internal name", and the client finds a downloaded jar by it. Rename it to
 * your script's name before you publish, and never rename it afterwards.
 */
@PluginDescriptor(
        // What the player sees in the plugin list.
        name = "Template Script",
        description = "Works on an object until the inventory is full, banks, and walks back",
        tags = {"template", "skilling", "example"},
        // Your script's version. Bump it on every build you upload: the store and the client
        // key off it, and each version can only be uploaded once.
        version = TemplatePlugin.version,
        // The oldest *plugin API* this script needs. This is not the client's release number
        // (1.0.x); it is the API level the client provides (2.x.y). A client older than this
        // refuses to load the script, and the store refuses to install it there.
        // 2.7.0 is the first API with the simple bot package (Inventory, Objects.query(), Loadout...).
        minClientVersion = "2.7.0",
        // Scripts start switched off; the player turns them on.
        enabledByDefault = false,
        // Must be true for anything not built into the client. It is what applies the
        // minClientVersion check above.
        isExternal = true
)
@Slf4j
public class TemplatePlugin extends Plugin {

    /** Shown in the stats panel title. Keep it in step with the descriptor above. */
    public static final String version = "1.0.0";

    @Inject
    private TemplateScript script;
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
        // Start the loop and return. startUp runs on the Swing UI thread: doing any work here,
        // or waiting for anything, freezes the client window.
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }

    /** The running script, for the overlay. */
    TemplateScript getScript() {
        return script;
    }
}
