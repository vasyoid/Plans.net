package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ru.spbau.mit.plansnet.constructor.DoorSprite;
import ru.spbau.mit.plansnet.constructor.MapObjectSprite;
import ru.spbau.mit.plansnet.constructor.WallSprite;
import ru.spbau.mit.plansnet.data.objects.Door;
import ru.spbau.mit.plansnet.data.objects.MapObject;
import ru.spbau.mit.plansnet.data.objects.Wall;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class FloorMap extends AbstractNamedData implements Serializable {
    @NonNull
    private ArrayList<MapObject> objects;
    @NonNull
    private String buildingName;
    @NonNull
    private String groupName;

    public FloorMap(@NonNull final String name, @NonNull final String groupName,
                    @NonNull final String buildingName,
                    @NonNull final ru.spbau.mit.plansnet.constructor.Map map) {
        List<MapObjectSprite> objList = map.getObjects();
//      List<RoomSprite> roomList map.getRooms();

        for (MapObjectSprite obj : objList) {
            if (obj instanceof WallSprite) {
                objects.add(new Wall((WallSprite) obj));
            } else if (obj instanceof DoorSprite) {
                objects.add(new Door((DoorSprite) obj));
            }
        }
//        for (ru.spbau.mit.plansnet.constructor.RoomSprite room : roomList) {
//              objects.add(new RoomSprite(room));
//        }
    }

    public FloorMap() {
        buildingName = "default";
        groupName = "default";
        objects = new ArrayList<>();
    }

    public FloorMap(@NonNull final String name, @NonNull final String groupName,
                    @NonNull final String buildingName) {
        super(name);
        objects = new ArrayList<>();

        this.groupName = groupName;
        this.buildingName = buildingName;
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
