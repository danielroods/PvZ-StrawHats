package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.Item;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.utils.GameSession;
import service.GameClock;

import java.util.ArrayList;
import java.util.List;

public class MageState implements ZombieEffectStatus {
    private final double hexCooldown;
    private double spellTimer = 0.0;

    private final List<Item> hexedTargetsList = new ArrayList<>();
    private boolean isDispellCompleted = false;

    /** Read-only view for the renderer, to tell a wizard-hexed plant apart from other INCAPACITATED causes (e.g. octopus wrap). */
    public boolean isHexed(Item target) {
        return hexedTargetsList.contains(target);
    }

    public MageState(double hexCooldown) {
        this.hexCooldown = hexCooldown;
    }

    @Override
    public void applyTickEffect(Zombie spellcaster, GameSession session) {
        if (!spellcaster.isAlive()) return;

        spellTimer += GameClock.SECONDS_PER_TICK;
        if (spellTimer >= hexCooldown) {
            spellTimer = 0;
            castHexOnRandomObjective(spellcaster, session);
        }

        interceptPhysicalCollisions(spellcaster, session);
    }

    @Override
    public void onDeath(Zombie target, GameSession session) {
        if (isDispellCompleted) return;
        for (Item targetedCursed : hexedTargetsList) {
            if (!targetedCursed.isAlive()) continue;
            if (targetedCursed instanceof Plant vegetation) {
                vegetation.setState(Plant.PlantState.ACTIVE);
            } else if (targetedCursed instanceof Zombie zombieServant) {
                zombieServant.setStatus(Zombie.Status.NORMAL);
            }
        }
        hexedTargetsList.clear();
        isDispellCompleted = true;
    }

    private void castHexOnRandomObjective(Zombie warlock, GameSession session) {
        List<Item> prospectiveTargets = new ArrayList<>();

        if (warlock.getFaction() == Faction.ZOMBIES) {
            for (Plant plant : session.getPlants()) {
                if (plant.isAlive() && plant.getPlantState() != Plant.PlantState.INCAPACITATED) {
                    prospectiveTargets.add(plant);
                }
            }
        } else {
            for (Zombie deadwalker : session.getZombies()) {
                if (deadwalker.isAlive() && deadwalker != warlock
                        && deadwalker.getStatus() != Zombie.Status.FREEZE
                        && deadwalker.getStatus() != Zombie.Status.FROZEN) {
                    prospectiveTargets.add(deadwalker);
                }
            }
        }

        if (!prospectiveTargets.isEmpty()) {
            int targetIdx = (int) (Math.random() * prospectiveTargets.size());
            applyCurseDebuff(prospectiveTargets.get(targetIdx));
        }
    }

    private void interceptPhysicalCollisions(Zombie warlock, GameSession session) {
        Item directTarget = warlock.acquireTarget(session);
        if (directTarget != null && directTarget.isAlive()) {
            applyCurseDebuff(directTarget);
        }
    }

    private void applyCurseDebuff(Item sacrificialEntity) {
        if (sacrificialEntity instanceof Plant plant) {
            plant.setState(Plant.PlantState.INCAPACITATED);
        } else if (sacrificialEntity instanceof Zombie zombie) {
            zombie.setStatus(Zombie.Status.FREEZE);
        }

        if (!hexedTargetsList.contains(sacrificialEntity)) {
            hexedTargetsList.add(sacrificialEntity);
        }
    }
}