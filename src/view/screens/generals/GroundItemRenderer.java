package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import controller.assets.GameAssetManager;
import model.collections.item.GroundItem;
import model.collections.item.GroundSun;
import model.match_mechanisms.vector.Position;

import java.util.IdentityHashMap;
import java.util.Map;

class GroundItemRenderer {

    private static final float SUN_PAM_SCALE_MULTIPLIER = 1.35f;
    private static final float SUN_PAM_LOOP_SECONDS = 1.0f;
    private static final float SUN_RED_TRANSITION_SECONDS = 0.24f;

    private static final String SEED_PACK_PNG_PATH = "images/ui/seedpacket.png";

    private final GameScreen screen;
    private Texture seedPackTexture;

    private final Map<GroundItem, Float> itemAnimTimes = new IdentityHashMap<>();

    GroundItemRenderer(GameScreen screen) {
        this.screen = screen;
    }

    float animTimeFor(GroundItem item) {
        return itemAnimTimes.getOrDefault(item, 0f);
    }

    void drawGroundItems(float delta, float bw, float bh) {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        for (model.collections.Item raw : screen.session.getItems()) {
            if (!(raw instanceof GroundItem item)) continue;
            if (item == null || !item.isAlive() || item.isCollected() || item.getPosition() == null) continue;

            float age = itemAnimTimes.getOrDefault(item, 0f) + delta;
            itemAnimTimes.put(item, age);

            Position p = item.getPosition();
            
            
            
            float x = GameScreen.BOARD_X + (float) p.x() * boardTileWidth + boardTileWidth * 0.28f;
            float y = screen.cellY(Math.round(p.y())) + boardTileHeight * 0.25f;

            if (item instanceof GroundSun sun) {
                float progress = sun.getFallProgress();
                y = GameScreen.BOARD_Y + bh + 35f + (y - (GameScreen.BOARD_Y + bh + 35f)) * progress;
            }

            float pulse = 1f;
            if (item instanceof GroundSun) {
                pulse = 0.92f + 0.08f * (float) Math.sin(age * 5.5f);
                y += (float) Math.sin(age * 3.0f) * 3f;
            }

            float size = boardTileWidth * 0.45f * pulse;
            float drawX = x + (boardTileWidth * 0.45f - size) * 0.5f;
            float drawY = y + (boardTileHeight * 0.45f - size) * 0.5f;

            String typeName = item.getItemType() != null ? item.getItemType().name().toUpperCase() : "";
            int row = (int) Math.round(p.y());
            float finalY = y;
            screen.queueRowDraw(row, () -> drawGroundItemVisual(item, typeName, x, finalY, drawX, drawY, size,
                    age, boardTileWidth, boardTileHeight));
        }
        itemAnimTimes.keySet().removeIf(i -> !screen.session.getItems().contains(i));
    }

    private void drawGroundItemVisual(GroundItem item, String typeName, float x, float y,
                                      float drawX, float drawY, float size, float age,
                                      float boardTileWidth, float boardTileHeight) {
        if (item instanceof GroundSun sun) {
            String pamPath = GroundSun.getPamAnimationPath(sun.getDropType());
            String clipName = GroundSun.getPamAnimationClip(sun.getDropType());
            if (sun.isRedStealAnimation()) {
                float redTime = (float) sun.getRedStealAnimationTime();
                if (redTime < SUN_RED_TRANSITION_SECONDS) {
                    clipName = "transition_red";
                    age = redTime;
                } else {
                    clipName = "red";
                    age = (redTime - SUN_RED_TRANSITION_SECONDS) % SUN_PAM_LOOP_SECONDS;
                }
                pamPath = GroundSun.NORMAL_SUN_PAM_PATH;
            }
            float loopingPamTime = age % SUN_PAM_LOOP_SECONDS;
            float pamScale = (size / 100f) * SUN_PAM_SCALE_MULTIPLIER;

            if (!screen.drawPam(pamPath, clipName, loopingPamTime,
                    drawX + size * 0.5f, drawY + size * 0.5f, pamScale, false)) {
                screen.drawFallback(drawX, drawY, size, size, GameScreenGraphics.itemColor("SUN"));
            }
        } else if (typeName.contains("PLANT_FOOD") || typeName.contains("PLANTFOOD")) {
            String pamPath = "768/INITIAL/EFFECTS/PLANTFOOD_PICKUP/PLANTFOOD_PICKUP.PAM";
            float pamScale = (size / 100f) * 1.2f;
            if (!screen.drawPam(pamPath, "idle", age, drawX + size * 0.5f, drawY + size * 0.5f, pamScale, false)) {
                screen.drawFallback(drawX, drawY, size, size, GameScreenGraphics.itemColor("PLANT_FOOD"));
            }
        } else if (typeName.contains("DIAMOND")) {
            String pamPath = "768/INITIAL/EFFECTS/COIN_DIAMOND/COIN_DIAMOND.PAM";
            float pamScale = (size / 100f) * 0.6f;
            if (!screen.drawPam(pamPath, "idle", age, drawX + size * 0.5f, drawY + size * 0.5f, pamScale, false)) {
                screen.drawFallback(drawX, drawY, size, size, GameScreenGraphics.itemColor("DIAMOND"));
            }
        } else if (typeName.contains("SILVER")) {
            String pamPath = "768/INITIAL/EFFECTS/COIN_SILVER/COIN_SILVER.PAM";
            float pamScale = (size / 100f) * 1.2f;
            if (!screen.drawPam(pamPath, "animation", age, drawX + size * 0.5f, drawY + size * 0.5f, pamScale, false)) {
                screen.drawFallback(drawX, drawY, size, size, GameScreenGraphics.itemColor("COIN"));
            }
        } else if (typeName.contains("COIN") || typeName.contains("GOLD")) {
            String pamPath = "768/INITIAL/EFFECTS/COIN_GOLD/COIN_GOLD.PAM";
            float pamScale = (size / 100f) * 1.2f;
            if (!screen.drawPam(pamPath, "animation", age, drawX + size * 0.5f, drawY + size * 0.5f, pamScale, false)) {
                screen.drawFallback(drawX, drawY, size, size, GameScreenGraphics.itemColor("COIN"));
            }
        } else if (typeName.contains("SEED_PACK") || typeName.contains("SEEDPACK")) {
            if (seedPackTexture == null && Gdx.files.internal(SEED_PACK_PNG_PATH).exists()) {
                seedPackTexture = new Texture(Gdx.files.internal(SEED_PACK_PNG_PATH));
            }

            if (seedPackTexture != null) {
                float packPulse = 1f + 0.04f * (float) Math.sin(age * 3.5f);
                float pSize = size * packPulse;
                float pDrawX = x + (boardTileWidth * 0.45f - pSize) * 0.5f;
                float pDrawY = y + (boardTileHeight * 0.45f - pSize) * 0.5f;
                screen.batch.draw(seedPackTexture, pDrawX, pDrawY, pSize, pSize);
            } else {
                screen.drawFallback(drawX, drawY, size, size,
                        GameScreenGraphics.itemColor("SEED_PACK"));
            }
        } else if (typeName.contains("POT") || typeName.contains("STACK")) {
            Texture pTex = screen.assets().getPotTexture();
            if (pTex != null) {
                float potPulse = 1f + 0.07f * (float) Math.sin(age * 3.5f);
                float pSize = size * potPulse;
                float pDrawX = x + (boardTileWidth * 0.45f - pSize) * 0.5f;
                float pDrawY = y + (boardTileHeight * 0.45f - pSize) * 0.5f;
                screen.batch.draw(pTex, pDrawX, pDrawY, pSize, pSize);
            } else {
                screen.drawFallback(drawX, drawY, size, size, Color.BROWN);
            }
        } else {
            TextureRegion region = GameAssetManager.get().getItemRegion(item.getItemType().name());
            if (region != null) {
                screen.batch.draw(region, drawX, drawY, size, size);
            } else {
                screen.drawFallback(drawX, drawY, size, size, GameScreenGraphics.itemColor(item.getItemType().name()));
            }
        }
    }

    void dispose() {
        if (seedPackTexture != null) {
            seedPackTexture.dispose();
            seedPackTexture = null;
        }
    }
}