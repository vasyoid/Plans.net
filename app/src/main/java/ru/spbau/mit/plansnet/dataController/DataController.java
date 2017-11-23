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
    private NetworkDataManager netManager;
    private Account userAccount;

    private static final String DATA_TAG = "DATA_CONTROLLER_FILES";

    public DataController(@NonNull final Context context, @NonNull final FirebaseUser account) {
        netManager = new NetworkDataManager(account);
        userAccount = netManager.getAccount(context);

    }

    public void writeMap(@NonNull final Context context, @NonNull final FloorMap map) {
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

    /**
     * Save map to account and send it to netWork
     */
    public void saveMap(@NonNull final Context context,
                        @NonNull final FloorMap map,
                        @NonNull final String groupName,
                        @NonNull final String buildingName)
            throws IllegalArgumentException {


        UsersGroup userGroup = userAccount.findByName(groupName);
        if (userGroup == null) {
            throw new IllegalArgumentException("This user haven't group: " + groupName);
        }

        Building building = userGroup.findByName(buildingName);
        if (building == null) {
            throw new IllegalArgumentException("User's group '" + groupName
                    + "' haven't building: " + buildingName);
        }

        building.setElementToContainer(map);

        writeMap(context, map);

        netManager.putMapOnServer(map);
    }


}
