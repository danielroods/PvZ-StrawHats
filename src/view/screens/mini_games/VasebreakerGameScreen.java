package view.screens.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import controller.mini_games.VasebreakerController;
import model.App;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.match.mini_games.vasebreaker.Vasebreaker;
import model.match.mini_games.vasebreaker.vase.Vase;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.GameScreen;
import view.general_screens.Toast;

import java.util.Map;

/**
 * VasebreakerGameScreen mini-game gameplay entry point. The actual gameplay machinery
 * lives in GameScreen, exactly like the regular chapter stages; this class only
 * points it at the mini-game's own art folder (background, left/right border
 * textures, and grave icon all come from the same folder - see
 * GameScreen.getSeasonGameplayFolder()) and wires up Vasebreaker-specific interaction:
 * clicking an unbroken vase breaks it, clicking a dropped seed packet collects it,
 * and otherwise the currently selected inventory seed is planted (default GameScreen
 * behaviour). Vases, dropped packets, and the seed inventory picker are not part of
 * GameScreen's normal rendering, so this screen draws its own on top.
 */
public class VasebreakerGameScreen extends GameScreen {

    {
       seasonFolder = "vasebreaker";
    }

    private Table seedPanel;
    private String selectedSeedName;

    @Override
    protected String getSeasonGameplayFolder() {
        return "assets/images/backg/mini_games/vasebrekaer/";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        selectedSeedName = null;
        buildSeedPanel();
    }

    private Vasebreaker currentGame() {
        return App.currentMenu instanceof VasebreakerController controller ? controller.getGame() : null;
    }

    /**
     * Click priority: break an unbroken vase first, then collect a dropped seed
     * packet, then fall back to planting the selected inventory seed (handled by
     * GameScreen's default plantAtCell, same "plant plant -t X -l (x,y)" command
     * the terminal engine already understands).
     */
    @Override
    protected void onCellClicked(int row, int col) {
        Vasebreaker game = currentGame();
        if (game == null) {
            super.onCellClicked(row, col);
            return;
        }

        Vase vase = game.getVaseAt(row, col);
        if (vase != null && !vase.isBroken()) {
            runCommand("break vase -l (" + (col + 1) + ", " + (row + 1) + ")");
            return;
        }

        boolean hasPacketHere = game.getDroppedPackets().stream()
                .anyMatch(packet -> !packet.collected
                        && (int) packet.position.y() == row && (int) packet.position.x() == col);
        if (hasPacketHere) {
            runCommand("collect seed -l (" + (col + 1) + ", " + (row + 1) + ")");
            buildSeedPanel();
            return;
        }

        if (selectedSeedName == null) {
            Toast.show(stage, "Pick a seed from your inventory first, or break a vase to find one.");
            return;
        }
        selectPlant(selectedSeedName);
        super.onCellClicked(row, col);
        buildSeedPanel();
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        Vasebreaker game = currentGame();
        if (game == null) return;

        for (Vase vase : game.getVases()) {
            if (vase.isBroken()) continue;
            float x = getCellX((int) vase.getPosition().x());
            float y = getCellY((int) vase.getPosition().y());
            drawFallback(x + getBoardTileWidth() * 0.22f, y + getBoardTileHeight() * 0.12f,
                    getBoardTileWidth() * 0.56f, getBoardTileHeight() * 0.7f, vaseColor(vase));
        }

        for (Vasebreaker.DroppedSeedPacket packet : game.getDroppedPackets()) {
            if (packet.collected) continue;
            float x = getCellX((int) packet.position.x());
            float y = getCellY((int) packet.position.y());
            drawFallback(x + getBoardTileWidth() * 0.3f, y + getBoardTileHeight() * 0.3f,
                    getBoardTileWidth() * 0.4f, getBoardTileHeight() * 0.4f, new Color(0.35f, 0.85f, 0.35f, 0.95f));
        }
    }

    private Color vaseColor(Vase vase) {
        return switch (vase.getVaseType()) {
            case ZOMBIE -> new Color(0.55f, 0.68f, 0.32f, 1f);
            case GARGANTUAR -> new Color(0.4f, 0.32f, 0.6f, 1f);
            case PLANT_SEED -> new Color(0.75f, 0.55f, 0.2f, 1f);
            case NORMAL -> new Color(0.72f, 0.5f, 0.32f, 1f);
        };
    }

    /**
     * Small seed-inventory picker pinned to the bottom-left of the stage, since
     * MatchHud's plant tray is built around the pre-match loadout (BeforeMenu) and
     * Vasebreaker's seeds are earned mid-match by breaking plant vases instead.
     * Added directly to the stage (like Toast) rather than into rootStack, which
     * force-stretches any actor added to it - see GameScreen board-layout notes.
     */
    private void buildSeedPanel() {
        Vasebreaker game = currentGame();
        if (seedPanel != null) {
            seedPanel.remove();
            seedPanel = null;
        }
        if (game == null) return;

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("card-background"));
        panel.pad(8f);

        Label title = new Label("SEEDS", skin, "main");
        panel.add(title).left().padBottom(4f).row();

        Table row = new Table();
        row.left();
        Map<Integer, Integer> inventory = game.getSeedInventory();
        if (inventory.isEmpty()) {
            row.add(new Label("Break plant vases to find seeds.", skin, "main")).left();
        } else {
            if (selectedSeedName != null && !inventory.keySet().stream()
                    .anyMatch(id -> plantName(id).equalsIgnoreCase(selectedSeedName))) {
                selectedSeedName = null;
            }
            for (Map.Entry<Integer, Integer> entry : inventory.entrySet()) {
                String name = plantName(entry.getKey());
                TextButton button = new TextButton(name + " x" + entry.getValue(), skin,
                        name.equalsIgnoreCase(selectedSeedName) ? "default" : "secondary");
                button.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        selectedSeedName = name;
                        selectPlant(name);
                        buildSeedPanel();
                    }
                });
                row.add(button).padRight(6f);
            }
        }

        ScrollPane scroll = new ScrollPane(row);
        scroll.setScrollingDisabled(false, true);
        panel.add(scroll).width(360f).height(48f);
        panel.pack();
        panel.setPosition(24f, 24f);

        seedPanel = panel;
        stage.addActor(seedPanel);
    }

    private String plantName(int plantId) {
        PlantJsonParser.PlantConfig config = PlantFactory.getBlueprints().get(plantId);
        return config == null ? "Plant#" + plantId : config.name;
    }
}
