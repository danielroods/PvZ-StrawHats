package controller.ui_menus.greenhouse;

import model.greenhouse.Pot;
import model.greenhouse.PotPlant;
import model.user_data.User;
import model.user_data.UserState;

public class PotController {
    private final Pot pot;

    public PotController(Pot pot) {
        this.pot = pot;
    }

    public String collect(UserState state) {
        PotPlant plant = pot.getPotPlant();
        if (plant == null) return "Error: this pot is empty.";
        if (!plant.isCollectAble()) return "Error: this plant is not ready to harvest yet.";

        String message;
        if (plant.isMarigold()) {
            state.coins += plant.getAwardCoins();
            message = "Collected Marigold; 500 coins awarded. You now have " + state.coins + " coins.";
        } else {
            boolean granted = state.grantBoost(plant.getPlantId());
            message = granted
                    ? "Collected " + plant.getPlantName() + "; a boost has been reserved for it."
                    : "Collected " + plant.getPlantName() + "; a boost was already saved, no extra boost granted.";
        }

        removePlant();
        User.save();
        return message;
    }

    public int calculateGrowCost() {
        PotPlant plant = pot.getPotPlant();
        if (plant == null) return 0;

        long remainingSeconds = plant.getRemainingSeconds();
        if (remainingSeconds <= 0) return 0;

        double remainingHours = remainingSeconds / 3600.0;
        return (int) Math.ceil(remainingHours);
    }

    public String grow(UserState state) {
        PotPlant plant = pot.getPotPlant();
        if (plant == null) return "Error: this pot is empty.";
        if (plant.isCollectAble()) return "Error: this plant is already fully grown.";

        int cost = calculateGrowCost();
        if (state.diamonds < cost) return "Error: not enough diamonds (need " + cost + ").";

        state.diamonds -= cost;
        plant.growInstantly();
        User.save();
        return "Growth accelerated for " + cost + " diamond(s); " + state.diamonds + " diamonds remaining.";
    }

    public void removePlant() {
        pot.setPotPlant(null);
    }
}
