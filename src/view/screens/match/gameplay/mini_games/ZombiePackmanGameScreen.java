package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.ScreenManager;
import model.App;
import model.collections.animations.AnimationFactory;
import model.match.mini_games.ZombiePackmanGame;
import model.match.mini_games.ZombiePackmanGame.Direction;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.BaseScreen;

import java.util.HashMap;
import java.util.Map;

/**
 * A self-contained 240x240 Pac-Man-style maze. The world is intentionally independent of
 * GameScreen because a large continuous board is a very different simulation from PvZ's
 * five-lane Match/GameSession model.
 */
public class ZombiePackmanGameScreen extends BaseScreen {
    private static final String PLAYER_PAM =
            "768/FULL/ZOMBIE/FOODFIGHT_ZOMBIE/FOODFIGHT_ZOMBIE.PAM";
    private static final String ZOYBEAN_PAM =
            "768/INITIAL/ZOMBIE/ZOYBEANPOD_ZOMBIE/ZOYBEANPOD_ZOMBIE.PAM";
    private static final String GHOST_PAM =
            "768/INITIAL/PLANT/GHOSTPEPPER/GHOSTPEPPER.PAM";
    private static final String BRAMBLE_PAM =
            "768/INITIAL/PLANT/BRAMBLEBUSH/BRAMBLEBUSH.PAM";
    private static final String COIN_TEXTURE = "assets/images/ui/packman/convrt_coin.png";
    private static final String LIFE_TEXTURE = "assets/images/ui/packman/timer_deco_bigbrainz.png";
    private static final String TILE_TEXTURE = "assets/images/ui/packman/card_plant_bg_dark.png";
    private static final String OCTOPUS_PROJECTILE_PAM =
            "768/FULL/EFFECTS/ZOMBIE_OCTOPUS_PROJECTILE/ZOMBIE_OCTOPUS_PROJECTILE.PAM";
    private static final String PEA_PROJECTILE_PAM =
            "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
    private static final String BRAIN_PAM =
            "768/FULL/ZOMBIE/POWER_BRAIN_PROJECTILE/POWER_BRAIN_PROJECTILE.PAM";
    private static final String PLANTFOOD_PAM =
            "768/INITIAL/EFFECTS/PLANTFOOD_PICKUP/PLANTFOOD_PICKUP.PAM";
    private static final String POTION_PAM =
            "768/FULL/MINIGAME/POTION_POWER/POTION_POWER.PAM";
    private static final String MAGNIFY_PAM =
            "768/FULL/EFFECTS/MAGNIFYING_GRASS_PLANTFOOD_HIT/MAGNIFYING_GRASS_PLANTFOOD_HIT.PAM";
    private static final String TRANSFORM_PAM =
            "768/INITIAL/EFFECTS/ZOMBIE_EGYPT_SARCOPHAGUS_BEAM_DOWN/ZOMBIE_EGYPT_SARCOPHAGUS_BEAM_DOWN.PAM";

    private static final float VIEW_W = SCREEN_WIDTH;
    private static final float VIEW_H = SCREEN_HEIGHT;
    private static final float PLAYER_SCALE = 0.52f;
    private static final float GHOST_SCALE = 0.52f;
    private static final float PLANT_SCALE = 0.50f;
    private static final float WALL_SCALE = 0.48f;

    /** Classic Pac-Man ghost colors, one per personality (Blinky/Pinky/Inky/Clyde). */
    private static final Color[] GHOST_COLORS = {
            new Color(0.92f, 0.24f, 0.24f, 1f),
            new Color(1f, 0.55f, 0.85f, 1f),
            new Color(0.35f, 0.85f, 0.98f, 1f),
            new Color(1f, 0.65f, 0.25f, 1f),
    };
    private static final Color GHOST_FRIGHT_COLOR = new Color(0.28f, 0.35f, 0.95f, 1f);
    private static final Color GHOST_RECOVER_COLOR = new Color(0.85f, 0.85f, 0.92f, 0.55f);

    private final Map<String, ClipRef> clipCache = new HashMap<>();
    private ZombiePackmanGame game;
    private boolean playerFacingRight;
    private final Map<ZombiePackmanGame.GhostState, Boolean> ghostFacingRight = new HashMap<>();
    private String playerAnimState;
    private float playerAnimTime;
    private OrthographicCamera camera;
    private TextureBank textureBank;
    private PamPlayer pamPlayer;
    private Texture coinTexture, lifeTexture, tileTexture;
    private TextureRegion whitePixel;
    private BitmapFont font;
    private InputAdapter keyboard;
    private float worldTime;
    private float deathFlash;
    private float winPulse;

    @Override public void initParticles() { }

    @Override
    public void show() {
        AudioManager.get().playMusic(AudioEnum.CONSULE_MUSIC, true);
        super.show();
        camera = new OrthographicCamera(VIEW_W, VIEW_H);
        game = new ZombiePackmanGame();
        camera.position.set(game.getX() * game.getMap().tileSize, game.getY() * game.getMap().tileSize, 0f);
        camera.update();

        font = new BitmapFont();
        font.getData().setScale(1.05f);
        coinTexture = loadTexture(COIN_TEXTURE);
        lifeTexture = loadTexture(LIFE_TEXTURE);
        tileTexture = loadTexture(TILE_TEXTURE);
        whitePixel = createWhitePixel();

        try {
            textureBank = new TextureBank("atlases", Gdx.files.internal("assets/pvz-assets"));
            pamPlayer = new PamPlayer(textureBank, Gdx.files.internal("assets/pvz-assets"));
            preload();
        } catch (Throwable t) {
            textureBank = null;
            pamPlayer = null;
            if (model.utils.GameSettings.get().isDebugMode())
                Gdx.app.error("ZOMBIE_PACKMAN_PAM", "PAM initialization failed", t);
        }

        keyboard = new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.UP) game.setDesiredDirection(Direction.UP);
                else if (keycode == Input.Keys.DOWN) game.setDesiredDirection(Direction.DOWN);
                else if (keycode == Input.Keys.LEFT) game.setDesiredDirection(Direction.LEFT);
                else if (keycode == Input.Keys.RIGHT) game.setDesiredDirection(Direction.RIGHT);
                else if (keycode == Input.Keys.SPACE) game.setEating(true);
                else if (keycode == Input.Keys.Z) game.useZ();
                else if (keycode == Input.Keys.R) restart();
                else if (keycode == Input.Keys.ESCAPE) exitToConsole();
                else return false;
                return true;
            }
            @Override public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.SPACE) { game.setEating(false); return true; }
                return false;
            }
        };
        multiplexer.addProcessor(keyboard);
    }

    private void preload() {
        for (String path : new String[]{PLAYER_PAM, ZOYBEAN_PAM, ZombotanyArt.BODY_PAM,
                "768/INITIAL/PLANT/PEASHOOTER/PEASHOOTER.PAM", GHOST_PAM, BRAMBLE_PAM,
                OCTOPUS_PROJECTILE_PAM, PEA_PROJECTILE_PAM, BRAIN_PAM, PLANTFOOD_PAM,
                POTION_PAM, MAGNIFY_PAM, TRANSFORM_PAM}) {
            try { pamPlayer.loadAsync(normalize(path), null); } catch (Throwable ignored) { }
        }
        
        for (ZombiePackmanGame.PlantState p : game.getPlants()) {
            String path = plantPath(p.type);
            if (path != null) try { pamPlayer.loadAsync(normalize(path), null); } catch (Throwable ignored) { }
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        worldTime += delta;
        if (textureBank != null) try { textureBank.update(); } catch (Throwable ignored) { }
        if (!game.isGameOver() && !game.isWon()) game.update(delta);
        if (game.isGameOver()) deathFlash += delta;
        if (game.isWon()) winPulse += delta;
        updateCamera(delta);
        drawWorld(delta);
        drawHud();
        stage.act(delta);
        stage.draw();
    }

    private void updateCamera(float delta) {
        float tile = game.getMap().tileSize;
        float halfW = VIEW_W / 2f, halfH = VIEW_H / 2f;
        float worldW = game.getMap().width * tile, worldH = game.getMap().height * tile;
        float targetX = game.getX() * tile;
        float targetY = game.getY() * tile;
        float minX = halfW, maxX = Math.max(halfW, worldW-halfW);
        float minY = halfH, maxY = Math.max(halfH, worldH-halfH);
        targetX = MathUtils.clamp(targetX, minX, maxX);
        targetY = MathUtils.clamp(targetY, minY, maxY);
        float smoothing = Math.min(1f, delta * 8f);
        camera.position.x += (targetX-camera.position.x)*smoothing;
        camera.position.y += (targetY-camera.position.y)*smoothing;
        camera.update();
    }

    private void drawWorld(float delta) {
        Gdx.gl.glClearColor(0.035f,0.035f,0.045f,1f);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        int tile = game.getMap().tileSize;
        int minX=Math.max(0,(int)((camera.position.x-VIEW_W/2)/tile)-2);
        int maxX=Math.min(game.getMap().width-1,(int)((camera.position.x+VIEW_W/2)/tile)+2);
        int minY=Math.max(0,(int)((camera.position.y-VIEW_H/2)/tile)-2);
        int maxY=Math.min(game.getMap().height-1,(int)((camera.position.y+VIEW_H/2)/tile)+2);

        drawFloor(tile,minX,maxX,minY,maxY);
        drawCoins(tile,minX,maxX,minY,maxY);
        drawPowerups(tile);
        drawPlants(tile,minX,maxX,minY,maxY);
        drawProjectiles(tile);
        drawGhosts(tile);
        drawWalls(tile,minX,maxX,minY,maxY);
        drawPlayer(tile,delta);
        if (game.getTransformEffectTimer()>0) drawPam(TRANSFORM_PAM,"beam_down",worldTime,game.getX()*tile-40,game.getY()*tile-40,0.55f,false,null);
        batch.end();
    }

    private void drawFloor(int tile,int minX,int maxX,int minY,int maxY){
        if(tileTexture==null)return;
        int block=4*tile;
        int sx=(minX/4)*4, sy=(minY/4)*4;
        for(int x=sx;x<=maxX;x+=4) for(int y=sy;y<=maxY;y+=4)
            batch.draw(tileTexture,x*tile,y*tile,block,block);
    }

    private void drawCoins(int tile,int minX,int maxX,int minY,int maxY){
        if(coinTexture==null)return;
        float base=tile*1.7f;
        for(long key:game.getCoins()){
            int x=(int)(key>>32), y=(int)key;
            if(x<minX||x>maxX||y<minY||y>maxY)continue;
            float s=0.90f+0.12f*MathUtils.sin(worldTime*6f+x*0.3f+y*0.2f);
            float size=base*s;
            batch.draw(coinTexture,x*tile+(tile-size)/2f,y*tile+(tile-size)/2f,size,size);
        }
    }

    private void drawPowerups(int tile){
        for(ZombiePackmanGame.PowerState p:game.getPowerups()) if(p.active){
            String path=switch(p.type.toLowerCase()){
                case "brain"->BRAIN_PAM; case "octopus"->OCTOPUS_PROJECTILE_PAM;
                case "plantfood"->PLANTFOOD_PAM; case "potion"->POTION_PAM; default->MAGNIFY_PAM;
            };
            String state=switch(p.type.toLowerCase()){
                case "brain"->"amimation"; case "octopus"->"animation3";
                case "plantfood"->"idle"; case "potion"->"animation2"; default->"idle";
            };
            drawPam(path,state,worldTime,p.x*tile+tile*0.5f-25,p.y*tile+tile*0.5f-25,0.48f,false,null);
        }
    }

    private void drawPlants(int tile,int minX,int maxX,int minY,int maxY){
        for(ZombiePackmanGame.PlantState p:game.getPlants()){
            if(!p.alive||p.x<minX-1||p.x>maxX+1||p.y<minY-1||p.y>maxY+1)continue;
            String path=plantPath(p.type); if(path==null)continue;
            boolean disabled=p.disableTimer>0;
            if(disabled)batch.setColor(0.65f,0.65f,0.85f,1f);
            String state="idle";
            float anim=worldTime;
            drawPam(path,state,anim,p.x*tile+tile*0.5f-32,p.y*tile+tile*0.42f-18,PLANT_SCALE,false,null);
            batch.setColor(Color.WHITE);
            if(p.eatTimer>0){
                batch.setColor(1f,0.75f,0.45f,0.75f);
                batch.draw(whitePixel,p.x*tile,p.y*tile,tile,tile);
                batch.setColor(Color.WHITE);
            }
            if(p.isWallNut()) drawHpBar(p.x*tile,p.y*tile+tile*0.12f,tile,p.hp/4.5f);
        }
    }

    private void drawProjectiles(int tile){
        for(ZombiePackmanGame.Projectile p:game.getProjectiles()){
            String path=p.octopus?OCTOPUS_PROJECTILE_PAM:PEA_PROJECTILE_PAM;
            String state=p.octopus?"animation3":"animation";
            boolean flip=p.vx<0;
            drawPam(path,state,worldTime,p.x*tile-18,p.y*tile-18,0.38f,flip,null);
        }
    }

    private void drawGhosts(int tile){
        boolean frightened = game.getZoybeanTimer()>0;
        boolean flashWarning = frightened && game.getZoybeanTimer()<2.5f;
        for(ZombiePackmanGame.GhostState g:game.getGhosts()){
            boolean recovering = g.isRecovering();
            String state;
            if(g.chasing){
                state="attack_loop";
            } else {
                state=((worldTime*0.6f + g.personality) % 2f) < 1.6f ? "idle" : "idle2";
            }
            float clipDur=AnimationFactory.clipDurationForPath(normalize(GHOST_PAM),state);
            float t=clipDur>0?(worldTime%clipDur):worldTime;
            if(recovering){
                batch.setColor(GHOST_RECOVER_COLOR);
            } else if(frightened){
                if(flashWarning && (worldTime % 0.3f) < 0.15f) batch.setColor(Color.WHITE);
                else batch.setColor(GHOST_FRIGHT_COLOR);
            } else {
                batch.setColor(GHOST_COLORS[g.personality]);
            }
            if(g.direction==Direction.RIGHT)ghostFacingRight.put(g,true);
            else if(g.direction==Direction.LEFT)ghostFacingRight.put(g,false);
            boolean gFlip=ghostFacingRight.getOrDefault(g,false);
            drawPam(GHOST_PAM,state,t,g.x*tile-25,g.y*tile-22,GHOST_SCALE,gFlip,null);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawWalls(int tile,int minX,int maxX,int minY,int maxY){
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++){
            boolean wall=false;
            for(ZombiePackmanGame.RectDef r:game.getMap().walls) if(r.contains(x,y)){wall=true;break;}
            if(!wall)continue;
            Map<String,Boolean> vis=new HashMap<>();
            vis.put("eye_white2",false);vis.put("eye_white1",false);vis.put("eye_ball2",false);vis.put("eye_ball1",false);
            drawPam(BRAMBLE_PAM,"idle",worldTime,x*tile+tile*0.5f-30,y*tile+tile*0.38f-18,WALL_SCALE,false,vis);
        }
    }

    private void drawPlayer(int tile,float delta){
        if(game.isGameOver()){
            float dur=AnimationFactory.clipDurationForPath(normalize(PLAYER_PAM),"die");
            float t=dur>0?Math.min(deathFlash,dur-0.03f):deathFlash;
            drawPam(PLAYER_PAM,"die",t,game.getX()*tile-28,game.getY()*tile-20,PLAYER_SCALE,false,null);
            return;
        }
        boolean zoy=game.getZoybeanTimer()>0;
        boolean zombotany=game.getZombotanyTimer()>0 && !zoy;
        String state;
        if(game.isEating())state="eat"; else state=game.getDirection()==Direction.NONE?"idle":"walk";
        if(game.getDirection()==Direction.RIGHT)playerFacingRight=true;
        else if(game.getDirection()==Direction.LEFT)playerFacingRight=false;
        boolean flip=zoy?!playerFacingRight:playerFacingRight;
        if(game.getPotionTimer()>0 && !zoy && !zombotany)batch.setColor(1f,0.55f,0.82f,1f);
        if(game.isInvulnerable() && !zoy)batch.setColor(0.92f,1f,0.95f,1f);

        String bodyPam=zombotany?ZombotanyArt.BODY_PAM:(zoy?ZOYBEAN_PAM:PLAYER_PAM);
        if(!state.equals(playerAnimState)){playerAnimState=state;playerAnimTime=0f;}
        else playerAnimTime+=delta;
        float clipDur=AnimationFactory.clipDurationForPath(normalize(bodyPam),state);
        float animTime=clipDur>0?(playerAnimTime%clipDur):playerAnimTime;

        if(zombotany){
            drawPam(ZombotanyArt.BODY_PAM,state,animTime,game.getX()*tile-28,game.getY()*tile-20,
                    PLAYER_SCALE,flip,ZombotanyArt.headlessBodyMask());
            drawPam("768/INITIAL/PLANT/PEASHOOTER/PEASHOOTER.PAM","idle",worldTime,
                    game.getX()*tile+6,game.getY()*tile+28,0.33f,flip,null);
        } else {
            drawPam(bodyPam,state,animTime,game.getX()*tile-28,
                    game.getY()*tile-20,PLAYER_SCALE,flip,null);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawHud(){
        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        batch.begin();
        if(lifeTexture!=null){
            for(int i=0;i<game.getLives();i++) batch.draw(lifeTexture,25+i*42,SCREEN_HEIGHT-62,34,34);
        }
        font.draw(batch,"LIVES",25,SCREEN_HEIGHT-12);
        font.draw(batch,"COINS  "+game.getCoinsCollected()+" / "+game.getTotalCoins(),25,SCREEN_HEIGHT-82);
        String power="";
        if(game.getOctopusTimer()>0)power="OCTOPUS "+Math.ceil(game.getOctopusTimer())+"s";
        else if(game.getZombotanyTimer()>0)power="ZOMBOTANY "+Math.ceil(game.getZombotanyTimer())+"s";
        else if(game.getPotionTimer()>0)power="POWER "+Math.ceil(game.getPotionTimer())+"s";
        else if(game.getZoybeanTimer()>0)power="ZOYBEAN "+Math.ceil(game.getZoybeanTimer())+"s";
        if(!power.isEmpty()) font.draw(batch,power,SCREEN_WIDTH-245,SCREEN_HEIGHT-20);
        font.draw(batch,"ARROWS MOVE   SPACE EAT   Z USE POWER   R RESTART   ESC CONSOLE",
                25,24);
        if(game.isGameOver()){
            font.getData().setScale(2.2f);
            GlyphLayout l=new GlyphLayout(font,"GAME OVER");
            font.draw(batch,l,SCREEN_WIDTH/2f-l.width/2f,SCREEN_HEIGHT/2f+25);
            font.getData().setScale(1.05f);
            font.draw(batch,"Press R to restart or ESC to return",SCREEN_WIDTH/2f-170,SCREEN_HEIGHT/2f-20);
        } else if(game.isWon()){
            font.getData().setScale(2.0f);
            GlyphLayout l=new GlyphLayout(font,"MAZE CLEARED!");
            font.draw(batch,l,SCREEN_WIDTH/2f-l.width/2f,SCREEN_HEIGHT/2f+25);
            font.getData().setScale(1.05f);
            font.draw(batch,"Every coin collected — press R to play again",SCREEN_WIDTH/2f-215,SCREEN_HEIGHT/2f-20);
        }
        batch.end();
    }

    private void drawHpBar(float x,float y,float w,float ratio){
        batch.setColor(0,0,0,0.75f);batch.draw(whitePixel,x,y,w,5);
        batch.setColor(0.85f,0.95f,0.35f,1);batch.draw(whitePixel,x,y,w*MathUtils.clamp(ratio,0,1),5);
        batch.setColor(Color.WHITE);
    }

    private boolean drawPam(String path,String state,float time,float x,float y,float scale,boolean flip,Map<String,Boolean> visibility){
        if(pamPlayer==null||path==null)return false;
        String p=normalize(path);
        try{
            String clipName=AnimationFactory.resolveClipNameForPath(p,state);
            if(clipName==null)clipName=state;
            ClipRef clip=clipCache.get(p+"#"+clipName);
            if(clip==null){clip=pamPlayer.getClip(p,clipName);if(clip!=null)clipCache.put(p+"#"+clipName,clip);}
            
            
            
            if(clip==null && "amimation".equalsIgnoreCase(state)){
                clip=pamPlayer.getClip(p,"animation");
                if(clip!=null)clipCache.put(p+"#animation",clip);
            }
            if(clip==null)return false;

            batch.flush();
            com.badlogic.gdx.math.Matrix4 old=batch.getTransformMatrix().cpy();
            float scaleX = flip ? -scale : scale;
            batch.getTransformMatrix().translate(x,y,0).scale(scaleX,scale,1);
            batch.setTransformMatrix(batch.getTransformMatrix());
            if(visibility==null)pamPlayer.draw(batch,clip,time,0,0,false);
            else pamPlayer.draw(batch,clip,time,0,0,false,visibility);
            batch.flush();
            batch.setTransformMatrix(old);
            return true;
        }catch(Throwable ignored){return false;}
    }

    private String plantPath(String type){
        if(type==null)return null;
        return AnimationFactory.pathForDisplayName(type);
    }

    private String normalize(String path){
        return path.startsWith("assets/pvz-assets/")?path.substring("assets/pvz-assets/".length()):path;
    }

    private Texture loadTexture(String path){
        if(path!=null&&Gdx.files.internal(path).exists()){
            Texture t=new Texture(Gdx.files.internal(path));t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);return t;
        }
        return null;
    }
    private TextureRegion createWhitePixel(){
        Pixmap p=new Pixmap(1,1,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);p.fill();Texture t=new Texture(p);p.dispose();return new TextureRegion(t);
    }

    private void restart(){ game=new ZombiePackmanGame(); clipCache.clear(); ghostFacingRight.clear(); playerFacingRight=false; playerAnimState=null; playerAnimTime=0f; preload(); deathFlash=0; winPulse=0; }
    private void exitToConsole(){ App.currentMenu=new controller.ui_menus.ConsoleMenu(); ScreenManager.forceResync(); }

    @Override public void dispose(){
        if(keyboard!=null)multiplexer.removeProcessor(keyboard);
        if(font!=null)font.dispose();
        if(coinTexture!=null)coinTexture.dispose(); if(lifeTexture!=null)lifeTexture.dispose(); if(tileTexture!=null)tileTexture.dispose();
        if(whitePixel!=null)whitePixel.getTexture().dispose();
        if(textureBank!=null)try{textureBank.dispose();}catch(Throwable ignored){}
        super.dispose();
    }
}
