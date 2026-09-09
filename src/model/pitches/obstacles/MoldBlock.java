package model.pitches.obstacles;




public class MoldBlock implements Obstacle {

    
    public static final String PAM_PATH = "768/INITIAL/EFFECTS/STAR_OBJECTIVE_MOLD/STAR_OBJECTIVE_MOLD.PAM";
    public static final String PAM_CLIP = "idle";

    @Override
    public boolean blocksPlanting() {
        return true;
    }

    @Override
    public String getName() {
        return "Mold";
    }
}