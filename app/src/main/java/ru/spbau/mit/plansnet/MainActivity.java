package ru.spbau.mit.plansnet;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


import ru.spbau.mit.plansnet.constructor.ConstructorActivity;
import ru.spbau.mit.plansnet.constructor.ViewerActivity;
import ru.spbau.mit.plansnet.data.AbstractNamedData;
import ru.spbau.mit.plansnet.data.Building;
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.UsersGroup;
import ru.spbau.mit.plansnet.dataController.DataController;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_IN_TAG = "LogIn";
    private static final int RC_GET_TOKEN = 9002;
    private static final int CONSTRUCTOR_TOKEN = 9003;

    private DataController dataController;
    private FirebaseUser user;

    private final Pattern allowedStringPattern = Pattern.compile("[a-zA-Zа-яА-ЯёЁ0-9_ ]*");

    private InputFilter filter = (charSequence, i, i1, spanned, i2, i3) -> {
        if (charSequence != null && !allowedStringPattern.matcher(charSequence).matches()) {
            return "";
        }
        return null;
    };

    @Nullable
    private UsersGroup currentGroup;
    @Nullable
    private Building currentBuilding;
    @Nullable
    private FloorMap currentMap;

    @NonNull
    private final List<UsersGroup> myGroupList = new ArrayList<>();
    @NonNull
    private final List<UsersGroup> netGroupList = new ArrayList<>();
    @NonNull
    private final List<Building> buildingList = new ArrayList<>();
    @NonNull
    private final List<FloorMap> floorList = new ArrayList<>();
    private ArrayAdapter<UsersGroup> myGroupListAdapter;
    private ArrayAdapter<UsersGroup> netGroupListAdapter;
    private ArrayAdapter<Building> buildingListAdapter;
    private ArrayAdapter<FloorMap> floorListAdapter;

    @NonNull
    private final List<SearchResult> findList = new ArrayList<>();
    private ArrayAdapter<SearchResult> findListAdapter;
    private GoogleSignInClient mGoogleSignInClient;

    private Button btnViewer;
    private Button btnConstructor;
    private Button btnCopyMap;

    private void createNewGroupDialog(@Nullable final FloorMap toSave) {
        AlertDialog newGroupDialog = new AlertDialog.Builder(MainActivity.this).create();
        newGroupDialog.setTitle("enter name of new group");

        EditText groupNameInput = new EditText(MainActivity.this);
        groupNameInput.setFilters(new InputFilter[]{filter});
        newGroupDialog.setView(groupNameInput);

        newGroupDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
            String newGroupName = groupNameInput.getText().toString().trim();

            if (newGroupName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Name is empty", Toast.LENGTH_LONG).show();
                return;
            }

            if (dataController.getAccount().findByName(newGroupName) != null) {
                Toast.makeText(MainActivity.this, "This group already exists", Toast.LENGTH_LONG).show();
                return;
            }
            UsersGroup group = dataController.addGroup(new UsersGroup(newGroupName));
            myGroupList.add(group);
            myGroupListAdapter.notifyDataSetChanged();

            createNewBuildingDialog(group, toSave);
        });

        newGroupDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        newGroupDialog.show();
    }

    private void createNewBuildingDialog(UsersGroup chosenGroup, @Nullable final FloorMap toSave) {
        AlertDialog newBuildingDialog = new AlertDialog.Builder(MainActivity.this).create();
        newBuildingDialog.setTitle("enter name of new building");

        EditText buildingNameInput = new EditText(MainActivity.this);
        buildingNameInput.setFilters(new InputFilter[]{filter});
        newBuildingDialog.setView(buildingNameInput);

        newBuildingDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
            String newBuildingName = buildingNameInput.getText().toString().trim();

            if (newBuildingName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Name is empty", Toast.LENGTH_LONG).show();
                return;
            }

            if (chosenGroup.findByName(newBuildingName) != null) {
                Toast.makeText(MainActivity.this, "This building already exists", Toast.LENGTH_LONG).show();
                return;
            }
            Building building = dataController.addBuildingToGroup(new Building(newBuildingName), chosenGroup);
            buildingList.clear();
            if (currentGroup == chosenGroup) {
                buildingList.addAll(currentGroup.getValues());
            }
            buildingListAdapter.notifyDataSetChanged();
            createNewMapDialog(chosenGroup, building, toSave);
        });

        newBuildingDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        newBuildingDialog.show();
    }

    private void createNewMapDialog(UsersGroup chosenGroup, Building chosenBuilding,
                                    @Nullable FloorMap toSave) {
        AlertDialog newMapNameDialog = new AlertDialog.Builder(MainActivity.this).create();
        newMapNameDialog.setTitle("enter name of new floor");

        EditText mapNameInput = new EditText(MainActivity.this);
        mapNameInput.setFilters(new InputFilter[]{filter});
        newMapNameDialog.setView(mapNameInput);

        newMapNameDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
            String newMapName = mapNameInput.getText().toString().trim();

            if (newMapName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Name is empty", Toast.LENGTH_LONG).show();
                return;
            }

            if (chosenBuilding.findByName(newMapName) != null) {
                Toast.makeText(MainActivity.this,
                        "This floor already exists", Toast.LENGTH_LONG).show();
                return;
            }
            FloorMap floor = new FloorMap(
                    dataController.getAccount().getID(),
                    chosenGroup.getName(),
                    chosenBuilding.getName(),
                    newMapName
            );
            floor.copyMap(toSave);
            dataController.saveMap(floor);
            floorListActivate();
        });

        newMapNameDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        newMapNameDialog.show();
    }


    private void createChooseBuildingForNewMapDialog(UsersGroup chosenGroup,
                                                     @Nullable final FloorMap toSave) {
        AlertDialog chooseBuildingsForNewMapDialog = new AlertDialog.Builder(MainActivity.this).create();
        chooseBuildingsForNewMapDialog.setTitle("select building");

        ListView buildingsSuggestedList = new ListView(MainActivity.this);
        chooseBuildingsForNewMapDialog.setView(buildingsSuggestedList);

        final List<Building> buildingList = chosenGroup.getListOfData();
        ArrayAdapter<Building> buildingAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, buildingList);

        buildingsSuggestedList.setAdapter(buildingAdapter);
        buildingsSuggestedList.setOnItemClickListener((adapterView, view, i, l) -> {
            Building chosenBuilding = buildingList.get(i);
            chooseBuildingsForNewMapDialog.cancel();
            createNewMapDialog(chosenGroup, chosenBuilding, null);
        });

        chooseBuildingsForNewMapDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Add new building",
                (dialog, which) -> createNewBuildingDialog(chosenGroup, toSave));
        chooseBuildingsForNewMapDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        chooseBuildingsForNewMapDialog.show();
    }

    private void createChooseGroupForNewMapDialog(@Nullable final FloorMap toSave) {
        AlertDialog chooseGroupForNewMapDialog = new AlertDialog.Builder(MainActivity.this).create();
        chooseGroupForNewMapDialog.setTitle("select group");

        ListView groupsSuggestedList = new ListView(MainActivity.this);
        chooseGroupForNewMapDialog.setView(groupsSuggestedList);

        ArrayAdapter<UsersGroup> groupAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, myGroupList);

        groupsSuggestedList.setAdapter(groupAdapter);
        groupsSuggestedList.setOnItemClickListener((adapterView, view, i, l) -> {
            UsersGroup chosenGroup = myGroupList.get(i);
            chooseGroupForNewMapDialog.cancel();
            createChooseBuildingForNewMapDialog(chosenGroup, toSave);
        });

        groupAdapter.notifyDataSetChanged();
        chooseGroupForNewMapDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Add new group",
                (dialog, which) -> createNewGroupDialog(toSave));
        chooseGroupForNewMapDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});
        chooseGroupForNewMapDialog.show();
    }

    private void createDeleteDialog(@NonNull UsersGroup group, @Nullable Building building,
                                    @Nullable FloorMap floor) {
        AlertDialog deleteDialog = new AlertDialog.Builder(MainActivity.this).create();
        String title = "Delete /" + group.toString();
        if (building != null) {
            title += "/" + building.getName();
        }
        if (floor != null) {
            title += "/" + floor.getName();
        }
        title += "?";
        deleteDialog.setTitle(title);

        TextView questionText = new TextView(MainActivity.this);
        questionText.setText(getString(R.string.deleteQuestion));
        questionText.setPadding(10, 10, 10, 10);
        deleteDialog.setView(questionText);

        deleteDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
            dataController.deleteByPath(group, building, floor);
            updateGroupLists();

            if (currentGroup != null && group.equals(currentGroup)) {
                currentGroup = null;
                currentBuilding = null;

                buildingList.clear();
                buildingListAdapter.notifyDataSetChanged();

                floorListInactivate();
            }
        });

        deleteDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        deleteDialog.show();
    }

    private void createGroupSettingsDialog(@NonNull final UsersGroup group) {
        AlertDialog groupSettingsDialog = new AlertDialog.Builder(MainActivity.this).create();
        groupSettingsDialog.setTitle("Group settings: " + group.toString());

        LinearLayoutCompat settings = (LinearLayoutCompat) getLayoutInflater()
                .inflate(R.layout.group_settings, null);


        final Button backBtn = settings.findViewById(R.id.backButton);
        final ListView hierarchy = settings.findViewById(R.id.hierarchyList);
        final CheckBox isPrivateBox = settings.findViewById(R.id.isPrivateCheckBox);
        final CheckBox isEditableBox = settings.findViewById(R.id.isEditableCheckBox);
        final TextView pathTextView = settings.findViewById(R.id.pathTextView);

        final List<AbstractNamedData> hierarchyList = new ArrayList<>();
        final ArrayAdapter<AbstractNamedData> hierarchyListAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                hierarchyList
        );
        hierarchy.setAdapter(hierarchyListAdapter);

        if (!group.getName().equals(group.toString())) {
            isPrivateBox.setVisibility(View.GONE);
            isEditableBox.setVisibility(View.GONE);
        }

        isPrivateBox.setChecked(group.isPrivate());
        isEditableBox.setChecked(group.isEditable());

        pathTextView.setText(String.format("path: /%s/", group.toString()));

        hierarchy.setOnItemClickListener((adapterView, view, i, l) -> {
            if (hierarchyList.get(i) instanceof Building) {
                Building building = (Building)hierarchyList.get(i);

                hierarchyList.clear();
                hierarchyList.addAll(building.getValues());
                hierarchyListAdapter.notifyDataSetChanged();

                pathTextView.setText(String.format("path: /%s/%s/",group.toString(), building.toString()));
                backBtn.setEnabled(true);
            }
        });

        hierarchy.setOnItemLongClickListener((adapterView, view, i, l) -> {
            AbstractNamedData item = hierarchyList.get(i);
            if (item instanceof FloorMap) {
                FloorMap map = (FloorMap)item;
                createDeleteDialog(group, group.findByName(map.getBuildingName()), map);
                groupSettingsDialog.cancel();
            } else if (item instanceof Building) {
                createDeleteDialog(group, (Building)item, null);
                groupSettingsDialog.cancel();
            }
            return true;
        });

        backBtn.setEnabled(false);
        backBtn.setOnClickListener(view -> {
            hierarchyList.clear();
            hierarchyList.addAll(group.getValues());
            hierarchyListAdapter.notifyDataSetChanged();
            backBtn.setEnabled(false);
            pathTextView.setText(String.format("path: /%s/", group.toString()));
        });

        isPrivateBox.setOnCheckedChangeListener((compoundButton, b) ->
                dataController.setIsPrivate(group, b));
        isEditableBox.setOnCheckedChangeListener((compoundButton, b) ->
                dataController.setIsEditable(group, b));

        groupSettingsDialog.setView(settings);
        groupSettingsDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, i) -> {});
        groupSettingsDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Delete group",
                (dialog, i) -> createDeleteDialog(group, null, null));

        groupSettingsDialog.show();

        hierarchyList.addAll(group.getValues());
        hierarchyListAdapter.notifyDataSetChanged();
    }

    private void setUpMyGroupListView() {
        ListView groupListView = findViewById(R.id.myGroupListView);

        myGroupListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                myGroupList);

        groupListView.setAdapter(myGroupListAdapter);

        groupListView.setOnItemClickListener((parent, view, i, id) -> {
            currentGroup = myGroupList.get(i);
            assert currentGroup != null;

            buildingList.clear();
            if (currentGroup != null) {
                buildingList.addAll(currentGroup.getValues());
            }
            buildingListAdapter.notifyDataSetChanged();

            if (buildingList.size() != 0) {
                currentBuilding = buildingList.get(0);
            }
            floorListActivate();
        });

        groupListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            UsersGroup group = myGroupList.get(i);
            assert group != null;
            createGroupSettingsDialog(group);
            return true;
        });
    }

    private void setUpNetGroupListView() {
        ListView groupListView = findViewById(R.id.netGroupListView);

        netGroupListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                netGroupList);

        groupListView.setAdapter(netGroupListAdapter);

        groupListView.setOnItemClickListener((parent, view, i, id) -> {
            currentGroup = netGroupList.get(i);
            assert currentGroup != null;

            buildingList.clear();
            if (currentGroup != null) {
                buildingList.addAll(currentGroup.getValues());
            }
            buildingListAdapter.notifyDataSetChanged();

            if (buildingList.size() != 0) {
                currentBuilding = buildingList.get(0);
            }

            floorListActivate();
        });

        groupListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            UsersGroup group = netGroupList.get(i);
            assert group != null;
            createGroupSettingsDialog(group);
            return true;
        });
    }

    private void setUpBuildingSpinnerView() {
        Spinner buildingSpinnerView = findViewById(R.id.buildingSpinnerView);

        buildingListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                buildingList);

        buildingSpinnerView.setAdapter(buildingListAdapter);

        buildingSpinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentGroup == null) {
                    buildingList.clear();
                    buildingListAdapter.notifyDataSetChanged();
                    return;
                }

                currentBuilding = buildingList.get(i);
                floorListActivate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("Spinner", "OnNothingSelected in building");
            }
        });
    }

    private void setUpFloorSpinnerView() {
        Spinner floorSpinnerView = findViewById(R.id.floorSpinnerView);
        floorListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                floorList);

        floorSpinnerView.setAdapter(floorListAdapter);

        floorSpinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentBuilding == null) {
                    floorListInactivate();
                    return;
                }
                currentMap = floorList.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("Spinner", "OnNothingSelected in floor");
            }
        });
    }

    private void setUpFindListView() {
        ListView findListView = findViewById(R.id.findListView);
        findListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                findList);

        findListView.setAdapter(findListAdapter);

        findListView.setOnItemClickListener((parent, view, i, id) -> {
            SearchResult searchResult = findList.get(i);
            findList.clear();
            findListAdapter.notifyDataSetChanged();
            new SearchAndDownloadGroupAsyncTask(MainActivity.this).execute(searchResult);
        });
    }

    private void setUpSearchView() {
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                new SearchAsyncTask(MainActivity.this).execute(text);
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        searchView.setOnCloseListener(() -> {
            findList.clear();
            findListAdapter.notifyDataSetChanged();
            return false;
        });
    }

    private void setUpTabHost() {
        TabHost tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec("Tag1");
        spec.setContent(R.id.myGroupLayout);
        spec.setIndicator("MY");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("Tag2");
        spec.setContent(R.id.netGroupListView);
        spec.setIndicator("NET");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("Tag3");
        spec.setContent(R.id.searchLayout);
        spec.setIndicator("SRCH");
        tabHost.addTab(spec);

        LinearLayout ll = (LinearLayout) tabHost.getChildAt(0);
        TabWidget tabWidget = (TabWidget) ll.getChildAt(0);
        for (int i = 0; i < tabWidget.getChildCount(); i++)
        {
            View tabView = tabWidget.getChildTabViewAt(i);
            TextView tv = tabView.findViewById(android.R.id.title);
            tv.setTextSize(i < 2 ? 10 : 8);
        }
    }

    private void floorListActivate() {
        floorList.clear();
        if (currentBuilding != null) {
            floorList.addAll(currentBuilding.getValues());
            if (currentBuilding.getValues().size() > 0) {
                currentMap = currentBuilding.getValues().iterator().next();
            }
        }
        floorListAdapter.notifyDataSetChanged();
        if (currentGroup != null && !currentGroup.getName().equals(currentGroup.toString())) {
            btnViewer.setEnabled(!currentGroup.isPrivate());
            btnConstructor.setEnabled(!currentGroup.isPrivate() && currentGroup.isEditable());
            btnCopyMap.setEnabled(!currentGroup.isPrivate());
        } else {
            btnViewer.setEnabled(true);
            btnConstructor.setEnabled(true);
            btnCopyMap.setEnabled(true);
        }
    }

    private void floorListInactivate() {
        floorList.clear();
        floorListAdapter.notifyDataSetChanged();
        currentMap = null;
//        btnConstructor.setEnabled(false);// for easy debug TODO: uncomment on release
        btnViewer.setEnabled(false);
        btnCopyMap.setEnabled(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnViewer = findViewById(R.id.btnViewer);
        btnConstructor = findViewById(R.id.btnConstructor);
        btnCopyMap = findViewById(R.id.btnCopyMap);

        setUpBuildingSpinnerView();
        setUpFloorSpinnerView();
        setUpMyGroupListView();
        setUpNetGroupListView();
        setUpFindListView();
        setUpSearchView();
        setUpTabHost();

    }

    @Override
    protected void onStart() {
        super.onStart();
        logIn();
    }

    private void logIn() {
        // Show an account picker to let the user choose a Google account from the device.
        // If the GoogleSignInOptions only asks for IDToken and/or profile and/or email then no
        // consent screen will be shown here.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();

        startActivityForResult(signInIntent, RC_GET_TOKEN);
    }

    // [START handle_sign_in_result]
    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.w(LOG_IN_TAG, "handleSignInResult:error", e);
            Toast.makeText(MainActivity.this, "Authentication failed.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
        Log.d(LOG_IN_TAG, "firebaseAuthWithGoogle:" + account.getId());

        final FirebaseAuth auth = FirebaseAuth.getInstance();

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }

                    // Sign in success, update UI with the signed-in user's information
                    Log.d(LOG_IN_TAG, "signInWithCredential:success");
                    Log.d("MYTEST", "firebase auth");
                    user = auth.getCurrentUser();
                    afterAuth();
                });
    }

    private void afterAuth() {
        dataController = new DataController(getApplicationContext(), user);

        new SearchAndDownloadMapsAsyncTask(this).execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class SearchAndDownloadMapsAsyncTask extends AsyncTask<Void, Void, Void> {
        @NonNull private ProgressDialog dialog;
        @NonNull private List<String> floorsPaths = new ArrayList<>();
        @NonNull private MainActivity activity;
        @NonNull private final AtomicBoolean isFinished = new AtomicBoolean(false);

        SearchAndDownloadMapsAsyncTask(@NonNull MainActivity activity) {
            dialog = new ProgressDialog(activity);
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            dialog.setTitle("Search maps on server");
            dialog.setCancelable(false);
            dialog.setMessage("Searching...");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("SearchMapsAsync", "In Searching");
            dataController.searchMaps(floorsPaths, isFinished);
            synchronized (isFinished) {
                Log.d("SearchMapsAsync", "In sync");
                while (!isFinished.get()) {
                    try {
                        Log.d("SearchMapsAsync", "before wait");
                        isFinished.wait();
                        Log.d("SearchMapsAsync", "after wait");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("SearchMapsAsync", "after while");
            }
            Log.d("SearchMapsAsync", "after sync");

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            new DownloadMapsAsyncTask(activity, floorsPaths).execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadMapsAsyncTask extends AsyncTask<Void, Void, Void> {
        @NonNull private ProgressDialog dialog;
        @NonNull private List<String> floorsPaths;
        private final AtomicInteger mapCount = new AtomicInteger(0);

        DownloadMapsAsyncTask(MainActivity activity, @NonNull List<String> floorsPaths) {
            dialog = new ProgressDialog(activity);
            this.floorsPaths = floorsPaths;
        }

        @Override
        protected void onPreExecute() {
            Log.d("AsyncWork", "starts download async task");
            dialog.setTitle("Loading maps from server");
            dialog.setCancelable(false);
            dialog.setMessage("Loading...");
            dialog.setMax(floorsPaths.size());
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.show();
        }

        protected Void doInBackground(Void... args) {
            mapCount.set(0);
            synchronized (mapCount) {
                dataController.downloadMaps(floorsPaths, mapCount);
                while (mapCount.get() < floorsPaths.size()) {
                    try {
                        mapCount.wait();
                        dialog.setProgress(mapCount.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d("SYNCHRONIZED_ERROR", e.getMessage());
                    }
                }
            }
            return null;
        }


        protected void onPostExecute(Void result) {
            // do UI work here
            Log.d("AsyncWork", "ends download async task");
            if (dialog.isShowing()) {
                Log.d("AsyncWork", "dismiss download async task");
                dialog.dismiss();
            }
            new LoadMapsAsyncTask(MainActivity.this).execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadMapsAsyncTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;

        LoadMapsAsyncTask(MainActivity activity) {
            dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            Log.d("AsyncWork", "starts load async task");
            dialog.setTitle("Loading maps from storage");
            dialog.setCancelable(false);
            dialog.setMessage("Loading...");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();
        }

        protected Void doInBackground(Void... args) {
            Log.d("AsyncWork", "load async starts work");
            dataController.loadLocalFiles();
            Log.d("AsyncWork", "load async task done");
            return null;
        }

        protected void onPostExecute(Void result) {
            // do UI work here
            Log.d("AsyncWork", "ends load async task");
            if (dialog.isShowing()) {
                Log.d("AsyncWork", "dismiss load async task");
                dialog.dismiss();
            }

            updateGroupLists();

            buildingList.clear();
            buildingListAdapter.notifyDataSetChanged();

            floorListInactivate();
        }
    }

    //helper structure for search results
    public static class SearchResult {
        public String ownerName, ownerId, groupName;

        public SearchResult(String ownerId, String ownerName, String groupName) {
            this.ownerName = ownerName;
            this.ownerId = ownerId;
            this.groupName = groupName;
        }

        @Override
        public String toString() {
            return ownerName + " - " + groupName;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SearchAsyncTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog dialog;
        private List<SearchResult> list = new ArrayList<>(); // list with <OwnerId, OwnerName, Group>. It is keys in Database

        SearchAsyncTask(MainActivity activity) {
            dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            Log.d("AsyncWork", "starts search async task");
        }

        protected Void doInBackground(String... args) {
            if (args.length == 0) {
                Log.d("AsyncWork", "nothing to search");
                return null;
            }
            dataController.getSearchedGroupsAndOwners(args[0], list);
            return null;
        }

        protected void onPostExecute(Void result) {
            // do UI work here
            Log.d("AsyncWork", "ends search async task");
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            findList.clear();
            findList.addAll(list);
            findListAdapter.notifyDataSetChanged();

            for (SearchResult x : list) {
                Log.d("AsyncWork!!", "in list: " + x);
            }

        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SearchAndDownloadGroupAsyncTask extends AsyncTask<SearchResult, Void, Void> {
        @NonNull private ProgressDialog dialog;
        @NonNull private List<String> floorsPaths = new ArrayList<>();
        @NonNull private MainActivity activity;
        @NonNull private final AtomicBoolean isFinished = new AtomicBoolean(false);
        private SearchResult arg;

        SearchAndDownloadGroupAsyncTask(@NonNull MainActivity activity) {
            dialog = new ProgressDialog(activity);
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            dialog.setTitle("Search maps of group on server");
            dialog.setCancelable(false);
            dialog.setMessage("Searching...");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();
        }

        @Override
        protected Void doInBackground(SearchResult... args) {
            if (args.length == 0) {
                throw new RuntimeException("Expected arguments in SearchAndDownloadGroupAsyncTask");
            }
            arg = args[0];
            dataController.searchGroupMaps(arg.ownerId, arg.groupName,
                    floorsPaths, isFinished);
            synchronized (isFinished) {
                Log.d("SearchGroups", "before while");
                while (!isFinished.get()) {
                    try {
                        Log.d("SearchGroups", "start wait");
                        isFinished.wait();
                        Log.d("SearchGroups", "end wait");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("SearchGroups", "after while");

            }
            Log.d("SearchGroups", "after sync");

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            new DownloadMapsAsyncTask(activity, floorsPaths).execute();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case RC_GET_TOKEN:
            // [START get_id_token]
            // This task is always completed immediately, there is no need to attach an
            // asynchronous listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
            // [END get_id_token]
            break;

        case CONSTRUCTOR_TOKEN:
            if (data == null) {
                Toast.makeText(this, "Nothing to save", Toast.LENGTH_LONG).show();
                break;
            }

            FloorMap toSaveMap = (FloorMap) data.getSerializableExtra("toSaveMap");
            if (toSaveMap != null) {
                dataController.saveMap(toSaveMap);
                floorListAdapter.notifyDataSetChanged();
            }
//            currentMap = toSaveMap;
//            myGroupList.clear();for what?
            break;
        }
    }

    public void openViewer(View v) {
        Intent intent = new Intent(MainActivity.this, ViewerActivity.class);
        if (currentMap != null) {
            intent.putExtra("currentMap", currentMap);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No map chosen", Toast.LENGTH_LONG).show();
        }
    }

    public void openConstructor(View v) {
        Intent intent = new Intent(MainActivity.this, ConstructorActivity.class);
        if (currentMap != null) {
            intent.putExtra("currentMap", currentMap);
        }
        startActivityForResult(intent, CONSTRUCTOR_TOKEN);
    }

    private void updateGroupLists() {
        myGroupList.clear();
        myGroupList.addAll(dataController.getAccount().getValues());
        myGroupListAdapter.notifyDataSetChanged();

        netGroupList.clear();
        netGroupList.addAll(dataController.getAccount().getDownloadedGroups());
        netGroupListAdapter.notifyDataSetChanged();
    }

    public void logOut(View v) {
        myGroupList.clear();
        netGroupList.clear();
        buildingList.clear();
        findList.clear();

        myGroupListAdapter.notifyDataSetChanged();
        netGroupListAdapter.notifyDataSetChanged();
        buildingListAdapter.notifyDataSetChanged();
        findListAdapter.notifyDataSetChanged();

        floorListInactivate();

        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> logIn());
        Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
    }

    public void copyMap(View v) {
        createChooseGroupForNewMapDialog(currentMap);
    }

    public void openHelp(View v) {
        AlertDialog help = new AlertDialog.Builder(MainActivity.this).create();

        TextView helpText = new TextView(MainActivity.this);
        help.setView(helpText);

        helpText.setText(getText(R.string.helpText));
        helpText.setPadding(20, 20, 20, 0);
        help.show();
    }

    public void addMap(View v) {
        createChooseGroupForNewMapDialog(null);
    }
}
