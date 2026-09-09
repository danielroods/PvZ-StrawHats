package controller.cheat;

import model.collections.zombie.Zombie;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the "release the nuke" cheat: kills every zombie currently on the
 * lawn except the Zomboss, which is immune. Kept separate from the view
 * (mirrors {@link CurrencyCheatController}) so the HUD button just triggers
 * this and the visual sequence, without owning any game-state logic itself.
 *
 * Killing is done through {@link Zombie#takeDamage(int, Object)} rather than
 * removing zombies directly from the session, so death handling runs exactly
 * as it would for any other kill (drops, wave-progress bookkeeping, mower
 * cleanup, etc. all fire normally and the wave/level continues as usual).
 */
public class NukeCheatController {

    /**
     * @return the zombies that were killed, so the caller (e.g. an on-screen
     * effect) can react to them if it wants to.
     */
    public List<Zombie> detonate() {
        List<Zombie> killed = new ArrayList<>();
        if (!CheatAccess.allow()) return killed;

        GameSession session = GameSession.peekInstance();
        if (session == null) return killed;

        
        
        List<Zombie> targets = new ArrayList<>(session.getZombies());
        for (Zombie zombie : targets) {
            if (zombie == null || !zombie.isAlive() || zombie.isBoss()) continue;
            zombie.takeDamage(Integer.MAX_VALUE / 2, this);
            killed.add(zombie);
        }

        GeneralPrinter.print("[Cheat] Released the nuke - " + killed.size() + " zombie(s) wiped out.");
        return killed;
    }
}