package net.runelite.client.plugins.projectx.exampleplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/** Anything the user can change. Shown in the Project X config panel. */
@ConfigGroup(ExampleConfig.GROUP)
public interface ExampleConfig extends Config {
    String GROUP = "example";

    @ConfigItem(
            keyName = "greeting",
            name = "Greeting",
            description = "Written to the log when the script starts",
            position = 0
    )
    default String greeting() {
        return "Hello from Project X";
    }
}
