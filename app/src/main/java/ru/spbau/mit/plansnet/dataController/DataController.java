package ru.spbau.mit.plansnet.dataController;

import com.google.firebase.auth.FirebaseUser;

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

    DataController(final FirebaseUser account) {
        netManager = new NetworkDataManager(account);
    }
    /**
     * Save map to account and send it to netWork
     */
    public void saveMap(FloorMap map, String groupName, String buildingName)
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

        netManager.putMapOnServer(map);
    }


}
