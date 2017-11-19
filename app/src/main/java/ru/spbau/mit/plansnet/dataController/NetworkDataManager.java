package ru.spbau.mit.plansnet.dataController;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import ru.spbau.mit.plansnet.data.Account;
import ru.spbau.mit.plansnet.data.Building;
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.UsersGroup;


/**
 * Manager of network data
 */

public class NetworkDataManager {
    @NonNull
    private final FirebaseStorage storage;
    @NonNull
    private FirebaseUser userAccount;
    @NonNull
    private StorageReference storageReference;
    @NonNull
    private FirebaseDatabase database;
    @NonNull
    private DatabaseReference databaseReference;

    private static final String STORAGE_TAG = "FIREBASE_STORAGE";

    public NetworkDataManager(@NonNull final FirebaseUser currentUser) {
        userAccount = currentUser;

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
    }

    public void putMapOnServer(@NonNull final FloorMap map) {
        //put on database
        DatabaseReference userRef = databaseReference.child(userAccount.getUid());
        userRef.child("mail").setValue(userAccount.getEmail());
        userRef.child("name").setValue(userAccount.getDisplayName());
        DatabaseReference buildingsRef = userRef.child(map.getGroupName()).child("buildings");

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
                //TODO do something helpful
                Log.d(STORAGE_TAG, "uploading file was incorrect");
            }
        }); //TODO check that this is correct
    }

    /**
     * Get all tree from database and download this to the phone,
     * create an account from it
     */
    @NonNull
    public Account getAccount(final Context context) {
        final ArrayList<String> floorsPaths = new ArrayList<>();

        databaseReference.child(userAccount.getUid()).child("groups")
                .addValueEventListener(new ValueEventListener() {
                    final ArrayList<DataSnapshot> groupsRefs = new ArrayList<>();
                    final ArrayList<DataSnapshot> buildingsRefs = new ArrayList<>();

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
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
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(STORAGE_TAG, databaseError.getMessage());
                    }
                });


        final Account account = new Account(userAccount.getDisplayName());

        for (String path : floorsPaths) {
            final File mapFile = new File(context.getApplicationContext().getFilesDir(), path);
            mapFile.getParentFile().mkdirs();
            storageReference.child(path).getFile(mapFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            try (ObjectInputStream ois =
                                         new ObjectInputStream(new FileInputStream(mapFile))) {
                                FloorMap map = (FloorMap) ois.readObject();

                                UsersGroup group = account.setElementToContainer(
                                        new UsersGroup(map.getGroupName()));
                                Building building = group.setElementToContainer(
                                        new Building(map.getBuildingName()));
                                building.addData(map);
                            } catch (Exception exception) {
                                Toast.makeText(context,
                                        "Can't read map from file", Toast.LENGTH_SHORT).show();
                                exception.printStackTrace();
                            }
                        }
                    });
        }
        return account;
    }
}