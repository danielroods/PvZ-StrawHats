package model.match.main.season;

import model.match.main.season.travellog.beach.Beach;
import model.match.main.season.travellog.cave.Cave;
import model.match.main.season.travellog.darkage.DarkAge;
import model.match.main.season.travellog.egypt.Egypt;
import model.match.main.season.travellog.future.Future;
import model.match.main.season.travellog.pirate.Pirate;

public class SeasonFactory {
    public static Season create(String name) {
        String lower = name.toLowerCase().replace(" ", "_");
        switch (lower) {
            case "egypt": return new Egypt();
            case "pirates":
            case "pirate": return new Pirate();
            case "frostbite_caves":
            case "cave":  return new Cave();
            case "big_wave_beach":
            case "beach": return new Beach();
            case "dark_ages":
            case "darkage": return new DarkAge();
            case "future": return new Future();
            default: throw new IllegalArgumentException("Unknown season: " + name);
        }
    }
}