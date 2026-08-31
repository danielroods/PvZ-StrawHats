package model.utils;

import controller.QuestManager;
import model.collections.Item;
import model.collections.item.GroundCoin;
import model.collections.item.GroundDiamond;
import model.collections.item.GroundItem;
import model.collections.item.GroundSeedPack;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.user_data.User;
import model.user_data.UserState;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;

/**
 * What a dying zombie leaves behind and what happens when the player picks a ground
 * item up, including the console announcements the text engine prints for each.
 */
class SessionDrops {

    private final GameSession session;

    SessionDrops(GameSession session) {
        this.session = session;
    }

    void notifyZombieDied(Zombie zombie, String killerName) {
        List<Item> items = session.getItems();
        if (zombie == null) return;
        Position dropPosition = zombie.getPosition();
        if (dropPosition == null) return;

        int displayX = (int) Math.round(dropPosition.x()) + 1;
        int displayY = (int) Math.round(dropPosition.y()) + 1;
        GeneralPrinter.print("Zombie of type " + zombie.getName() + " is dead at ("
                + displayX + ", " + displayY + ").");

        GroundCoin coin = new GroundCoin(dropPosition, GroundCoin.CoinTier.rollRandom());
        items.add(coin);
        GeneralPrinter.print("A " + coin.getTier().name().toLowerCase()
                + " coin dropped at (" + displayX + ", " + displayY + ").");

        if (GameSession.ITEM_RANDOM.nextInt(100) < 10) {
            items.add(new GroundDiamond(dropPosition, 1));
            GeneralPrinter.print("A diamond dropped at (" + displayX + ", " + displayY + ").");
        }

        if (User.currentUser != null) {
            UserState state = User.currentUser.userState;
            if (!state.unlockedPlantIds.isEmpty() && GameSession.ITEM_RANDOM.nextInt(100) < 5) {
                List<Integer> unlocked = new ArrayList<>(state.unlockedPlantIds);
                int plantId = unlocked.get(GameSession.ITEM_RANDOM.nextInt(unlocked.size()));
                items.add(new GroundSeedPack(dropPosition, plantId, 1));
                GeneralPrinter.print("A seed pack dropped at (" + displayX + ", " + displayY + ").");
            }
        }

        QuestManager.notifyZombieKilled(session, zombie, killerName);
    }

    List<GroundItem> collectItemsNear(Position target) {
        List<GroundItem> collectedItems = new ArrayList<>();
        if (User.currentUser == null || target == null) return collectedItems;

        UserState state = User.currentUser.userState;
        for (Item item : new ArrayList<>(session.getItems())) {
            if (item instanceof GroundItem groundItem
                    && groundItem.isAlive()
                    && !groundItem.isCollected()
                    // All ground items, including every type of sky sun,
                    // are collectible by an explicit player click.
                    && groundItem.isNear(target)) {
                groundItem.collect(session, state);
                collectedItems.add(groundItem);
            }
        }
        return collectedItems;
    }

}