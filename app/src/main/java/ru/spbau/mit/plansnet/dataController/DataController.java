package ru.spbau.mit.plansnet.dataController;

import ru.spbau.mit.plansnet.data.Account;
import ru.spbau.mit.plansnet.data.Building;
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.UsersGroup;

/**
 * Controller class which manage data and network connection
 */

public class DataController {
    private NetworkDataManager netManager;
    /**
     * Save map to account and send it to netWork
     */
    public void saveMap(Object accountID, FloorMap map, String groupName, String buildingName)
            throws IllegalArgumentException {

        Account user = netManager.getAccountByID(accountID);

        UsersGroup userGroup = user.findByName(groupName);
        if (userGroup == null) {
            throw new IllegalArgumentException("This user haven't group: " + groupName);
        }

        Building building = userGroup.findByName(buildingName);
        if (building == null) {
            throw new IllegalArgumentException("User's group '" + groupName
                    + "' haven't building: " + buildingName);
        }

        building.setElementToContainer(map);

        netManager.updateAccount(user);
    }


}
