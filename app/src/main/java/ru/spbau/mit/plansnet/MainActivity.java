
package ru.spbau.mit.plansnet;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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

import ru.spbau.mit.plansnet.constructor.ConstructorActivity;
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

    private Button btnLogOut;
    private Button btnSettings;

    private FloatingActionButton btnAddMap;

    private FloorMap toOpenMap;
    private TextView txtNameOfSlectedMap;

    private GoogleSignInClient mGoogleSignOutClient;

    private ArrayList<FloorMap> myMaps = new ArrayList<>();
    private ArrayAdapter adapter;

    private int mapCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLogOut = findViewById(R.id.btnLogOut);
        btnSettings = findViewById(R.id.btnSettings);
        txtNameOfSlectedMap = findViewById(R.id.nameOfSlectedMap);
        btnAddMap = findViewById(R.id.btnAddMap);
        btnLogOut.setOnClickListener(v -> mGoogleSignOutClient.signOut()
                .addOnCompleteListener(MainActivity.this, task ->
                        Toast.makeText(MainActivity.this, "You logged out.",
                                Toast.LENGTH_SHORT).show()));

        btnAddMap.setOnClickListener(v -> {
            AlertDialog dialogNameOfNewMap = new AlertDialog.Builder(MainActivity.this).create();
            dialogNameOfNewMap.setTitle("Give me new name of Map");

            final EditText input = new EditText(MainActivity.this);
            dialogNameOfNewMap.setView(input);

            dialogNameOfNewMap.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog, which) -> {
                final String nameOfNewMap = input.getText().toString();

                dataController.addBuildingToGroup(new Building("default"),
                        dataController.addGroup(new UsersGroup("default")));
                dataController.saveMap(new FloorMap(nameOfNewMap,
                        "default", "default"));
            });


            dialogNameOfNewMap.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> {

            });
            dialogNameOfNewMap.show();
        });

        ListView listOfMaps = findViewById(R.id.listOfMaps);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, myMaps);
        listOfMaps.setAdapter(adapter);

        listOfMaps.setOnItemClickListener((parent, view, position, id) -> {
            toOpenMap = myMaps.get(position);
            txtNameOfSlectedMap.setText("Current map: " + toOpenMap.getName());
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

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

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
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(LOG_IN_TAG, "signInWithCredential:success");
                        Log.d("MYTEST", "firebase auth");

                        user = auth.getCurrentUser();
                        dataController = new DataController(getApplicationContext(), user, myMaps);

                        new SearchAndDownloadMapsAsyncTask(this).execute();
                    }
                });
    }
    // [END handle_sign_in_result]

    public void onServerDataLoaded() {

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
            adapter.notifyDataSetChanged();


            SearchAsyncTask st = new SearchAsyncTask(MainActivity.this);
            st.execute("aul");

        }
    }

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
        private List<SearchResult> list; // list with <OwnerId, OwnerName, Group>. It is keys in Database

        public SearchAsyncTask(MainActivity activity) {
            dialog = new ProgressDialog(activity);
            list = new ArrayList<>();
        }

        @Override
        protected void onPreExecute() {
            Log.d("AsyncWork", "starts search async task");
        }

        protected Void doInBackground(String... args) {
            if (args.length < 1) {
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
            for (SearchResult x : list) {
//                dataController.addGroupByRef(x.ownerId, x.ownerName); // download found groups
                Log.d("AsyncWork!!", "in list: " + x.ownerName + " --> " + x.groupName);
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GET_TOKEN) {
            // [START get_id_token]
            // This task is always completed immediately, there is no need to attach an
            // asynchronous listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
            // [END get_id_token]
        } else if (requestCode == CONSTRUCTOR_TOKEN) {
            if (data != null) {
                FloorMap toSaveMap = (FloorMap) data.getSerializableExtra("toSaveMap");
//                Log.d("VASYOID", toOpenMap.getGroupName());
//                Log.d("VASYOID", toOpenMap.getName());
                if (toSaveMap != null) {
                    dataController.saveMap(toSaveMap);
                    adapter.notifyDataSetChanged();
                }
                toOpenMap = toSaveMap;
                myMaps.clear();
            } else {
                Toast.makeText(this, "Nothing to save", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void openConstructor(View v) {
        Intent intent = new Intent(MainActivity.this,
                ConstructorActivity.class);
        if (toOpenMap != null) {
            intent.putExtra("toOpenMap", toOpenMap);
        }
        startActivityForResult(intent, CONSTRUCTOR_TOKEN);
    }
}
