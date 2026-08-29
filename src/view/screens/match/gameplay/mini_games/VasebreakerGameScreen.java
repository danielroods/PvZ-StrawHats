package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import controller.ScreenManager;
import controller.match.mini_games.MiniGameEndMenu;
import controller.match.mini_games.VasebreakerController;
import model.App;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.match.mini_games.vasebreaker.Vasebreaker;
import model.match.mini_games.vasebreaker.vase.Vase;
import service.GameClock;
import service.card_factory.SeedPacketCard;
import service.card_factory.SeedPacketCardFactory;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;
import view.screens.generals.Toast;

import java.util.Map;


public class VasebreakerGameScreen extends GameScreen {

    {
        seasonFolder = "vasebreaker";
    }

    private Table seedPanel;
    private String selectedSeedName;
    private final SeedPacketCardFactory cardFactory = new SeedPacketCardFactory();

    @Override
    protected boolean areLawnMowersVisible() {
        return false;
    }

    @Override
    protected void tickSession() {
        if (App.currentMenu instanceof VasebreakerController controller) {
            controller.tick(GameClock.SECONDS_PER_TICK);
        }
    }

    @Override
    protected void checkMatchEnd() {
        if (matchFinished || !isMatchEndSequenceIdle()) return;
        if (App.currentMenu instanceof VasebreakerController) return;

        if (App.currentMenu instanceof MiniGameEndMenu end) {
            if (seedPanel != null) seedPanel.setVisible(false);
            startMatchEndSequence(end.isWon());
            return;
        }
        matchFinished = true;
        ScreenManager.syncWithCurrentMenu();
    }

    @Override
    public void onMatchEndSequenceFinished(boolean won) {
    }

    @Override
    protected String getSeasonGameplayFolder() {
        return ASSET_ROOT + "/mini_games/" + seasonFolder + "/gameplay/";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        selectedSeedName = null;
        if (hud != null) {
            hud.setLoadoutBankVisible(false);
            hud.setFoodVisible(false);
            hud.setShovelVisible(false);
            hud.setStartButtonAvailable(false);
        }
        buildSeedPanel();
    }

    private Vasebreaker currentGame() {
        return App.currentMenu instanceof VasebreakerController controller ? controller.getGame() : null;
    }

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

    // Dropped-packet preview is the same seed packet card used in the loadout (same
    // plant icon + packet skin, same proportions), just shown smaller on the board.
    private static final float DROPPED_PACKET_SCALE = 0.42f;

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        Vasebreaker game = currentGame();
        if (game == null) return;

        for (Vase vase : game.getVases()) {
            if (vase.isBroken()) continue;
            float x = getCellX((int) vase.getPosition().x());
            float y = getCellY((int) vase.getPosition().y());
            drawEntity(vaseRegion(vase), x + getBoardTileWidth() * 0.22f, y + getBoardTileHeight() * 0.12f,
                    getBoardTileWidth() * 0.56f, getBoardTileHeight() * 0.7f, vaseColor(vase), "");
        }

        for (Vasebreaker.DroppedSeedPacket packet : game.getDroppedPackets()) {
            if (packet.collected) continue;
            float x = getCellX((int) packet.position.x());
            float y = getCellY((int) packet.position.y());
            drawDroppedSeedPacket(packet, x, y);
        }
    }

    private void drawDroppedSeedPacket(Vasebreaker.DroppedSeedPacket packet, float cellX, float cellY) {
        String name = plantName(packet.plantId);

        SeedPacketCard card = null;
        try {
            card = cardFactory.buildCardByPlantName(name);
        } catch (Throwable ignored) {
            // Falls through to the plain-square fallback below.
        }

        if (card == null) {
            drawFallback(cellX + getBoardTileWidth() * 0.3f, cellY + getBoardTileHeight() * 0.3f,
                    getBoardTileWidth() * 0.4f, getBoardTileHeight() * 0.4f, new Color(0.35f, 0.85f, 0.35f, 0.95f));
            return;
        }

        float w = card.getWidth() * DROPPED_PACKET_SCALE;
        float h = card.getHeight() * DROPPED_PACKET_SCALE;
        float x = cellX + (getBoardTileWidth() - w) * 0.5f;
        float y = cellY + (getBoardTileHeight() - h) * 0.5f;

        card.setPosition(x, y);
        card.setSize(w, h);
        card.draw(batch, 1f);
    }

    private Color vaseColor(Vase vase) {
        return switch (vase.getVaseType()) {
            case GARGANTUAR -> new Color(0.4f, 0.32f, 0.6f, 1f);
            case PLANT_SEED -> new Color(0.75f, 0.55f, 0.2f, 1f);
            case NORMAL -> new Color(0.72f, 0.5f, 0.32f, 1f);
        };
    }

    private TextureRegion vaseRegion(Vase vase) {
        return switch (vase.getVaseType()) {
            case NORMAL -> new TextureRegion(new Texture(getSeasonGameplayFolder()+"Vase_brown_115x150.png"));
            case GARGANTUAR -> new TextureRegion(new Texture(getSeasonGameplayFolder()+"Vase_gargantuar_115x150.png"));
            case PLANT_SEED -> new TextureRegion(new Texture(getSeasonGameplayFolder()+"vasebreaker_endless_node_115x150_2.png"));
        };
    }

    private void buildSeedPanel() {
        Vasebreaker game = currentGame();
        if (seedPanel != null) {
            seedPanel.remove();
            seedPanel = null;
        }
        if (game == null) return;

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("card-background"));
        panel.pad(6f);

        Label title = new Label("SEEDS", skin, "main");
        title.setAlignment(com.badlogic.gdx.utils.Align.center);
        panel.add(title).growX().padBottom(4f).row();

        Table row = new Table();
        row.top().center();
        Map<Integer, Integer> inventory = game.getSeedInventory();
        if (inventory.isEmpty()) {
            row.add(new Label("Break plant vases to find seeds.", skin, "main"))
                    .width(94f).center();
        } else {
            if (selectedSeedName != null && inventory.keySet().stream()
                    .noneMatch(id -> plantName(id).equalsIgnoreCase(selectedSeedName))) {
                selectedSeedName = null;
            }
            for (Map.Entry<Integer, Integer> entry : inventory.entrySet()) {
                String name = plantName(entry.getKey());
                Stack seedCard = buildSeedCard(name, entry.getValue());
                row.add(seedCard).size(seedCard.getWidth(), seedCard.getHeight())
                        .padBottom(6f).row();
            }
        }

        ScrollPane scroll = new ScrollPane(row);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        float panelWidth = 150f + 12f;
        panel.add(scroll).width(panelWidth).height(Math.min(520f, Math.max(180f, stage.getViewport().getWorldHeight() - 90f)));
        panel.pack();
        panel.setPosition(18f, (stage.getViewport().getWorldHeight() - panel.getHeight()) * 0.5f);

        seedPanel = panel;
        stage.addActor(seedPanel);
    }

    private Stack buildSeedCard(String name, int count) {
        Stack stack = new Stack();
        boolean selected = name.equalsIgnoreCase(selectedSeedName);

        // Same card, same intrinsic size/scale as the plant-picker cards on the
        // before-match loadout screen (BeforeMatchScreen#buildCard) - no forced
        // .size() override here, just take the card's own native dimensions.
        float cardW = 104f;
        float cardH = cardW * 1.38f;

        SeedPacketCard card = null;
        try {
            card = cardFactory.buildCardByPlantName(name);
        } catch (Throwable ignored) {
            // Falls through to the text-button fallback below.
        }

        if (card != null) {
            cardW = card.getWidth();
            cardH = card.getHeight();
            stack.add(card);
            if (selected) {
                Table ring = new Table();
                ring.setBackground(skin.getDrawable("card-background"));
                ring.getColor().set(1f, 0.9f, 0.2f, 0.35f);
                stack.add(ring);
            }
        } else {
            Table fallback = new Table();
            fallback.setBackground(skin.getDrawable("card-background"));
            fallback.add(new Label(name, skin, "main")).expand().center();
            stack.add(fallback);
        }

        stack.setSize(cardW, cardH);

        Table badge = new Table();
        badge.bottom().right();
        Label countLabel = new Label("x" + count, skin, "main");
        badge.add(countLabel).padBottom(2f).padRight(2f);
        stack.add(badge);

        stack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                selectedSeedName = name;
                selectPlant(name);
                buildSeedPanel();
            }
        });
        return stack;
    }


    private String plantName(int plantId) {
        PlantJsonParser.PlantConfig config = PlantFactory.getBlueprints().get(plantId);
        return config == null ? "Plant#" + plantId : config.name;
    }

    @Override
    public void dispose() {
        cardFactory.dispose();
        super.dispose();
    }
}