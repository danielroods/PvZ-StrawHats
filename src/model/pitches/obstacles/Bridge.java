package model.pitches.obstacles;

/**
 * A wooden plank laid across a water tile (Pirate Seas chapter).
 * Wherever a Bridge sits on a {@link model.pitches.TileType#Water} cell, that
 * cell behaves like normal ground: plants can be placed there and ground-bound
 * zombies can walk across it instead of being stopped by the water.
 */
public class Bridge implements Obstacle {
    /** The only art asset currently available for this chapter's bridges. */
    public static final String PLANK_TEXTURE =
            "assets/images/chapters/pirate/gameplay/plank_02.png";

    private final String texturePath;

    public Bridge() {
        this(PLANK_TEXTURE);
    }

    public Bridge(String texturePath) {
        this.texturePath = texturePath == null ? PLANK_TEXTURE : texturePath;
    }

    public String getTexturePath() { return texturePath; }

    @Override
    public boolean blocksPlanting() { return false; }

    @Override
    public String getName() { return "Bridge"; }
}