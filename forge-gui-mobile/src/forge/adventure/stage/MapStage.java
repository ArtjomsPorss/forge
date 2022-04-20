package forge.adventure.stage;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.SerializationException;
import forge.Forge;
import forge.adventure.character.*;
import forge.adventure.data.*;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.DuelScene;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.screens.TransitionScreen;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stage to handle tiled maps for points of interests
 */
public class MapStage extends GameStage {

    public static MapStage instance;
    Array<MapActor> actors = new Array<>();

    TiledMap map;
    ArrayList<Rectangle>[][] collision;
    private float tileHeight;
    private float tileWidth;
    private float width;
    private float height;
    private boolean isInMap = false;
    MapLayer spriteLayer;
    private PointOfInterestChanges changes;
    private EnemySprite currentMob;
    private final Vector2 oldPosition = new Vector2();//todo
    private final Vector2 oldPosition2 = new Vector2();
    private final Vector2 oldPosition3 = new Vector2();
    private final Vector2 oldPosition4 = new Vector2();
    private boolean isLoadingMatch = false;

    private Dialog dialog;
    private Stage dialogStage;
    private boolean dialogOnlyInput;

    private EffectData effect;

    public boolean getDialogOnlyInput() {
        return dialogOnlyInput;
    }
    public Dialog getDialog() {
        return dialog;
    }

    public void clearIsInMap() {
        isInMap = false;
        effect = null;
        GameHUD.getInstance().showHideMap(true);
    }
    public void draw (Batch batch) {
        //Camera camera = getCamera() ;
        //camera.update();
        //update camera after all layers got drawn

        if (!getRoot().isVisible()) return;
        getRoot().draw(batch, 1);
    }

    public MapLayer getSpriteLayer() {
        return spriteLayer;
    }

    public PointOfInterestChanges getChanges() {
        return changes;
    }

    public static MapStage getInstance() {
        return instance == null ? instance = new MapStage() : instance;
    }
    public void resLoaded()
    {
        dialog = Controls.newDialog("");
    }

    public void addMapActor(MapObject obj, MapActor newActor) {
        newActor.setWidth(Float.parseFloat(obj.getProperties().get("width").toString()));
        newActor.setHeight(Float.parseFloat(obj.getProperties().get("height").toString()));
        newActor.setX(Float.parseFloat(obj.getProperties().get("x").toString()));
        newActor.setY(Float.parseFloat(obj.getProperties().get("y").toString()));
        actors.add(newActor);
        foregroundSprites.addActor(newActor);
    }

    public void addMapActor(MapActor newActor) {
        actors.add(newActor);
        foregroundSprites.addActor(newActor);
    }

    @Override
    public boolean isColliding(Rectangle adjustedBoundingRect) {
        for (Rectangle collision : currentCollidingRectangles) {
            if (collision.overlaps(adjustedBoundingRect)) {
                return true;
            }
        }
        return false;

    }

    final ArrayList<Rectangle> currentCollidingRectangles = new ArrayList<>();

    @Override
    public void prepareCollision(Vector2 pos, Vector2 direction, Rectangle boundingRect) {
        currentCollidingRectangles.clear();
        int x1 = (int) (Math.min(boundingRect.x, boundingRect.x + direction.x) / tileWidth);
        int y1 = (int) (Math.min(boundingRect.y, boundingRect.y + direction.y) / tileHeight);
        int x2 = (int) (Math.min(boundingRect.x + boundingRect.width, boundingRect.x + boundingRect.width + direction.x) / tileWidth);
        int y2 = (int) (Math.min(boundingRect.y + boundingRect.height, boundingRect.y + boundingRect.height + direction.y) / tileHeight);

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    continue;
                }
                currentCollidingRectangles.addAll(collision[x][y]);
            }
        }
    }


    Group collisionGroup;

    @Override
    protected void debugCollision(boolean b) {

        if (collisionGroup == null) {
            collisionGroup = new Group();

            for (int x = 0; x < collision.length; x++) {
                for (int y = 0; y < collision[x].length; y++) {
                    for (Rectangle rectangle : collision[x][y]) {
                        MapActor collisionActor = new MapActor(0);
                        collisionActor.setBoundDebug(true);
                        collisionActor.setWidth(rectangle.width);
                        collisionActor.setHeight(rectangle.height);
                        collisionActor.setX(rectangle.x);
                        collisionActor.setY(rectangle.y);
                        collisionGroup.addActor(collisionActor);
                    }
                }
            }

        }
        if (b) {
            addActor(collisionGroup);
        } else {
            collisionGroup.remove();
        }

    }

    private void effectDialog(EffectData E){
        dialog.getContentTable().clear();
        dialog.getButtonTable().clear();
        String text = "Strange magical energies flow within this place...\nAll opponents get:\n";
        text += E.getDescription();
        dialog.text(text);
        dialog.getButtonTable().add(Controls.newTextButton("OK", this::hideDialog));
        dialog.setKeepWithinStage(true);
        showDialog();
    }

    public void loadMap(TiledMap map, String sourceMap) {
        isLoadingMatch = false;
        isInMap = true;
        GameHUD.getInstance().showHideMap(false);
        this.map = map;
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            actor.remove();
            foregroundSprites.removeActor(actor);

        }
        actors = new Array<>();
        width = Float.parseFloat(map.getProperties().get("width").toString());
        height = Float.parseFloat(map.getProperties().get("height").toString());
        tileHeight = Float.parseFloat(map.getProperties().get("tileheight").toString());
        tileWidth = Float.parseFloat(map.getProperties().get("tilewidth").toString());
        setBounds(width * tileWidth, height * tileHeight);
        collision = new ArrayList[(int) width][(int) height];

        //Load dungeon effects.
        if( map.getProperties().get("dungeonEffect") != null && !map.getProperties().get("dungeonEffect").toString().isEmpty()){
            Json json = new Json();
            try { effect = json.fromJson(EffectData.class, map.getProperties().get("dungeonEffect").toString()); }
            catch(SerializationException E) {
                //JSON parsing could fail. Since this an user written part, assume failure is possible (it happens).
                System.err.printf("[%s] while loading JSON file for dialog actor. JSON:\n%s\nUsing a default dialog.", E.getMessage(), map.getProperties().get("dungeonEffect").toString());
                effect = json.fromJson(EffectData.class, "");
            }
            effectDialog(effect);
        }

        GetPlayer().stop();

        for (MapLayer layer : map.getLayers()) {
            if (layer.getProperties().containsKey("spriteLayer") && layer.getProperties().get("spriteLayer", boolean.class)) {
                spriteLayer = layer;
            }
            if (layer instanceof TiledMapTileLayer) {
                loadCollision((TiledMapTileLayer) layer);
            } else {
                loadObjects(layer, sourceMap);
            }
        }

    }

    private void loadCollision(TiledMapTileLayer layer) {
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                if (collision[x][y] == null)
                    collision[x][y] = new ArrayList<>();
                ArrayList<Rectangle> map = collision[x][y];
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null)
                    continue;
                for (MapObject collision : cell.getTile().getObjects()) {
                    if (collision instanceof RectangleMapObject) {
                        Rectangle r = ((RectangleMapObject) collision).getRectangle();
                        map.add(new Rectangle((Math.round(layer.getTileWidth() * x) + r.x), (Math.round(layer.getTileHeight() * y) + r.y), Math.round(r.width), Math.round(r.height)));
                    }
                }
            }
        }
    }

    private void loadObjects(MapLayer layer, String sourceMap) {
        player.setMoveModifier(2);
        for (MapObject obj : layer.getObjects()) {

            MapProperties prop = obj.getProperties();
            Object typeObject = prop.get("type");
            if (typeObject != null) {
                String type = prop.get("type", String.class);
                int id = prop.get("id", int.class);
                if (changes.isObjectDeleted(id))
                    continue;
                switch (type) {
                    case "entry":
                        float x = Float.parseFloat(prop.get("x").toString());
                        float y = Float.parseFloat(prop.get("y").toString());
                        float w = Float.parseFloat(prop.get("width").toString());
                        float h = Float.parseFloat(prop.get("height").toString());
                        EntryActor entry = new EntryActor(this, sourceMap, id, prop.get("teleport").toString(), x, y, w, h, prop.get("direction").toString());
                        addMapActor(obj, entry);
                        break;
                    case "reward":
                        if (prop.get("reward") != null) {
                            RewardSprite R = new RewardSprite(id, prop.get("reward").toString(), prop.get("sprite").toString());
                            addMapActor(obj, R);
                        }
                        break;
                    case "enemy":
                        EnemySprite mob = new EnemySprite(id, WorldData.getEnemy(prop.get("enemy").toString()));
                        addMapActor(obj, mob);
                        if(prop.get("dialog") != null && !prop.get("dialog").toString().isEmpty()) {
                            mob.dialog = new MapDialog(prop.get("dialog").toString(), this, mob.getId());
                        }
                        break;
                    case "dummy": //Does nothing. Mostly obstacles to be removed by ID by switches or such.
                        TiledMapTileMapObject obj2 = (TiledMapTileMapObject) obj;
                        DummySprite D = new DummySprite(id, obj2.getTextureRegion(), this);
                        addMapActor(obj, D);
                        break;
                    case "inn":
                        addMapActor(obj, new OnCollide(new Runnable() {
                            @Override
                            public void run() {
                                Forge.switchScene(SceneType.InnScene.instance);
                            }
                        }));
                        break;
                    case "exit":
                        addMapActor(obj, new OnCollide(new Runnable() {
                            @Override
                            public void run() {
                                MapStage.this.exit();
                            }
                        }));
                        break;
                    case "dialog":
                        if(obj instanceof TiledMapTileMapObject) {
                            TiledMapTileMapObject tiledObj = (TiledMapTileMapObject) obj;
                            DialogActor dialog = new DialogActor(this, id, prop.get("dialog").toString(), tiledObj.getTextureRegion());
                            addMapActor(obj, dialog);
                        }
                        break;
                    case "shop":
                        String shopList = prop.get("shopList").toString();
                        List possibleShops = Arrays.asList(shopList.split(","));
                        Array<ShopData> shops;
                        if (possibleShops.size() == 0 || shopList.equals(""))
                            shops = WorldData.getShopList();
                        else {
                            shops = new Array<>();
                            for (ShopData data : new Array.ArrayIterator<>(WorldData.getShopList())) {
                                if (possibleShops.contains(data.name)) {
                                    shops.add(data);
                                }
                            }
                        }
                        if (shops.size == 0)
                            continue;

                        ShopData data = shops.get(WorldSave.getCurrentSave().getWorld().getRandom().nextInt(shops.size));
                        Array<Reward> ret = new Array<>();
                        for (RewardData rdata : new Array.ArrayIterator<>(data.rewards)) {
                            ret.addAll(rdata.generate(false));
                        }
                        ShopActor actor = new ShopActor(this, id, ret, data.unlimited);
                        addMapActor(obj, actor);
                        if (prop.containsKey("signYOffset") && prop.containsKey("signXOffset")) {
                            try {
                                TextureSprite sprite = new TextureSprite(Config.instance().getAtlas(data.spriteAtlas).createSprite(data.sprite));
                                sprite.setX(actor.getX() + Float.parseFloat(prop.get("signXOffset").toString()));
                                sprite.setY(actor.getY() + Float.parseFloat(prop.get("signYOffset").toString()));
                                addMapActor(sprite);
                            } catch (Exception e) {
                                System.err.print("Can not create Texture for " + data.sprite + " Obj:" + data);
                            }
                        }
                        break;
                }
            }
        }
    }

    public boolean exit() {
        isLoadingMatch = false;
        effect = null; //Reset dungeon effects.
        clearIsInMap();
        Forge.switchScene(SceneType.GameScene.instance);
        return true;
    }


    @Override
    public void setWinner(boolean playerWins) {
        isLoadingMatch = false;
        if (playerWins) {
            player.setAnimation(CharacterSprite.AnimationTypes.Attack);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Death);
            startPause(0.3f, new Runnable() {
                @Override
                public void run() {
                    MapStage.this.getReward();
                }
            });
        } else {
            player.setAnimation(CharacterSprite.AnimationTypes.Hit);
            currentMob.setAnimation(CharacterSprite.AnimationTypes.Attack);
            startPause(0.5f, new Runnable() {
                @Override
                public void run() {
                    player.setAnimation(CharacterSprite.AnimationTypes.Idle);
                    currentMob.setAnimation(CharacterSprite.AnimationTypes.Idle);
                    player.setPosition(oldPosition4);
                    Current.player().defeated();
                    MapStage.this.stop();
                    currentMob = null;
                }
            });
        }

    }

    public boolean deleteObject(int id) {
        changes.deleteObject(id);
        for (int i=0;i< actors.size;i++) {
            if (actors.get(i).getObjectId() == id && id > 0) {
                actors.get(i).remove();
                actors.removeIndex(i);
                return true;
            }
        }
        return false;
    }

    public boolean lookForID(int id){
        for(MapActor A : new Array.ArrayIterator<>(actors)){
            if(A.getId() == id)
                return true;
        }
        return false;
    }

    public EnemySprite getEnemyByID(int id) {
        for(MapActor A : new Array.ArrayIterator<>(actors)){
            if(A instanceof EnemySprite && A.getId() == id)
                return ((EnemySprite) A);
        }
        return null;
    }

    protected void getReward() {
        isLoadingMatch = false;
        ((RewardScene) SceneType.RewardScene.instance).loadRewards(currentMob.getRewards(), RewardScene.Type.Loot, null);
        currentMob.remove();
        actors.removeValue(currentMob, true);
        changes.deleteObject(currentMob.getId());
        currentMob = null;
        Forge.switchScene(SceneType.RewardScene.instance);
    }
    public void removeAllEnemies()
    {
        List<Integer> idsToRemove=new ArrayList<>();
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
                if (actor instanceof EnemySprite) {
                    idsToRemove.add(actor.getObjectId());
                }
        }
        for(Integer i:idsToRemove)
            deleteObject(i);
    }

    @Override
    protected void onActing(float delta) {
        oldPosition4.set(oldPosition3);
        oldPosition3.set(oldPosition2);
        oldPosition2.set(oldPosition);
        oldPosition.set(player.pos());
        for (MapActor actor : new Array.ArrayIterator<>(actors)) {
            if (actor.collideWithPlayer(player)) {
                if (actor instanceof EnemySprite) {
                    EnemySprite mob = (EnemySprite) actor;
                    currentMob = mob;
                    if (mob.dialog != null){ //This enemy has something to say. Display a dialog like if it was a DialogActor.
                        resetPosition();
                        showDialog();
                        mob.dialog.activate();
                    } else { //Duel the enemy.
                        beginDuel(mob);
                    }
                    break;
                } else if (actor instanceof RewardSprite) {
                    Gdx.input.vibrate(50);
                    startPause(0.1f, new Runnable() {
                        @Override
                        public void run() { //Switch to item pickup scene.
                            RewardSprite RS = (RewardSprite) actor;
                            ((RewardScene) SceneType.RewardScene.instance).loadRewards(RS.getRewards(), RewardScene.Type.Loot, null);
                            RS.remove();
                            actors.removeValue(RS, true);
                            changes.deleteObject(RS.getId());
                            Forge.switchScene(SceneType.RewardScene.instance);
                        }
                    });
                    break;
                }
            }
        }
    }

    public void beginDuel(EnemySprite mob){
        if(mob == null) return;
        currentMob = mob;
        player.setAnimation(CharacterSprite.AnimationTypes.Attack);
        mob.setAnimation(CharacterSprite.AnimationTypes.Attack);
        Gdx.input.vibrate(50);
        Forge.setCursor(null, Forge.magnifyToggle ? "1" : "2");
        SoundSystem.instance.play(SoundEffectType.ManaBurn, false);
        if (!isLoadingMatch) {
            isLoadingMatch = true;
            Forge.setTransitionScreen(new TransitionScreen(new Runnable() {
                @Override
                public void run() {
                    Forge.clearTransitionScreen();
                }
            }, ScreenUtils.getFrameBufferTexture(), true, false));
        }
        startPause(0.4f, new Runnable() {
            @Override
            public void run() {
                DuelScene S = ((DuelScene) SceneType.DuelScene.instance);
                S.setEnemy(mob);
                S.setPlayer(player);
                if(isInMap && effect != null) S.setDungeonEffect(effect);
                Forge.switchScene(SceneType.DuelScene.instance);
            }
        });
    }

    public void setPointOfInterest(PointOfInterestChanges change) {
        changes = change;
    }

    public boolean isInMap() {
        return isInMap;
    }

    public void showDialog() {
        dialog.show(dialogStage);
        dialogOnlyInput=true;
    }
    public void hideDialog() {
        dialog.hide();
        dialogOnlyInput=false;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage=dialogStage;
    }

    public void resetPosition() {

        player.setPosition(oldPosition4);
        stop();
    }
}