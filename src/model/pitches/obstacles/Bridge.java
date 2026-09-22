package model.pitches.obstacles;


public class Bridge implements Obstacle {
    
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