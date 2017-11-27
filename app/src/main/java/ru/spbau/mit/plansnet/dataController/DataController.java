package ru.spbau.mit.plansnet.dataController;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import ru.spbau.mit.plansnet.data.Account;
import ru.spbau.mit.plansnet.data.Building;
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.UsersGroup;


/**
 * Controller class which manage data and network connection
 */

public class DataController {
    @NonNull
    private final ArrayAdapter adapter;
    @NonNull
    private final List<FloorMap> listOfMaps;
    @NonNull
    private NetworkDataManager netManager;
    @NonNull
    private Account userAccount;
    @NonNull
    private Context context;

    private static final String DATA_TAG = "DATA_CONTROLLER_FILES";

    public DataController(@NonNull final Context context, @NonNull final FirebaseUser account,
                          @NonNull final ArrayAdapter adapter, @NonNull final List<FloorMap> listOfMaps) {
        this.context = context;
        netManager = new NetworkDataManager(this, context, account, adapter, listOfMaps);
        this.adapter = adapter;
        this.listOfMaps = listOfMaps;
        userAccount = new Account(account.getDisplayName(), account.getUid());
        netManager.initAccount();
        loadLocalFiles();
    }

    void readMapFromFile(File mapFile) {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(mapFile))) {
            FloorMap map = (FloorMap) ois.readObject();


            UsersGroup group = userAccount.setElementToContainer(new UsersGroup(map.getGroupName()));
            Building building = group.setElementToContainer(new Building(map.getBuildingName()));

            if (building.findByName(map.getName()) == null) {
                building.addData(map);
                listOfMaps.add(map);
                adapter.notifyDataSetChanged();
            }


            Log.d(DATA_TAG, "map " + map.getName() + " was readed");
        } catch (Exception exception) {
            Toast.makeText(context,
                    "Can't read map from file", Toast.LENGTH_SHORT).show();
            exception.printStackTrace();
        }
    }

    private void loadLocalFiles() {
        File root = new File(context.getApplicationContext().getFilesDir(), userAccount.getID());
        if (!root.exists()) {
            return;
        }

        for (File group : root.listFiles()) {
            for (File building : group.listFiles()) {
                for (File floor : building.listFiles()) {
                    readMapFromFile(floor);
                }
            }
        }
    }


    private void writeMap(@NonNull final FloorMap map) {
        File accountFile = new File(context.getApplicationContext().getFilesDir(),
                userAccount.getID() + File.pathSeparator
                        + map.getGroupName() + File.pathSeparator
                        + map.getGroupName() + File.pathSeparator
                        + map.getName() + ".plannet");
        accountFile.getParentFile().mkdirs();

        try (ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(accountFile))) {
            ous.writeObject(map);
        } catch (IOException e) {
            Toast.makeText(context, "Can't save a map to the phone", Toast.LENGTH_SHORT).show();
            Log.d(DATA_TAG, "Can't write a map to the phone");
            e.printStackTrace();
        }
    }

    @NonNull
    public UsersGroup addGroup(@NonNull final UsersGroup group) {
        return userAccount.setElementToContainer(group);
    }

    @Nullable
    public UsersGroup getGroup(@NonNull final String groupName) {
        return userAccount.findByName(groupName);
    }

    @Nullable
    public FloorMap getMapFromBuilding(@NonNull final Building building,
                                       @NonNull final String mapName) {
        return building.findByName(mapName);
    }

    @NonNull
    public Building addBuildingToGroup(@NonNull final Building building,
                                       @NonNull final UsersGroup group) {
        return group.setElementToContainer(building);
    }

    @Nullable
    public Building getBuildingFromGroup(@NonNull final String buildingName,
                                         @NonNull final UsersGroup group) {
        return group.findByName(buildingName);
    }

    public void addMap(@NonNull final FloorMap map) {

    }

    /**
     * Save map to account, to file and send it to netWork
     */
    public void saveMap(@NonNull final FloorMap map)
            throws IllegalArgumentException {


        UsersGroup userGroup = userAccount.findByName(map.getGroupName());
        if (userGroup == null) {
            throw new IllegalArgumentException("This user haven't group: " + map.getGroupName());
        }

        Building building = userGroup.findByName(map.getBuildingName());
        if (building == null) {
            throw new IllegalArgumentException("User's group '" + map.getGroupName()
                    + "' haven't building: " + map.getBuildingName());
        }

        building.setElementToContainer(map);

//        listOfMaps.add(map);
//        adapter.notifyDataSetChanged();

        writeMap(map);

        netManager.putMapOnServer(map);
    }


}
