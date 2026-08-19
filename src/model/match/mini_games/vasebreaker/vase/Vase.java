package model.match.mini_games.vasebreaker.vase;

import model.match.mini_games.vasebreaker.Vasebreaker;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public abstract class Vase {
    public enum VaseType {
        NORMAL,
        PLANT_SEED,
        GARGANTUAR
    }

    protected final Position position;
    private boolean broken = false;

    protected Vase(Position position) {
        this.position = position;
    }

    public Position getPosition() { return position; }
    public boolean isBroken() { return broken; }

    public VaseType getVaseType() { return VaseType.NORMAL; }

    public String getDisplayName() {
        return switch (getVaseType()) {
            case PLANT_SEED -> "Plant seed vase";
            case GARGANTUAR -> "Gargantuar vase";
            case NORMAL -> "Normal vase";
        };
    }

    public char getMapSymbol() {
        return switch (getVaseType()) {
            case PLANT_SEED -> 'P';
            case GARGANTUAR -> 'G';
            case NORMAL -> '?';
        };
    }

    public String getRevealedContents() {
        return getDisplayName();
    }

    public final void breakVase(GameSession session, Vasebreaker minigame) {
        if (broken) return;
        broken = true;
        onBreak(session, minigame);
    }

    protected abstract void onBreak(GameSession session, Vasebreaker minigame);
}
