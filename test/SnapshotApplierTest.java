import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.projectile.Projectile;
import model.utils.GameSession;
import net.client.SnapshotApplier;
import net.dto.MatchSnapshot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotApplierTest {

    @BeforeAll
    static void initFactories() {
        PlantFactory.autoInit();
        ZombieFactory.init();
    }

    private MatchSnapshot snapshotWithOneOfEach() {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.brains = new int[] {300, 300, 300, 300, 300};

        MatchSnapshot.PlantDto plant = new MatchSnapshot.PlantDto();
        plant.id = 1;
        plant.plantId = PlantFactory.findPlantIdByName("Peashooter");
        plant.name = "Peashooter";
        plant.row = 2;
        plant.col = 3;
        plant.hp = 300;
        plant.maxHp = 300;
        plant.intervalTimer = 1.4;
        snapshot.plants.add(plant);

        MatchSnapshot.ZombieDto zombie = new MatchSnapshot.ZombieDto();
        zombie.id = 2;
        zombie.alias = "ZombieDefault";
        zombie.row = 2;
        zombie.x = 7.5;
        zombie.hp = 190;
        zombie.maxHp = 190;
        zombie.state = "WALKING";
        zombie.status = "NORMAL";
        snapshot.zombies.add(zombie);

        MatchSnapshot.ProjectileDto projectile = new MatchSnapshot.ProjectileDto();
        projectile.id = 3;
        projectile.sourceId = 1;
        projectile.sourcePlantId = plant.plantId;
        projectile.sourceName = "Peashooter";
        projectile.x = 4.0;
        projectile.y = 2.0;
        projectile.vx = 3.3;
        snapshot.projectiles.add(projectile);

        return snapshot;
    }

    @Test
    void aSnapshotRebuildsPlantsZombiesAndProjectilesInTheShadowSession() {
        GameSession shadow = new GameSession(5, 9);
        SnapshotApplier applier = new SnapshotApplier(shadow);

        applier.apply(snapshotWithOneOfEach());

        assertEquals(1, shadow.getPlants().size());
        Plant plant = shadow.getPlants().get(0);
        assertEquals("Peashooter", plant.getName());
        assertEquals(3, Math.round(plant.getPosition().x()));
        assertEquals(2, Math.round(plant.getPosition().y()));
        assertEquals(1.4, plant.getIntervalTimer(), 0.0001,
                "the shot timer is mirrored so the firing animation plays");
        assertTrue(shadow.getEnvironment().getCell(2, 3).hasPlant(),
                "the plant is placed on its own tile, not just in the list");

        assertEquals(1, shadow.getZombies().size());
        Zombie zombie = shadow.getZombies().get(0);
        assertEquals(7.5, zombie.getPosition().x(), 0.0001);
        assertEquals(2, Math.round(zombie.getPosition().y()));

        assertEquals(1, shadow.getProjectiles().size());
        Projectile projectile = shadow.getProjectiles().get(0);
        assertTrue(projectile.isVisible(), "a synced projectile is never stuck in its spawn delay");
        assertNotNull(projectile.getSourcePlant(),
                "the source plant is resolved so the pea art can be picked");
        assertEquals("Peashooter", projectile.getSourcePlant().getName());
        assertEquals(4.0, projectile.getPosition().x(), 0.0001);
    }

    @Test
    void projectilesKeepMovingBetweenSnapshots() {
        GameSession shadow = new GameSession(5, 9);
        SnapshotApplier applier = new SnapshotApplier(shadow);
        applier.apply(snapshotWithOneOfEach());

        double before = shadow.getProjectiles().get(0).getPosition().x();
        applier.advanceProjectiles(0.05f);
        double after = shadow.getProjectiles().get(0).getPosition().x();

        assertEquals(before + 3.3 * 0.05, after, 0.001,
                "between snapshots the client dead-reckons at the server's own speed");
    }

    @Test
    void entitiesMissingFromTheNextSnapshotAreRemoved() {
        GameSession shadow = new GameSession(5, 9);
        SnapshotApplier applier = new SnapshotApplier(shadow);
        applier.apply(snapshotWithOneOfEach());

        MatchSnapshot empty = new MatchSnapshot();
        empty.brains = new int[] {300, 300, 300, 300, 300};
        applier.apply(empty);

        assertTrue(shadow.getPlants().isEmpty());
        assertTrue(shadow.getZombies().isEmpty());
        assertTrue(shadow.getProjectiles().isEmpty());
        assertEquals(1, applier.drainRemovedZombies().size(),
                "a vanished zombie is handed to the death-animation tracker");
    }
}
