package model.pitches.obstacles;

/// Mold growing on a tile - permanently blocks planting on that tile.
/// Used by the "Not Every Where You Can Plant!" special level, where an
/// entire column of the lawn is covered in mold and cannot be planted on.
public class MoldBlock implements Obstacle {

    /// PAM animation asset used to render the mold on a blocked tile.
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