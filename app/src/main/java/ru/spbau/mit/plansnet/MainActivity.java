
package ru.spbau.mit.plansnet;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.dataController.DataController;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_IN_TAG = "LogIn";
    private static final int RC_GET_TOKEN = 9002;

    private DataController dataController;
    private FirebaseUser user;

    private Button btnLogOut;
    private Button btnSettings;

    private FloatingActionButton btnAddMap;

    private FloorMap toOpenMap;
    private TextView txtNameOfSlectedMap;

    private GoogleSignInClient mGoogleSignOutClient;

    private ArrayList<FloorMap> myMaps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLogOut = findViewById(R.id.btnLogOut);
        btnSettings = findViewById(R.id.btnSettings);
        txtNameOfSlectedMap = findViewById(R.id.nameOfSlectedMap);
        btnAddMap = findViewById(R.id.btnAddMap);

        OnClickListener oclBtnLogOut = new OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnLogOut:
                        signOut();
                        break;
                }
            }

            private void signOut() {
                mGoogleSignOutClient.signOut()
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(MainActivity.this, "You logged out.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        };


        btnAddMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialogNameOfNewMap = new AlertDialog.Builder(MainActivity.this).create();
                dialogNameOfNewMap.setTitle("Give me new name of Map");

                final EditText input = new EditText(MainActivity.this);
                dialogNameOfNewMap.setView(input);

                dialogNameOfNewMap.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String nameOfNewMap = input.toString();
                                //TODO add new map
                            }
                        });


                dialogNameOfNewMap.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialogNameOfNewMap.show();


                //TODO create map
            }
        });

        ListView listOfMaps = findViewById(R.id.listOfMaps);
        dataController.saveMap(new FloorMap("map1", "building1", "group1"));
        dataController.saveMap(new FloorMap("map2", "building1", "group1"));
        dataController.saveMap(new FloorMap("map3", "building1", "group2"));
        dataController.saveMap(new FloorMap("map4", "building2", "group2"));
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, myMaps);
        listOfMaps.setAdapter(adapter);

        listOfMaps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                toOpenMap = myMaps.get(position);
                txtNameOfSlectedMap.setText("Current map: " + toOpenMap.getName());
                //TODO update UI
            }
        });

        btnLogOut.setOnClickListener(oclBtnLogOut);

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
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(LOG_IN_TAG, "signInWithCredential:success");
                            Log.d("MYTEST", "firebase auth");

                            user = auth.getCurrentUser();
                            dataController = new DataController(getApplicationContext(), user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(LOG_IN_TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }
    // [END handle_sign_in_result]

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
        }
    }

    public void openConstructor(View v) {
        Intent intent = new Intent(MainActivity.this,
                ConstructorActivity.class);
        startActivity(intent);
    }
}
