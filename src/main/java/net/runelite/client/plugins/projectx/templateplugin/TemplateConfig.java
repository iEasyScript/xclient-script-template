package net.runelite.client.plugins.projectx.templateplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;

/**
 * Everything the player can change, shown in the Project X config panel.
 *
 * The script reads these through the proxy every loop, so a change takes effect on the next
 * iteration without a restart. Keep reads cheap and never cache the values in fields.
 */
@ConfigGroup(TemplateConfig.GROUP)
@ConfigInformation("Stand next to the thing you want to work on (trees, rocks, a fishing spot's bank side) and press start.<br><br>"
        + "When the inventory is full the script banks at the nearest bank, then walks back to where you started.")
public interface TemplateConfig extends Config {
    /**
     * The key every setting is saved under. It must be unique across every script a player
     * might install, so name it after your script, and never change it after publishing:
     * players would lose their saved settings.
     */
    String GROUP = "templatescript";

    @ConfigSection(
            name = "Activity",
            description = "What to work on",
            position = 0
    )
    String activitySection = "activity";

    @ConfigItem(
            keyName = "objectName",
            name = "Object",
            description = "The name of the object to work on, exactly as the game shows it, e.g. Tree, Oak tree, Iron rocks",
            position = 0,
            section = activitySection
    )
    default String objectName() {
        return "Tree";
    }

    @ConfigItem(
            keyName = "action",
            name = "Action",
            description = "The menu option to use on it, e.g. Chop down, Mine",
            position = 1,
            section = activitySection
    )
    default String action() {
        return "Chop down";
    }

    @ConfigSection(
            name = "Banking",
            description = "What to keep when banking",
            position = 1
    )
    String bankingSection = "banking";

    /**
     * A config item of type InventorySetup is shown as a picker of the player's saved
     * inventory setups. It returns null when nothing is picked, so always handle that.
     */
    @ConfigItem(
            keyName = "inventorySetup",
            name = "Inventory setup",
            description = "Optional. Loaded from the bank before starting, and everything in it is kept when banking",
            position = 0,
            section = bankingSection
    )
    default InventorySetup inventorySetup() {
        return null;
    }

    @ConfigItem(
            keyName = "keepItems",
            name = "Keep items",
            description = "Without an inventory setup: items never to bank, comma separated. Matches part of the name, so 'axe' keeps any axe",
            position = 1,
            section = bankingSection
    )
    default String keepItems() {
        return "axe, pickaxe";
    }
}
