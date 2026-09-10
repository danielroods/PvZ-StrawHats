package model.match.mini_games;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import model.utils.ResourceResolver;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightweight standalone simulation for the Console's Zombie Packman game.
 * It deliberately does not use GameSession: the board is 240x240 and needs its own
 * continuous, camera-following movement model rather than the 5x9 PvZ lane model.
 */
public final class ZombiePackmanGame {
    public static final int WIDTH = 240, HEIGHT = 240;
    public static final int MAX_LIVES = 9;
    private static final int COIN_SPACING = 3;
    private static final float COIN_PICKUP_RADIUS = 0.85f;

    public enum Direction { NONE, UP, DOWN, LEFT, RIGHT }

    public static final class RectDef {
        public int x, y, w, h;
        public boolean contains(int tx, int ty) {
            return tx >= x && tx < x + w && ty >= y && ty < y + h;
        }
    }

    public static final class PointDef { public int x, y; }
    public static final class PlantDef { public String type; public int x, y; }
    public static final class PowerDef { public String type; public int x, y; }
    public static final class GhostDef { public int x, y; }
    public static final class TunnelDef { public int y; public String from, to; }

    public static final class MapDef {
        public int version;
        public int width = WIDTH, height = HEIGHT, tileSize = 32;
        public PointDef spawn;
        public List<TunnelDef> tunnels = new ArrayList<>();
        public List<RectDef> walls = new ArrayList<>();
        public List<PointDef> coinExclusions = new ArrayList<>();
        public List<PlantDef> plants = new ArrayList<>();
        public List<PowerDef> powerups = new ArrayList<>();
        public List<GhostDef> ghosts = new ArrayList<>();
    }

    public static final class PlantState {
        public final String type;
        public final int x, y;
        public float hp = 1f;
        public float shootTimer;
        public float disableTimer;
        public float eatTimer;
        public boolean alive = true;

        PlantState(PlantDef d) {
            type = d.type; x = d.x; y = d.y;
            hp = type.equalsIgnoreCase("Wall-nut") ? 4.5f : 0.7f;
        }
        public boolean isWallNut() { return type.equalsIgnoreCase("Wall-nut"); }
    }

    /** Personality index: 0 Blinky (direct chaser), 1 Pinky (ambusher), 2 Inky (erratic), 3 Clyde (shy). */
    public static final class GhostState {
        public float x, y;
        public final float spawnX, spawnY;
        public final int personality;
        public boolean chasing;
        public float pathTimer;
        public float frightRecoverTimer;
        public Direction direction = Direction.LEFT;
        GhostState(GhostDef d, int personality) { x=d.x+0.5f; y=d.y+0.5f; spawnX=x; spawnY=y; this.personality=personality; }
        public boolean isRecovering() { return frightRecoverTimer > 0; }
    }

    public static final class Projectile {
        public float x, y, vx, vy;
        public final boolean pea;
        public final boolean octopus;
        public float life = 7f;
        public float damage;
        Projectile(float x, float y, float vx, float vy, boolean pea, boolean octopus, float damage) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.pea=pea; this.octopus=octopus; this.damage=damage;
        }
    }

    public static final class PowerState {
        public String type;
        public int x, y;
        public boolean active = true;
        PowerState(PowerDef d) { type=d.type; x=d.x; y=d.y; }
    }

    private final MapDef map;
    private final Set<Long> walls = new HashSet<>();
    private final Set<Long> coins = new HashSet<>();
    private final List<PlantState> plants = new ArrayList<>();
    private final List<GhostState> ghosts = new ArrayList<>();
    private final List<PowerState> powerups = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    private float x, y;
    private float spawnX, spawnY;
    private Direction direction = Direction.NONE;
    private Direction desiredDirection = Direction.NONE;
    private boolean eating;
    private float eatPulse;
    private float invulnerableTimer;
    private float transformEffectTimer;
    private float eatAnimTimer;
    private int lives = MAX_LIVES;
    private int coinsCollected;
    private int totalCoins;
    private float octopusTimer;
    private float zombotanyTimer;
    private float potionTimer;
    private float zoybeanTimer;
    private float zoybeanFlashTimer;
    private float winDelay;
    private boolean gameOver;
    private boolean won;

    public ZombiePackmanGame() { this(loadMap()); }

    public ZombiePackmanGame(MapDef map) {
        this.map = map;
        for (RectDef r : map.walls) {
            for (int xx=r.x; xx<r.x+r.w; xx++) for (int yy=r.y; yy<r.y+r.h; yy++) walls.add(key(xx,yy));
        }
        Set<Long> excluded = new HashSet<>();
        for (PointDef p : map.coinExclusions) excluded.add(key(p.x,p.y));
        for (PlantDef p : map.plants) {
            if (!walls.contains(key(p.x,p.y))) {
                plants.add(new PlantState(p));
                excluded.add(key(p.x,p.y));
            }
        }
        for (GhostDef g : map.ghosts) {
            if (!walls.contains(key(g.x,g.y))) ghosts.add(new GhostState(g, ghosts.size() % 4));
            excluded.add(key(g.x,g.y));
        }
        for (PowerDef p : map.powerups) {
            powerups.add(new PowerState(p));
            excluded.add(key(p.x,p.y));
        }
        for (int xx=0; xx<map.width; xx++) for (int yy=0; yy<map.height; yy++) {
            if (xx % COIN_SPACING != 0 || yy % COIN_SPACING != 0) continue;
            if (!walls.contains(key(xx,yy)) && !excluded.contains(key(xx,yy))) coins.add(key(xx,yy));
        }
        totalCoins = coins.size();
        PointDef s = map.spawn == null ? null : map.spawn;
        spawnX = (s == null ? 100 : s.x) + 0.5f;
        spawnY = (s == null ? 100 : s.y) + 0.5f;
        x = spawnX; y = spawnY;
    }

    private static MapDef loadMap() {
        try (InputStream is = ResourceResolver.open("ZombiePackmanMap.json")) {
            if (is == null) throw new IllegalStateException("ZombiePackmanMap.json not found");
            return new Gson().fromJson(new InputStreamReader(is), MapDef.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load ZombiePackmanMap.json", e);
        }
    }

    public MapDef getMap() { return map; }
    public List<PlantState> getPlants() { return plants; }
    public List<GhostState> getGhosts() { return ghosts; }
    public List<PowerState> getPowerups() { return powerups; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public Set<Long> getCoins() { return coins; }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getLives() { return lives; }
    public int getCoinsCollected() { return coinsCollected; }
    public int getTotalCoins() { return totalCoins; }
    public float getOctopusTimer() { return octopusTimer; }
    public float getZombotanyTimer() { return zombotanyTimer; }
    public float getPotionTimer() { return potionTimer; }
    public float getZoybeanTimer() { return zoybeanTimer; }
    public float getTransformEffectTimer() { return transformEffectTimer; }
    public boolean isEating() { return eating; }
    public boolean isGameOver() { return gameOver; }
    public boolean isWon() { return won; }
    public Direction getDirection() { return direction; }
    public int getTileX() { return (int)Math.floor(x); }
    public int getTileY() { return (int)Math.floor(y); }
    public float getSpeed() { return potionTimer > 0 ? 8.5f : 5.8f; }
    public float getDamageMultiplier() { return potionTimer > 0 ? 2.0f : 1.0f; }

    public void setDesiredDirection(Direction d) { if (!gameOver) desiredDirection = d; }
    public void setEating(boolean value) { eating = value; }

    public void update(float delta) {
        if (gameOver || won) return;
        delta = Math.min(delta, 0.05f);
        invulnerableTimer = Math.max(0, invulnerableTimer-delta);
        transformEffectTimer = Math.max(0, transformEffectTimer-delta);
        octopusTimer = Math.max(0, octopusTimer-delta);
        zombotanyTimer = Math.max(0, zombotanyTimer-delta);
        potionTimer = Math.max(0, potionTimer-delta);
        zoybeanTimer = Math.max(0, zoybeanTimer-delta);
        zoybeanFlashTimer = Math.max(0, zoybeanFlashTimer-delta);
        eatAnimTimer += delta;

        if (canMove(desiredDirection)) direction = desiredDirection;
        float speed = getSpeed();
        if (direction != Direction.NONE) {
            float nx=x, ny=y;
            if (direction==Direction.LEFT) nx -= speed*delta;
            if (direction==Direction.RIGHT) nx += speed*delta;
            if (direction==Direction.UP) ny += speed*delta;
            if (direction==Direction.DOWN) ny -= speed*delta;
            if (canPlayerOccupy(nx,ny)) { x=nx; y=ny; }
            else if (!canMove(direction)) direction=Direction.NONE;
        }
        applyTunnels();
        collectCoins();
        collectPowerups();
        updateEating(delta);
        updatePlants(delta);
        updateProjectiles(delta);
        updateGhosts(delta);
        if (coins.isEmpty()) { won=true; winDelay=0.1f; }
    }

    private void updateEating(float delta) {
        if (!eating) { eatPulse=0; return; }
        eatPulse += delta;
        if (eatPulse < 0.20f) return;
        eatPulse = 0;
        PlantState p = targetPlant();
        if (p != null) {
            float damage = (p.isWallNut() ? 0.23f : 1f) * getDamageMultiplier();
            p.hp -= damage;
            p.eatTimer = 0.35f;
            if (p.hp <= 0) p.alive=false;
        }
    }

    private PlantState targetPlant() {
        float tx=x, ty=y;
        if (direction==Direction.LEFT) tx-=0.8f;
        else if (direction==Direction.RIGHT) tx+=0.8f;
        else if (direction==Direction.UP) ty+=0.8f;
        else if (direction==Direction.DOWN) ty-=0.8f;
        for (PlantState p:plants) if (p.alive && Math.abs(p.x+0.5f-tx)<0.72f && Math.abs(p.y+0.5f-ty)<0.72f) return p;
        return null;
    }

    private void updatePlants(float delta) {
        for (PlantState p:plants) {
            if (!p.alive) continue;
            p.eatTimer=Math.max(0,p.eatTimer-delta);
            p.disableTimer=Math.max(0,p.disableTimer-delta);
            p.shootTimer -= delta;
            if (p.disableTimer>0 || p.shootTimer>0 || !isShooter(p.type)) continue;
            if (hasLineOfSight(p.x+0.5f,p.y+0.5f,x,y)) {
                firePlant(p);
                p.shootTimer = shootInterval(p.type);
            } else p.shootTimer = 0.25f;
        }
        for (PlantState p:plants) {
            if (!p.alive || !p.type.equalsIgnoreCase("Cherry Bomb") || p.disableTimer>0) continue;
            p.shootTimer -= delta;
            if (p.shootTimer<=0 && distance(p.x+0.5f,p.y+0.5f,x,y)<5.5f) {
                hurtPlayer(); p.shootTimer=7f;
            }
        }
    }

    private boolean isShooter(String t) {
        return t.equalsIgnoreCase("Peashooter") || t.equalsIgnoreCase("Repeater") ||
                t.equalsIgnoreCase("Snow Pea") || t.equalsIgnoreCase("Melon-pult") || t.equalsIgnoreCase("Citron");
    }
    private float shootInterval(String t) {
        if (t.equalsIgnoreCase("Repeater")) return 1.15f;
        if (t.equalsIgnoreCase("Snow Pea")) return 1.45f;
        if (t.equalsIgnoreCase("Citron")) return 2.0f;
        if (t.equalsIgnoreCase("Melon-pult")) return 1.8f;
        return 1.35f;
    }
    private void firePlant(PlantState p) {
        float dx=x-(p.x+0.5f), dy=y-(p.y+0.5f);
        float vx=0,vy=0;
        if (Math.abs(dx)>Math.abs(dy)) vx=Math.signum(dx)*7f; else vy=Math.signum(dy)*7f;
        if (p.type.equalsIgnoreCase("Repeater")) {
            projectiles.add(new Projectile(p.x+0.5f,p.y+0.5f,vx,vy,true,false,1));
            projectiles.add(new Projectile(p.x+0.5f,p.y+0.5f,vx,vy,true,false,1));
        } else {
            float d = p.type.equalsIgnoreCase("Citron") ? 2f : 1f;
            projectiles.add(new Projectile(p.x+0.5f,p.y+0.5f,vx,vy,true,false,d));
        }
    }

    private void updateProjectiles(float delta) {
        for (int i=projectiles.size()-1;i>=0;i--) {
            Projectile p=projectiles.get(i);
            p.life-=delta; p.x+=p.vx*delta; p.y+=p.vy*delta;
            if (p.life<=0 || !canOccupy(p.x,p.y)) { projectiles.remove(i); continue; }
            if (distance(p.x,p.y,x,y)<0.55f) { hurtPlayer(); projectiles.remove(i); continue; }
            if (p.octopus) {
                for (PlantState plant:plants) if (plant.alive && distance(p.x,p.y,plant.x+0.5f,plant.y+0.5f)<0.75f) {
                    plant.disableTimer=3f; p.life=0; break;
                }
            }
        }
    }

    private static final float[] GHOST_CHASE_SPEED = {4.4f, 4.0f, 4.2f, 3.8f};
    private static final float[] GHOST_PATROL_SPEED = {2.3f, 2.2f, 2.2f, 2.0f};

    private void updateGhosts(float delta) {
        boolean frightened = zoybeanTimer > 0;
        for (GhostState g:ghosts) {
            g.frightRecoverTimer = Math.max(0, g.frightRecoverTimer-delta);
            boolean recovering = g.isRecovering();
            float dist=distance(g.x,g.y,x,y);
            float speed;
            if (recovering) { g.chasing=false; speed=3.4f; }
            else if (frightened) { g.chasing=false; speed=3.0f; }
            else {
                g.chasing = dist<12f || (g.chasing && dist<18f);
                speed = g.chasing ? GHOST_CHASE_SPEED[g.personality] : GHOST_PATROL_SPEED[g.personality];
            }
            g.pathTimer-=delta;
            if (g.pathTimer<=0) {
                g.pathTimer=0.25f;
                Direction d=chooseGhostDirection(g,frightened,recovering);
                if (d!=Direction.NONE) g.direction=d;
            }
            float nx=g.x,ny=g.y;
            if(g.direction==Direction.LEFT)nx-=speed*delta;
            if(g.direction==Direction.RIGHT)nx+=speed*delta;
            if(g.direction==Direction.UP)ny+=speed*delta;
            if(g.direction==Direction.DOWN)ny-=speed*delta;
            if(canOccupy(nx,ny)) {g.x=nx;g.y=ny;} else g.pathTimer=0;
            if(distance(g.x,g.y,x,y)<0.65f) {
                if (recovering) { /* eyes heading home - harmless */ }
                else if (frightened) eatGhost(g);
                else hurtPlayer();
            }
        }
    }

    private void eatGhost(GhostState g) {
        g.x=g.spawnX; g.y=g.spawnY; g.frightRecoverTimer=3f; g.chasing=false;
    }

    private Direction chooseGhostDirection(GhostState g, boolean frightened, boolean recovering) {
        float targetX, targetY;
        boolean maximize;
        if (recovering) { targetX=g.spawnX; targetY=g.spawnY; maximize=false; }
        else if (frightened) { targetX=x; targetY=y; maximize=true; }
        else { float[] t=personalityTarget(g); targetX=t[0]; targetY=t[1]; maximize=false; }

        Direction[] dirs={Direction.UP,Direction.DOWN,Direction.LEFT,Direction.RIGHT};
        Direction best=Direction.NONE;
        float bestScore = maximize ? -Float.MAX_VALUE : Float.MAX_VALUE;
        for(Direction d:dirs){
            float nx=g.x,ny=g.y;
            if(d==Direction.LEFT)nx-=1;if(d==Direction.RIGHT)nx+=1;if(d==Direction.UP)ny+=1;if(d==Direction.DOWN)ny-=1;
            if(!canOccupy(nx,ny))continue;
            float score=distance(nx,ny,targetX,targetY);
            if(!g.chasing && !maximize && !recovering) score += (float)(Math.random()*4);
            boolean better = maximize ? score>bestScore : score<bestScore;
            if(better){bestScore=score;best=d;}
        }
        return best;
    }

    /** Where each ghost personality currently wants to head, in world (tile) coordinates. */
    private float[] personalityTarget(GhostState g) {
        switch (g.personality) {
            case 1: { // Pinky: ambush a few tiles ahead of the player's current heading
                float ax=x, ay=y;
                if(direction==Direction.LEFT) ax-=4; else if(direction==Direction.RIGHT) ax+=4;
                else if(direction==Direction.UP) ay+=4; else if(direction==Direction.DOWN) ay-=4;
                return new float[]{ax, ay};
            }
            case 2: // Inky: erratic - occasionally lunges at a random offset instead of the player directly
                if (Math.random() < 0.35) return new float[]{x + (float)(Math.random()*10-5), y + (float)(Math.random()*10-5)};
                return new float[]{x, y};
            case 3: // Clyde: shy - chases from afar but retreats toward home once close
                if (distance(g.x,g.y,x,y) < 5f) return new float[]{g.spawnX, g.spawnY};
                return new float[]{x, y};
            default: // Blinky: direct chase
                return new float[]{x, y};
        }
    }

    private void collectCoins() {
        int tx=(int)Math.floor(x),ty=(int)Math.floor(y);
        for (int dx=-1; dx<=1; dx++) for (int dy=-1; dy<=1; dy++) {
            int cx=tx+dx, cy=ty+dy;
            if (!coins.contains(key(cx,cy))) continue;
            if (distance(x,y,cx+0.5f,cy+0.5f) <= COIN_PICKUP_RADIUS) {
                coins.remove(key(cx,cy));
                coinsCollected++;
            }
        }
    }
    private void collectPowerups() {
        int tx=(int)Math.floor(x),ty=(int)Math.floor(y);
        for(PowerState p:powerups)if(p.active&&p.x==tx&&p.y==ty){
            p.active=false;
            switch(p.type.toLowerCase()){
                case "brain" -> lives=Math.min(MAX_LIVES,lives+1);
                case "octopus" -> octopusTimer=15f;
                case "plantfood" -> zombotanyTimer=15f;
                case "potion" -> potionTimer=10f;
                case "magnifying_grass" -> {zoybeanTimer=10f;transformEffectTimer=1.1f;zoybeanFlashTimer=0.6f;}
            }
        }
    }

    public void useZ() {
        if(gameOver||won)return;
        if(octopusTimer>0 || zombotanyTimer>0) {
            float vx=0,vy=0;
            if(direction==Direction.LEFT)vx=-9;if(direction==Direction.RIGHT)vx=9;if(direction==Direction.UP)vy=9;if(direction==Direction.DOWN)vy=-9;
            if(vx==0&&vy==0)vx=9;
            projectiles.add(new Projectile(x,y,vx,vy,false,octopusTimer>0,2.5f*getDamageMultiplier()));
        }
    }

    private void hurtPlayer(){
        if(invulnerableTimer>0||zoybeanTimer>0||gameOver||won)return;
        lives--;
        if(lives<=0){lives=0;gameOver=true;return;}
        x=spawnX;y=spawnY;direction=Direction.NONE;desiredDirection=Direction.NONE;invulnerableTimer=1.8f;
    }

    private boolean hasLineOfSight(float ax,float ay,float bx,float by){
        float dx=bx-ax,dy=by-ay;
        if(Math.abs(dx)>0.65f&&Math.abs(dy)>0.65f)return false;
        int steps=(int)(distance(ax,ay,bx,by)*4)+1;
        for(int i=0;i<=steps;i++){
            float t=i/(float)steps;
            if(isWall((int)Math.floor(ax+dx*t),(int)Math.floor(ay+dy*t)))return false;
        }
        return true;
    }

    private boolean canMove(Direction d){
        float step=0.62f;
        float nx=x,ny=y;
        if(d==Direction.LEFT)nx-=step;if(d==Direction.RIGHT)nx+=step;if(d==Direction.UP)ny+=step;if(d==Direction.DOWN)ny-=step;
        return canPlayerOccupy(nx,ny) || tunnelCrossing(nx,ny);
    }
    private boolean canPlayerOccupy(float px,float py){
        if(!canOccupy(px,py)) return false;
        for(PlantState p:plants){
            if(!p.alive) continue;
            float cx=p.x+0.5f, cy=p.y+0.5f;
            if(Math.abs(px-cx)<0.48f && Math.abs(py-cy)<0.48f) return false;
        }
        return true;
    }

    private boolean canOccupy(float px,float py){
        int tx=(int)Math.floor(px),ty=(int)Math.floor(py);
        if(tx<0||tx>=map.width||ty<0||ty>=map.height)return tunnelCrossing(px,py);
        return !isWall(tx,ty);
    }
    private boolean tunnelCrossing(float px,float py){
        int tx=(int)Math.floor(px),ty=(int)Math.floor(py);
        if(tx>=0&&tx<map.width&&ty>=0&&ty<map.height)return true;
        for(TunnelDef t:map.tunnels) if(Math.abs(ty-t.y)<=1) return true;
        return false;
    }
    private void applyTunnels(){
        if(x<0){for(TunnelDef t:map.tunnels)if(Math.abs((int)Math.floor(y)-t.y)<=1){x=map.width-0.35f;return;}x=0.35f;}
        if(x>map.width){for(TunnelDef t:map.tunnels)if(Math.abs((int)Math.floor(y)-t.y)<=1){x=0.35f;return;}x=map.width-0.35f;}
    }
    private boolean isWall(int tx,int ty){return walls.contains(key(tx,ty));}
    private static long key(int x,int y){return ((long)x<<32) ^ (y&0xffffffffL);}
    private static float distance(float ax,float ay,float bx,float by){return (float)Math.sqrt((ax-bx)*(ax-bx)+(ay-by)*(ay-by));}

    public boolean isInvulnerable(){return invulnerableTimer>0||zoybeanTimer>0;}
    public float getEatAnimationTime(){return eatAnimTimer;}
    public float getZoybeanFlashTimer(){return zoybeanFlashTimer;}
    public float getInvulnerableTimer(){return invulnerableTimer;}
}
