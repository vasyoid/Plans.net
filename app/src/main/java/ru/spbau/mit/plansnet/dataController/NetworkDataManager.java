package ru.spbau.mit.plansnet.dataController;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import ru.spbau.mit.plansnet.data.Account;
import ru.spbau.mit.plansnet.data.FloorMap;


/**
 * Manager of network data
 */

public class NetworkDataManager {

    @NonNull
    private final ArrayAdapter adapter;
    @NonNull
    private final List<FloorMap> listOfMaps;
    @NonNull
    private final FirebaseStorage storage;
    @NonNull
    private final Context context;
    @NonNull
    private final DataController dataController;
    @NonNull
    private FirebaseUser userAccount;
    @NonNull
    private StorageReference storageReference;
    @NonNull
    private FirebaseDatabase database;
    @NonNull
    private DatabaseReference databaseReference;

    private boolean inProcess = false;

    private static final String STORAGE_TAG = "FIREBASE_STORAGE";

    public NetworkDataManager(@NonNull final DataController dataController,
                              @NonNull final Context context,
                              @NonNull final FirebaseUser currentUser,
                              @NonNull final ArrayAdapter adapter,
                              @NonNull final List<FloorMap> listOfMaps) {
        this.dataController = dataController;
        this.context = context;
        userAccount = currentUser;

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        this.adapter = adapter;
        this.listOfMaps = listOfMaps;
    }

    public void putMapOnServer(@NonNull final FloorMap map) {
//        Log.d(STORAGE_TAG, "in put map func");
//        while (inProcess) {
//            SystemClock.sleep(100);
//        };
//        Log.d(STORAGE_TAG, "start put map func");
//        inProcess = true;
        //put on database
        DatabaseReference userRef = databaseReference.child(userAccount.getUid());
        userRef.child("mail").setValue(userAccount.getEmail());
        userRef.child("name").setValue(userAccount.getDisplayName());
        DatabaseReference buildingsRef = userRef.child("groups")
                .child(map.getGroupName()).child("buildings");

        buildingsRef.child("isPublic").setValue(true);
        DatabaseReference floorsRef = buildingsRef.child(map.getBuildingName())
                .child("floors")//need to add some order in future
                .child(map.getName());

        String pathInStorage = userAccount.getUid() + "/"
                + map.getGroupName() + "/"
                + map.getBuildingName() + "/"
                + map.getName() + ".plannet";
        floorsRef.child("path").setValue(pathInStorage);

        //put on storage
        StorageReference storageMapRef = storageReference.child(pathInStorage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream ous = new ObjectOutputStream(baos)) {
            ous.writeObject(map);
        } catch (IOException e) {
            Log.d(STORAGE_TAG, "writing to byte array was incorrect");
            e.printStackTrace();
        }

        storageMapRef.putBytes(baos.toByteArray()).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Fail while map uploading", Toast.LENGTH_SHORT).show();
                Log.d(STORAGE_TAG, "uploading was incorrect");
//                inProcess = false;
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(context, "Uploading map successful", Toast.LENGTH_SHORT).show();
                Log.d(STORAGE_TAG, "uploading success");
//                inProcess = false;
            }
        });

    }

    /**
     * Get all tree from database and download this to the phone,
     * create an account from it
     */
    @NonNull
    public void initAccount() {
//        Log.d(STORAGE_TAG, "in get account func");
//        while (inProcess) {
//            SystemClock.sleep(100);
//        };
//        Log.d(STORAGE_TAG, "start get account");
//        inProcess = true;
        final ArrayList<String> floorsPaths = new ArrayList<>();

        databaseReference.child(userAccount.getUid()).child("groups")
                .addValueEventListener(new ValueEventListener() {
                    final ArrayList<DataSnapshot> groupsRefs = new ArrayList<>();
                    final ArrayList<DataSnapshot> buildingsRefs = new ArrayList<>();

                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            groupsRefs.add(data);
                        }
                        for (DataSnapshot group : groupsRefs) {
                            for (DataSnapshot data : group.child("buildings").getChildren()) {
                                buildingsRefs.add(data);
                            }
                        }
                        for (DataSnapshot building : buildingsRefs) {
                            for (DataSnapshot data : building.child("floors").getChildren()) {
                                floorsPaths.add((String) data.child("path").getValue());
                            }
                        }
                        for (final String path : floorsPaths) {
                            storageReference.child(path).getMetadata().addOnCompleteListener(
                                    new OnCompleteListener<StorageMetadata>() {
                                        @Override
                                        public void onComplete(@NonNull Task<StorageMetadata> task) {

                                            final File mapFile = new File(context.getApplicationContext()
                                                    .getFilesDir(), path);

                                            if (mapFile.exists() && mapFile.lastModified() > task.getResult().getUpdatedTimeMillis()) {
                                                Log.d(STORAGE_TAG,
                                                        task.getResult().getName() + " is up to date");
                                                return;
                                            }

                                            mapFile.getParentFile().mkdirs();
                                            storageReference.child(path).getFile(mapFile)
                                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask
                                                            .TaskSnapshot>() {
                                                        @Override
                                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                                            dataController.readMapFromFile(mapFile);
                                                        }
                                                    });
                                        }
                                    });
                        }
//                        inProcess = false;
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(STORAGE_TAG, databaseError.getMessage());
//                        inProcess = false;
                    }


                });
    }

}