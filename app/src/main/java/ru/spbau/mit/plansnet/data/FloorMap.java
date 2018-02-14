package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ru.spbau.mit.plansnet.constructor.DoorSprite;
import ru.spbau.mit.plansnet.constructor.MapObjectSprite;
import ru.spbau.mit.plansnet.constructor.RoomSprite;
import ru.spbau.mit.plansnet.constructor.StickerSprite;
import ru.spbau.mit.plansnet.constructor.WallSprite;
import ru.spbau.mit.plansnet.constructor.WindowSprite;
import ru.spbau.mit.plansnet.data.objects.Door;
import ru.spbau.mit.plansnet.data.objects.MapObject;
import ru.spbau.mit.plansnet.data.objects.Room;
import ru.spbau.mit.plansnet.data.objects.Sticker;
import ru.spbau.mit.plansnet.data.objects.Wall;
import ru.spbau.mit.plansnet.data.objects.Window;

/**
 * Class for users map
 */

public class FloorMap extends AbstractNamedData implements Serializable {
    @NonNull
    private ArrayList<MapObject> objects = new ArrayList<>();
    @NonNull
    private String buildingName;
    @NonNull
    private String groupName;
    @NonNull //isUnique for each user
    private String owner;

    public FloorMap(@NonNull final String owner, @NonNull final String groupName,
                    @NonNull final String buildingName, @NonNull final String name) {
        super(name);

        this.owner = owner;
        this.groupName = groupName;
        this.buildingName = buildingName;
    }

    public FloorMap(FloorMap map) {
        owner = map.owner;
        groupName = map.groupName;
        buildingName = map.buildingName;
        setName(map.getName());

        objects = map.objects;
    }

    public FloorMap addObjectsFromMap(@NonNull final ru.spbau.mit.plansnet.constructor.Map map) {
        List<MapObjectSprite> objList = map.getObjects();
        List<RoomSprite> roomList = map.getRooms();

        FloorMap newMap = new FloorMap(owner, groupName, buildingName, getName());
        List<MapObject> objects = newMap.objects;
        objects.clear();

        for (MapObjectSprite obj : objList) {
            if (obj instanceof WallSprite) {
                objects.add(new Wall((WallSprite) obj));
            } else if (obj instanceof DoorSprite) {
                objects.add(new Door((DoorSprite) obj));
            } else if (obj instanceof WindowSprite) {
                objects.add(new Window((WindowSprite) obj));
            } else if (obj instanceof StickerSprite) {
                objects.add(new Sticker((StickerSprite) obj));
            }
        }
        for (ru.spbau.mit.plansnet.constructor.RoomSprite room : roomList) {
            objects.add(new Room(room));
        }
        return newMap;
    }

    public void copyMap(@Nullable FloorMap map) {
        if (map != null) {
            objects = map.objects;
        }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(@NonNull String owner) {
        this.owner = owner;
    }

    public void setPath(@NonNull String groupName, @NonNull String buildingName) {
        this.groupName = groupName;
        this.buildingName = buildingName;
    }

    @NonNull
    public String getBuildingName() {
        return buildingName;
    }

    @NonNull
    public String getGroupName() {
        return groupName;
    }

    @NonNull
    public List<MapObject> getArrayData() {
        return objects;
    }

    public MapObject addData(MapObject element) {
        objects.add(element);
        return objects.get(objects.size() - 1);
    }
}
