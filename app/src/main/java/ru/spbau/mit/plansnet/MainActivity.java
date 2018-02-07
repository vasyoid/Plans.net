package ru.spbau.mit.plansnet;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


import ru.spbau.mit.plansnet.constructor.ConstructorActivity;
import ru.spbau.mit.plansnet.constructor.ViewerActivity;
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

    InputFilter filter = (charSequence, i, i1, spanned, i2, i3) -> {//TODO: why spaces doesn't works?
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
    private final List<String> groupList = new ArrayList<>();
    @NonNull
    private final List<String> buildingList = new ArrayList<>();
    @NonNull
    private final List<String> floorList = new ArrayList<>();
    private ArrayAdapter<String> groupListAdapter;
    private ArrayAdapter<String> buildingListAdapter;
    private ArrayAdapter<String> floorListAdapter;

    @NonNull
    private final List<SearchResult> findList = new ArrayList<>();
    private ArrayAdapter<SearchResult> findListAdapter;
    private GoogleSignInClient mGoogleSignInClient;

    private void createNewGroupDialog() {
        AlertDialog newGroupDialog = new AlertDialog.Builder(MainActivity.this).create();
        newGroupDialog.setTitle("enter name of new group");

        EditText groupNameInput = new EditText(MainActivity.this);
        groupNameInput.setFilters(new InputFilter[]{filter});
        newGroupDialog.setView(groupNameInput);

        newGroupDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
            String newGroupName = groupNameInput.getText().toString();
            if (dataController.getAccount().findByName(newGroupName) != null) {
                Toast.makeText(MainActivity.this, "This group already exists", Toast.LENGTH_LONG).show();
                return;
            }
            dataController.addGroup(new UsersGroup(newGroupName));
            groupList.add(newGroupName);
            groupListAdapter.notifyDataSetChanged();
        });

        newGroupDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        newGroupDialog.show();
    }

    private void createNewBuildingDialog(UsersGroup chosenGroup) {
        AlertDialog newBuildingDialog = new AlertDialog.Builder(MainActivity.this).create();
        newBuildingDialog.setTitle("enter name of new building");

        EditText buildingNameInput = new EditText(MainActivity.this);
        buildingNameInput.setFilters(new InputFilter[]{filter});
        newBuildingDialog.setView(buildingNameInput);

        newBuildingDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
            String newBuildingName = buildingNameInput.getText().toString();
            if (chosenGroup.findByName(newBuildingName) != null) {
                Toast.makeText(MainActivity.this, "This building already exists", Toast.LENGTH_LONG).show();
                return;
            }
            dataController.addBuildingToGroup(new Building(newBuildingName), chosenGroup);
            buildingList.clear();
            if (currentGroup == chosenGroup) {
                buildingList.addAll(currentGroup.getListOfNames());
            }
            buildingListAdapter.notifyDataSetChanged();
        });

        newBuildingDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        newBuildingDialog.show();
    }

    private void createNewMapDialog(UsersGroup chosenGroup, Building chosenBuilding) {
        AlertDialog newMapDialog = new AlertDialog.Builder(MainActivity.this).create();
        newMapDialog.setTitle("enter name of new floor");

        EditText mapNameInput = new EditText(MainActivity.this);
        mapNameInput.setFilters(new InputFilter[]{filter});
        newMapDialog.setView(mapNameInput);

        newMapDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
            String newMapName = mapNameInput.getText().toString();
            if (chosenBuilding.findByName(newMapName) != null) {
                Toast.makeText(MainActivity.this, "This floor already exists", Toast.LENGTH_LONG).show();
                return;
            }
            FloorMap floor = new FloorMap(
                    chosenGroup.getName(),
                    chosenBuilding.getName(),
                    newMapName
            );
            dataController.saveMap(floor);
            floorList.clear();
            if (currentBuilding == chosenBuilding) {
                floorList.addAll(currentBuilding.getListOfNames());
            }
            floorListAdapter.notifyDataSetChanged();
        });

        newMapDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        newMapDialog.show();
    }

    private void createChooseBuildingForNewMapDialog(UsersGroup chosenGroup) {
        AlertDialog chooseBuildingsForNewMapDialog = new AlertDialog.Builder(MainActivity.this).create();
        chooseBuildingsForNewMapDialog.setTitle("select building");

        ListView buildingsSuggestedList = new ListView(MainActivity.this);
        chooseBuildingsForNewMapDialog.setView(buildingsSuggestedList);

        final List<String> buildingList = chosenGroup.getListOfNames();
        ArrayAdapter<String> buildingAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, buildingList);

        buildingsSuggestedList.setAdapter(buildingAdapter);
        buildingsSuggestedList.setOnItemClickListener((adapterView, view, i, l) -> {
            Building chosenBuilding = chosenGroup.findByName(buildingList.get(i));
            chooseBuildingsForNewMapDialog.cancel();
            createNewMapDialog(chosenGroup, chosenBuilding);
        });

        chooseBuildingsForNewMapDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Add new building",
                (dialog, which) -> createNewBuildingDialog(chosenGroup));
        chooseBuildingsForNewMapDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        chooseBuildingsForNewMapDialog.show();
    }

    private void createChooseGroupForNewMapDialog() {
        AlertDialog chooseGroupForNewMapDialog = new AlertDialog.Builder(MainActivity.this).create();
        chooseGroupForNewMapDialog.setTitle("select group");

        ListView groupsSuggestedList = new ListView(MainActivity.this);
        chooseGroupForNewMapDialog.setView(groupsSuggestedList);

        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, groupList);

        groupsSuggestedList.setAdapter(groupAdapter);
        groupsSuggestedList.setOnItemClickListener((adapterView, view, i, l) -> {
            UsersGroup chosenGroup = dataController.getGroup(groupList.get(i));
            chooseGroupForNewMapDialog.cancel();
            createChooseBuildingForNewMapDialog(chosenGroup);
        });

        groupAdapter.notifyDataSetChanged();
        chooseGroupForNewMapDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Add new group",
                (dialog, which) -> createNewGroupDialog());
        chooseGroupForNewMapDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});
        chooseGroupForNewMapDialog.show();
    }

    private void createDeleteDialog(@NonNull String groupName, @Nullable String buildingName,
                                    @Nullable String floorName) {
        AlertDialog deleteDialog = new AlertDialog.Builder(MainActivity.this).create();
        String title = groupName;
        if (buildingName != null) {
            title += " : " + buildingName;
        }
        if (floorName != null) {
            title += " : " + floorName;
        }
        deleteDialog.setTitle(title);

        TextView questionText = new TextView(MainActivity.this);
        questionText.setText("Do you want to delete this?");
        deleteDialog.setView(questionText);

        deleteDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
            dataController.deleteByPath(groupName, buildingName, floorName);
            groupList.clear();
            groupList.addAll(dataController.getAccount().getListOfNames());
            groupListAdapter.notifyDataSetChanged();

            if (currentGroup != null && groupName.equals(currentGroup.getName())) {
                currentGroup = null;
                currentBuilding = null;
                currentMap = null;

                buildingList.clear();
                floorList.clear();

                buildingListAdapter.notifyDataSetChanged();
                floorListAdapter.notifyDataSetChanged();
            }
        });

        deleteDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                (dialog, which) -> {});

        deleteDialog.show();
    }

    private void setUpGroupListView() {
        ListView groupListView = findViewById(R.id.groupListView);

        groupListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                groupList);

        groupListView.setAdapter(groupListAdapter);
        Log.d("ID", "id: " + groupListView.getId());

        groupListView.setOnItemClickListener((parent, view, i, id) -> {
            currentGroup = dataController.getAccount().findByName(groupList.get(i));
            assert currentGroup != null;

            buildingList.clear();
            if (currentGroup != null) {
                buildingList.addAll(currentGroup.getListOfNames());
            }
            buildingListAdapter.notifyDataSetChanged();

            floorList.clear();
            if (buildingList.size() > 0) {
                currentBuilding = currentGroup.findByName(buildingList.get(0));
                floorList.addAll(currentBuilding.getListOfNames());
            }
            floorListAdapter.notifyDataSetChanged();
        });

        groupListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            UsersGroup group = dataController.getAccount().findByName(groupList.get(i));
            assert group != null;
            createDeleteDialog(group.getName(), null, null);
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
                floorList.clear();
                if (currentGroup == null) {
                    groupList.clear();
                    groupListAdapter.notifyDataSetChanged();
                    return;
                }

                currentBuilding = currentGroup.findByName(buildingList.get(i));
                assert currentBuilding != null;
                floorList.addAll(currentBuilding.getListOfNames());
                floorListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("Spinner", "OnNothingSelected in building");
            }
        });

        buildingSpinnerView.setOnItemLongClickListener((adapterView, view, i, l) -> {
//            assert currentGroup != null;
//            Building building = currentGroup.findByName(groupList.get(i));
//            assert building != null;
//            createDeleteDialog(currentGroup.getName(), building.getName(), null);
//
            return true;
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
                    floorList.clear();
                    floorListAdapter.notifyDataSetChanged();
                    return;
                }
                currentMap = currentBuilding.findByName(floorList.get(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("Spinner", "OnNothingSelected in floor");
//                floorList.clear();
//                floorListAdapter.notifyDataSetChanged();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnLogOut = findViewById(R.id.btnLogOut);
        Button btnAddGroup = findViewById(R.id.btnAddGroup);
        FloatingActionButton btnAddMap = findViewById(R.id.btnAddMap);

        setUpBuildingSpinnerView();
        setUpFloorSpinnerView();
        setUpGroupListView();
        setUpFindListView();
        setUpSearchView();

        btnAddGroup.setOnClickListener(groupView -> createNewGroupDialog());

        btnAddMap.setOnClickListener(v -> createChooseGroupForNewMapDialog());

        btnLogOut.setOnClickListener(v -> {
            signOut();
            Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
        });
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

    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> logIn());
    }

    private void afterAuth() {
        dataController = new DataController(getApplicationContext(), user);

        new SearchAndDownloadMapsAsyncTask(this).execute();
    }
    // [END handle_sign_in_result]

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
            synchronized (isFinished) {
                dataController.searchMaps(floorsPaths, isFinished);
            }
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

        public LoadMapsAsyncTask(MainActivity activity) {
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

        protected Void doInBackground(Void... args) { //TODO: think about void in this function
            Log.d("AsyncWork", "load async starts work");
            dataController.loadLocalFiles();
            //#1 можно сделать, чтобы эта функция возвращала массив прочитанных групп и в
            // onPostExecute его записывала.
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

            groupList.clear();
            groupList.addAll(dataController.getAccount().getListOfNames());

            buildingList.clear();
            floorList.clear();

            groupListAdapter.notifyDataSetChanged();
            buildingListAdapter.notifyDataSetChanged();
            floorListAdapter.notifyDataSetChanged();
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

        public SearchAsyncTask(MainActivity activity) {
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
            CountDownLatch latch = new CountDownLatch(1);
            dataController.getSearchedGroupsAndOwners(args[0], list, latch);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        @NonNull SearchResult arg;

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
            //TODO: args can be empty
            arg = args[0];
            synchronized (isFinished) {
                dataController.searchGroupMaps(arg.ownerId, arg.groupName,
                        floorsPaths, isFinished);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            new DownloadGroupMapsAsyncTask(activity, floorsPaths).execute(arg);
        }
    }


    @SuppressLint("StaticFieldLeak")
    private class DownloadGroupMapsAsyncTask extends AsyncTask<SearchResult, Void, Void> {
        @NonNull private ProgressDialog dialog;
        @NonNull private final AtomicInteger mapCount = new AtomicInteger(0);
        @NonNull private List<String> floorsPaths;

        public DownloadGroupMapsAsyncTask(MainActivity activity, @NonNull List<String> floorsPaths) {
            dialog = new ProgressDialog(activity);
            this.floorsPaths = floorsPaths;
        }

        @Override
        protected void onPreExecute() {
            Log.d("AsyncWork", "starts group download async task");
            dialog.setTitle("Loading group from server");
            dialog.setCancelable(false);
            dialog.setMessage("Loading...");
            dialog.setMax(floorsPaths.size());
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.show();
        }

        protected Void doInBackground(SearchResult... args) {
            //TODO args can be empty
            mapCount.set(0);
            synchronized (mapCount) {
                dataController.downloadGroup(args[0].ownerId, floorsPaths, mapCount);
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
            currentMap = toSaveMap;
            groupList.clear();
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
}
