import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.match_mechanisms.vector.Position;
import model.projectile.LobArcMove;
import model.projectile.Projectile;
import model.projectile.ProjectileImpact;
import model.utils.GameSession;
import net.JsonLine;
import net.client.SnapshotApplier;
import net.dto.MatchSnapshot;
import net.server.MatchSnapshotBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetProjectileSyncTest {

    @BeforeAll
    static void initFactories() {
        PlantFactory.autoInit();
        ZombieFactory.init();
    }

    private MatchSnapshot lobSnapshot(double travelled) {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.brains = new int[] {300, 300, 300, 300, 300};

        MatchSnapshot.ProjectileDto dto = new MatchSnapshot.ProjectileDto();
        dto.id = 7;
        dto.sourceName = "Cabbage-pult";
        dto.sourcePlantId = PlantFactory.findPlantIdByName("Cabbage-pult");
        dto.kind = "LOB";
        dto.motion = new double[] {1.0, 2.0, 6.0, 2.0, 1.4, 4.5, travelled};
        dto.vx = 4.5;
        dto.x = 1.0 + travelled;
        dto.y = 2.0;
        snapshot.projectiles.add(dto);
        return snapshot;
    }

    @Test
    void aLobbedShotIsRebuiltAsAnArcInsteadOfADeadReckonedStraightLine() {
        GameSession shadow = new GameSession(5, 9);
        SnapshotApplier applier = new SnapshotApplier(shadow);

        applier.apply(lobSnapshot(0.5));
        assertEquals(1, shadow.getProjectiles().size());
        Projectile mirrored = shadow.getProjectiles().get(0);
        assertInstanceOf(LobArcMove.class, mirrored.getMoveStrategy(),
                "a LOB shot must come back as a real arc on the client");
        assertTrue(mirrored.isLobbed());
        assertTrue(mirrored.isVisible(), "a mirrored shot is never stuck in its spawn delay");

        double startY = mirrored.getPosition().y();
        double highest = startY;
        for (int i = 0; i < 20; i++) {
            applier.advanceProjectiles(0.05f);
            highest = Math.min(highest, mirrored.getPosition().y());
        }

        assertTrue(highest < startY - 0.5,
                "the client must replay the curve, not fly the cabbage flat along its lane");
        assertTrue(mirrored.getPosition().x() > 1.5, "the shot still travels forward");
    }

    @Test
    void aLobIsCorrectedAlongItsOwnArcRatherThanBeingYankedSideways() {
        GameSession shadow = new GameSession(5, 9);
        SnapshotApplier applier = new SnapshotApplier(shadow);

        applier.apply(lobSnapshot(0.5));
        Projectile mirrored = shadow.getProjectiles().get(0);
        applier.advanceProjectiles(0.10f);

        applier.apply(lobSnapshot(1.4));
        LobArcMove arc = (LobArcMove) mirrored.getMoveStrategy();

        assertTrue(arc.getTravelledX() > 0.95 && arc.getTravelledX() < 1.4,
                "the correction eases onto the server's progress, got " + arc.getTravelledX());
        assertEquals(1.0 + arc.getTravelledX(), mirrored.getPosition().x(), 1.0e-6,
                "the corrected position is recomputed from the arc, so the curve stays intact");
    }

    @Test
    void impactsCrossTheWireSoTheClientPlaysSplatsOnlyForRealHits() {
        GameSession shadow = new GameSession(5, 9);
        SnapshotApplier applier = new SnapshotApplier(shadow);

        MatchSnapshot snapshot = lobSnapshot(0.5);
        MatchSnapshot.ImpactDto impact = new MatchSnapshot.ImpactDto();
        impact.plantName = "Melon-pult";
        impact.plantFood = true;
        impact.assetVariant = 1;
        impact.x = 5.25;
        impact.y = 3.0;
        snapshot.impacts.add(impact);

        applier.apply(snapshot);

        List<ProjectileImpact> queued = shadow.drainProjectileImpacts();
        assertEquals(1, queued.size());
        assertEquals("Melon-pult", queued.get(0).plantName());
        assertTrue(queued.get(0).plantFood());
        assertEquals(1, queued.get(0).assetVariant());
        assertEquals(5.25, queued.get(0).position().x(), 1.0e-9);
        assertEquals(3.0, queued.get(0).position().y(), 1.0e-9);

        applier.apply(lobSnapshot(1.0));
        assertTrue(shadow.drainProjectileImpacts().isEmpty(),
                "a snapshot with no hits must not queue a splat");
    }

    @Test
    void theServerPublishesArcParametersAndImpactsForALobbedShot() {
        IZombieMatch match = new IZombieMatch(60.0, List.of("Cabbage-pult"), null);
        GameSession server = match.getSession();
        GameSession.setCurrent(server);
        clearLawn(server);

        Plant pult = PlantFactory.createPlantByName("Cabbage-pult", 1, new Position(1, 2));
        assertTrue(server.plantAt(2, 1, pult));
        Zombie target = ZombieFactory.create("ZombieDefault", 2, 5);
        target.setPosition(new Position(5.0, 2.0));
        target.setSpeed(Position.ShowZero());
        target.setHP(100000);
        server.getZombies().add(target);

        MatchSnapshotBuilder builder = new MatchSnapshotBuilder();
        MatchSnapshot.ProjectileDto lob = null;
        MatchSnapshot withImpact = null;

        for (int tick = 0; tick < 120 && withImpact == null; tick++) {
            GameSession.setCurrent(server);
            List<Plant> before = new ArrayList<>(server.getPlants());
            match.tick();
            builder.captureRemovals(match, before, new ArrayList<>(server.getZombieProjectiles()));
            MatchSnapshot snapshot = roundTrip(builder.build(match, Role.PLANTS, tick));
            builder.clearRemovals();

            for (MatchSnapshot.ProjectileDto dto : snapshot.projectiles) {
                if ("LOB".equals(dto.kind)) lob = dto;
            }
            if (!snapshot.impacts.isEmpty()) withImpact = snapshot;
        }

        assertNotNull(lob, "the pult's shot was never published as a LOB");
        assertNotNull(lob.motion, "a LOB carries the arc the client has to rebuild");
        assertEquals(7, lob.motion.length);
        assertEquals("Cabbage-pult", lob.sourceName);
        assertFalse(lob.plantFood);
        assertEquals(1.0, lob.motion[0], 1.0e-6, "the arc starts at the plant's own column");

        assertNotNull(withImpact, "landing on the zombie was never published as an impact");
        assertEquals("Cabbage-pult", withImpact.impacts.get(0).plantName);
    }

    private MatchSnapshot roundTrip(MatchSnapshot snapshot) {
        return JsonLine.fromTree(JsonLine.compactTree(snapshot), MatchSnapshot.class);
    }

    private void clearLawn(GameSession session) {
        for (Plant plant : new ArrayList<>(session.getPlants())) plant.setAlive(false);
        session.getPlants().clear();
        for (int row = 0; row < session.getRows(); row++) {
            for (int col = 0; col < session.getCols(); col++) {
                var cell = session.getEnvironment().getCell(row, col);
                if (cell != null && cell.hasPlant()) cell.setPlant(null);
            }
        }
        session.getZombies().clear();
        session.getProjectiles().clear();
        session.drainProjectileImpacts();
    }
}
