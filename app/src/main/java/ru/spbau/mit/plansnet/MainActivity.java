
package ru.spbau.mit.plansnet;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;

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
                        dataController = new DataController(getApplicationContext(), user,
                                adapter, myMaps);

                        final ProgressDialog downloadingDialog = new ProgressDialog(MainActivity.this);
                        downloadingDialog.setTitle("Loading maps from server");
                        downloadingDialog.setCancelable(false);
                        downloadingDialog.setMessage("Loading...");
                        downloadingDialog.setMax(1);
                        downloadingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        downloadingDialog.show();
                        dataController.downloadMaps(downloadingDialog, MainActivity.this);
                    }
                });
    }
    // [END handle_sign_in_result]

    public void onServerDataLoaded() {
        ProgressDialog loadingDialog = new ProgressDialog(MainActivity.this);
        loadingDialog.setTitle("Loading maps from storage");
        loadingDialog.setCancelable(false);
        loadingDialog.setMessage("Loading...");
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        runOnUiThread(() -> {
            loadingDialog.show();
            dataController.loadLocalFiles();
            loadingDialog.dismiss();
        });
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
