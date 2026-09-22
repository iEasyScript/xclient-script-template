package net.runelite.client.plugins.projectx.exampleplugin;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

/** On-screen status. Keep render() cheap: it runs every frame. */
public class ExampleOverlay extends OverlayPanel {
    @Inject
    ExampleOverlay(ExamplePlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredSize(new Dimension(200, 60));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Example v" + ExamplePlugin.version)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Status:")
                .right(ProjectX.status)
                .build());
        return super.render(graphics);
    }
}
