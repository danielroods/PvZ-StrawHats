package model.pitches.obstacles;

public enum ObstacleInformation {
    CRATER,       // left behind when a zombie eats a plant (Beghouled); permanently unplantable
    GRAVE,        // Egypt / Dark Age gravestone; zombies can rise from it (see model.pitches.obstacles.Grave, DarkAge.necromancy)
    OCTOPUS_WRAP, // Octopus zombie's grapple; see model.pitches.obstacles.OctopusWrap
    ICE_BLOCK,    // maxed-out chill from repeated snowball hits; see model.pitches.obstacles.IceBlock
    BRIDGE        // Pirate Seas plank crossing a water row; see model.pitches.obstacles.Bridge
}