package ru.spbau.mit.plansnet.dataController;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ru.spbau.mit.plansnet.MainActivity;
import ru.spbau.mit.plansnet.data.Account;
import ru.spbau.mit.plansnet.data.Building;
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.UsersGroup;


/**
 * Controller class which manage data and network connection
 */

public class DataController {
    @NonNull
    private NetworkDataManager netManager;
    @NonNull
    private Account userAccount;
    @NonNull
    private Context context;

    private static final String DATA_TAG = "DATA_CONTROLLER_FILES";

    public DataController(@NonNull final Context context,
                          @NonNull final FirebaseUser account) {
        this.context = context;
        netManager = new NetworkDataManager(context, account);

        userAccount = new Account(account.getDisplayName(), account.getUid());
    }

    /**
     * Download maps from server
     */
    public void downloadMaps(@NonNull final List<String> floorsPaths, @NonNull AtomicInteger mapCount) {
        netManager.downloadByPaths(floorsPaths, mapCount);
    }

    /**
     * Delete first non null element of hierarchy from local memory and server
     * @param group always non null name of group
     * @param building name of building, can be null
     * @param map name of map, can be null
     * @throws IllegalArgumentException when there is non null name of elements which doesn't exists
     */
    public void deleteByPath(@NotNull final UsersGroup group,
                             @Nullable final Building building,
                             @Nullable final FloorMap map)
            throws IllegalArgumentException {

        File mapFile = new File(context.getApplicationContext().getFilesDir().getAbsolutePath() +
                "/" + userAccount.getID() + "/" + group.getName());
        boolean isDownloaded = userAccount.findDownloadedGroup(group.getName()) != null;
        if (userAccount.findByName(group.getName()) == null
                && userAccount.findDownloadedGroup(group.getName()) == null) {
            throw new IllegalArgumentException("Doesn't exists group: " + group.getName());
        }
        if (building == null) {
            deleteRecursive(mapFile);
            if (isDownloaded) {
                userAccount.getDownloadedGroupsMap().remove(group.getName());
            } else {
                userAccount.getInnerMap().remove(group.getName());
            }
            netManager.deleteReference(group, null, null);
            return;
        }

        mapFile = new File(mapFile, building.getName());
        if (group.findByName(building.getName()) == null) {
            throw new IllegalArgumentException("Doesn't exists building: " + building.getName());
        }
        if (map == null) {
            deleteRecursive(mapFile);
            group.getInnerMap().remove(building.getName());
            netManager.deleteReference(group, building, null);
            return;
        }

        mapFile = new File(mapFile, map.getName() + ".plannet");
        if (building.findByName(map.getName()) == null) {
            throw new IllegalArgumentException("Doesn't exists map: " + map.getName());
        }
        building.getInnerMap().remove(map.getName());
        deleteRecursive(mapFile);

        netManager.deleteReference(group, building, map);
    }

    /**
     * Load data from local files to user account
     */
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

    /**
     * Method for searching groups on server
     * @param substring substring of names of groups
     * @param ownersAndGroups list for results of searching
     */
    public void getSearchedGroupsAndOwners(@NonNull String substring,
                                           @NonNull final List<MainActivity.SearchResult> ownersAndGroups) {
        CountDownLatch latch = new CountDownLatch(1);
        netManager.getGroupsWhichContainsName(substring, ownersAndGroups, latch);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void searchGroupMaps(@NonNull String owner, @NonNull String group,
                                @NonNull List<String> floorsPaths, @NonNull AtomicBoolean isFinished) {
        netManager.searchGroupMaps(owner, group, floorsPaths, isFinished, userAccount);
    }


    public void searchMaps(@NonNull List<String> floorsPaths, @NonNull AtomicBoolean isFinished) {
        netManager.searchMaps(floorsPaths, isFinished, userAccount);
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

    public void setIsPrivate(@NonNull UsersGroup group, boolean isPrivate) {
        group.setPrivate(isPrivate);
        netManager.setIsPrivate(group, isPrivate);
    }

    public void setIsEditable(@NonNull UsersGroup group, boolean isEditable) {
        group.setEditable(isEditable);
        netManager.setIsEditable(group, isEditable);
    }

    /**
     * Save map to account and file and send it to server
     */
    public void saveMap(@NonNull final FloorMap map)
            throws IllegalArgumentException {
        UsersGroup userGroup;
        if (map.getOwner().equals(userAccount.getID())) {
            userGroup = userAccount.findByName(map.getGroupName());
        } else {
            userGroup = userAccount.findDownloadedGroup(map.getGroupName());
        }
        Log.d("saveMap", "search: " + map.getGroupName());
        if (userGroup == null) {
            throw new IllegalArgumentException("This user haven't group: " + map.getGroupName());
        }

        Building building = userGroup.findByName(map.getBuildingName());
        if (building == null) {
            throw new IllegalArgumentException("User's group '" + map.getGroupName()
                    + "' haven't building: " + map.getBuildingName());
        }

        building.setElementToContainer(map);
        Log.d(DATA_TAG, "set new map to account");

        writeMap(map);
        Toast.makeText(context, "Map saved", Toast.LENGTH_SHORT).show();


        netManager.putMapOnServer(map, userGroup);
    }

    //private function for writing map to file
    private void writeMap(@NonNull final FloorMap map) {
        File accountFile = formingFileFromMap(map);
        accountFile.getParentFile().mkdirs();

        try (ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(accountFile))) {
            ous.writeObject(map);

        } catch (IOException e) {
            Toast.makeText(context, "Can't save a map to the phone", Toast.LENGTH_SHORT).show();
            Log.d(DATA_TAG, "Can't write a map to the phone");
            e.printStackTrace();
        }
    }

    //private function for reading map from file
    private void readMapFromFile(File mapFile) {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(mapFile))) {
            FloorMap map = (FloorMap) ois.readObject();
            UsersGroup group;

            Log.d("readMap", map.getGroupName() + "/" + map.getName());

            if (!map.getOwner().equals(userAccount.getID())) {
                //add prefix "owner_" to groupName
                String oldGroupName = map.getGroupName();
                if (!map.getGroupName().startsWith(map.getOwner() + "_")) {
                    map.setPath(map.getOwner() + "_" + map.getGroupName(),
                            map.getBuildingName());
                }

                group = userAccount.findDownloadedGroup(map.getGroupName());
                if (group == null) {
                    group = new UsersGroup(map.getGroupName());
                    userAccount.addDownloadedGroup(group);
                    group.setVisibleName(oldGroupName + " by " + map.getOwner());
                    netManager.setUpDownloadedGroup(group, map.getOwner());
                }
            } else {
                group = userAccount.findByName(map.getGroupName());
                if (group == null) {
                    group = userAccount.setElementToContainer(new UsersGroup(map.getGroupName()));
                    netManager.setUpDownloadedGroup(group, map.getOwner());
                }
            }

            Building building = group.findByName(map.getBuildingName());
            if (building == null) {
                building = group.setElementToContainer(new Building(map.getBuildingName()));
            }

            building.setElementToContainer(map);

            Log.d(DATA_TAG, "map " + map.getName() + " was read");
        } catch (Exception exception) {
            Log.d(DATA_TAG, "map can't be read");
            exception.printStackTrace();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                Log.d("Delete recursive", child.getAbsolutePath());
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    @NonNull
    private File formingFileFromMap(@NonNull final FloorMap map) {
        return new File(context.getApplicationContext().getFilesDir(),
                userAccount.getID() + "/"
                        + map.getGroupName() + "/"
                        + map.getBuildingName() + "/"
                        + map.getName() + ".plannet"
        );
    }
}
