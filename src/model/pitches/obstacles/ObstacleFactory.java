package model.pitches.obstacles;

public final class ObstacleFactory {
    private ObstacleFactory() { }

    public static Obstacle create(ObstacleInformation kind) {
        return switch (kind) {
            case CRATER -> new Crater();
            case GRAVE -> new Grave();
            case OCTOPUS_WRAP -> throw new UnsupportedOperationException(
                    "OctopusWrap needs a Plant and hp - construct it directly: new OctopusWrap(plant, hp).");
            case ICE_BLOCK -> throw new UnsupportedOperationException(
                    "IceBlock needs a frozen Plant/Zombie and hp - construct it directly.");
            case BRIDGE -> null;
        };
    }
}
