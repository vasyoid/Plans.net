package ru.spbau.mit.plansnet.dataController;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

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

    public DataController(@NonNull final Context context, @NonNull final FirebaseUser account) {
        this.context = context;
        netManager = new NetworkDataManager(context, account);
        userAccount = new Account(account.getDisplayName());
        netManager.getAccount(userAccount);
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

    public void saveGroup(@NonNull final UsersGroup group) {
        userAccount.setElementToContainer(group);
    }

    public UsersGroup getGroup(@NonNull String groupName) {
        return userAccount.findByName(groupName);
    }

    public FloorMap getMap(@NonNull String groupName,
                           @NonNull String buildingName,
                           @NonNull String mapName) {
        return userAccount.findByName(groupName).findByName(buildingName).findByName(mapName);
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

        writeMap(map);

        netManager.putMapOnServer(map);
    }


}
