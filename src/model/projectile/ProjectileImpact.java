package model.projectile;

import model.match_mechanisms.vector.Position;

public record ProjectileImpact(String plantName, boolean plantFood, int assetVariant,
                               Position position) {
}
