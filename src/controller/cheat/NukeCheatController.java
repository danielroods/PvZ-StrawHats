package controller.cheat;

import model.collections.zombie.Zombie;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;


public class NukeCheatController {

    
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