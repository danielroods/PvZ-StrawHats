package model.match.main.levels.special_levels;

import model.match.main.levels.Level;
import model.utils.GameSession;

public class LoveYourPlantsLevel extends Level {
    private int maxPlantLoss = 3; // if more than this many plants die, you lose
    private int plantsLost = 0;

    @Override
    public void initSpecial(GameSession session) {
        plantsLost = 0;
    }

    
    public void recordPlantLoss() {
        plantsLost++;
    }

    @Override
    public boolean checkLossCondition(GameSession session) {
        return plantsLost >= maxPlantLoss;
    }

    public int getPlantsLost() { return plantsLost; }

    public int getMaxPlantLoss() { return maxPlantLoss; }
    public void setMaxPlantLoss(int maxPlantLoss) { this.maxPlantLoss = maxPlantLoss; }
}
