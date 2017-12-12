package ru.spbau.mit.plansnet.dataController;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
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
import java.util.concurrent.CountDownLatch;

import ru.spbau.mit.plansnet.MainActivity;
import ru.spbau.mit.plansnet.data.AbstractDataContainer;
import ru.spbau.mit.plansnet.data.AbstractNamedData;
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
        netManager = new NetworkDataManager(context, account);

        this.adapter = adapter;
        this.listOfMaps = listOfMaps;
        userAccount = new Account(account.getDisplayName(), account.getUid());
    }

    public void downloadMaps(@NonNull final ProgressDialog progressDialog) {
        netManager.downloadMaps(progressDialog);
    }

//    public void renameGroup(@NonNull final UsersGroup usersGroup,
//                            @NonNull final String newName) {
//        netManager.renameGroup(usersGroup.getName(), newName);
//        usersGroup.setName(newName);
//        //UI update??
//    }

    public void deleteByPath(@Nullable final String groupName,
                             @Nullable final String buildingName,
                             @Nullable final String mapName) {
        AbstractDataContainer ref;
        if (groupName != null) {
            ref = userAccount;
            if (buildingName != null) {
                ref = (AbstractDataContainer) ref.findByName(groupName);
                if (ref == null) {
                    return;
                }
                if (mapName != null) {
                    ref = (AbstractDataContainer) ref.findByName(buildingName);
                    if (ref == null) {
                        return;
                    }
                    ref.getAllData().remove(mapName);
                } else {
                    ref.getAllData().remove(buildingName);
                }
            } else {
                ref.getAllData().remove(groupName);
            }
        } else {
            return;
        }
        netManager.deleteReference(groupName, buildingName, mapName);
    }

    public void deleteMap(@NonNull final FloorMap map) {
        userAccount.findByName(map.getGroupName())
                .findByName(map.getBuildingName()).getAllData().remove(map);
        netManager.deleteReference(map.getGroupName(), map.getBuildingName(), map.getName());
    }

    /**
     * unsupported operation
     */
    public void renameMap(@NonNull final FloorMap map) {
        throw new UnsupportedOperationException();
    }

    private void readMapFromFile(File mapFile) {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(mapFile))) {
            FloorMap map = (FloorMap) ois.readObject();


            UsersGroup group = userAccount.findByName(map.getGroupName());
            if (group == null) {
                group = userAccount.setElementToContainer(new UsersGroup(map.getGroupName()));
            }
            Building building = group.findByName(map.getBuildingName());
            if (building == null) {
                building = group.setElementToContainer(new Building(map.getBuildingName()));
            }

            if (building.findByName(map.getName()) == null) {
                listOfMaps.add(map);
            } else {
                for (int i = 0; i < listOfMaps.size(); i++) {
                    if (listOfMaps.get(i).getName().equals(map.getName())) {
                        listOfMaps.set(i, map);
                    }
                }
            }
            building.setElementToContainer(map);


            Log.d(DATA_TAG, "map " + map.getName() + " was read");
        } catch (Exception exception) {
            Log.d(DATA_TAG, "map can't be read");
//            Toast.makeText(context,
//                    "Can't read map from file", Toast.LENGTH_SHORT).show();
            exception.printStackTrace();
        }
    }

    public void loadLocalFiles() {
        File root = new File(context.getApplicationContext().getFilesDir(), userAccount.getID());
        if (!root.exists()) {
            Log.d(DATA_TAG, "folder for user there isn't exists");
            return;
        }

        for (File group : root.listFiles()) {
            for (File building : group.listFiles()) {
                for (File floor : building.listFiles()) {
                    readMapFromFile(floor);
                }
            }
        }
        Log.d(DATA_TAG, "local files was read");
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

    public void getSearchedGroupsAndOwners(@NonNull String name,
                                           @NonNull final List<MainActivity.SearchResult> ownersAndGroups,
                                           @NonNull final CountDownLatch latch) {
        netManager.getGroupsWhichContainsName(name, ownersAndGroups, latch);
    }

    public void addGroupByRef(@NonNull final String owner, @NonNull final String group,
                              @NonNull final ProgressDialog progressDialog) {
        netManager.downloadGroup(owner, group, progressDialog);
    }

    @NonNull
    public Account getAccount() {
        return userAccount;
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

        if (building.findByName(map.getName()) == null) {
            listOfMaps.add(map);
        }
        building.setElementToContainer(map);
        Log.d(DATA_TAG, "set new map to account");


        writeMap(map);
        Toast.makeText(context, "Map saved", Toast.LENGTH_SHORT).show();

        netManager.putMapOnServer(map);
    }
}
