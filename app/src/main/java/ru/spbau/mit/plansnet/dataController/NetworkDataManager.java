package ru.spbau.mit.plansnet.dataController;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import ru.spbau.mit.plansnet.MainActivity;
import ru.spbau.mit.plansnet.MainActivity.SearchResult;
import ru.spbau.mit.plansnet.data.Building;
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.UsersGroup;

/**
 * Manager of network data
 */

public class NetworkDataManager {
    @NonNull
    private Context context;
    @NonNull
    private FirebaseUser userAccount;
    @NonNull
    private StorageReference storageReference;
    @NonNull
    private DatabaseReference databaseReference;

    private static final String STORAGE_TAG = "FIREBASE_STORAGE";

    public NetworkDataManager(@NonNull final Context context,
                              @NonNull final FirebaseUser currentUser) {
        this.context = context;
        userAccount = currentUser;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
    }

    public void putMapOnServer(@NonNull final FloorMap map) {
        //put on database
        DatabaseReference userRef = databaseReference.child(userAccount.getUid());
        userRef.child("mail").setValue(userAccount.getEmail());
        userRef.child("name").setValue(userAccount.getDisplayName());
        DatabaseReference buildingsRef = userRef.child("groups")
                .child(map.getGroupName()).child("buildings");

        buildingsRef.child("isPublic").setValue(true);
        DatabaseReference floorsRef = buildingsRef
                .child(map.getBuildingName())
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

        storageMapRef.putBytes(baos.toByteArray()).addOnFailureListener(e -> {
            Toast.makeText(context, "Fail while map uploading", Toast.LENGTH_SHORT).show();
            Log.d(STORAGE_TAG, "uploading was incorrect");
        }).addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(context, "Uploading map successful", Toast.LENGTH_SHORT).show();
            Log.d(STORAGE_TAG, "uploading success");
        });

    }

    /**
     * Get all tree from database and download this to the phone,
     * create an account from it
     */
    public void downloadMaps(@NonNull final ProgressDialog progressDialog) {
        final ArrayList<String> floorsPaths = new ArrayList<>();

        databaseReference
                .child(userAccount.getUid())
                .child("groups")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        for (DataSnapshot group : dataSnapshot.getChildren()) {
                            for (DataSnapshot building : group.child("buildings").getChildren()) {
                                for (DataSnapshot floor : building.child("floors").getChildren()) {
                                    floorsPaths.add((String) floor.child("path").getValue());
                                }
                            }
                        }

                        progressDialog.setMax(floorsPaths.size());
                        Log.d(STORAGE_TAG, "progress dialog size updated: " + progressDialog.getMax());

                        downloadByPaths(floorsPaths, progressDialog);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(STORAGE_TAG, databaseError.getMessage());
                        progressDialog.setMax(0);
                        progressDialog.cancel();
                    }


                });
    }

    private void downloadByPaths(List<String> floorsPaths, ProgressDialog progressDialog) {
        for (final String path : floorsPaths) {
            storageReference.child(path).getMetadata().addOnCompleteListener(
                    task -> {
                        Log.d(STORAGE_TAG, "downloading file: " + path);
                        final File mapFile = new File(context.getApplicationContext()
                                .getFilesDir(), path);

                        if (mapFile.exists() && mapFile.lastModified() > task.getResult().getUpdatedTimeMillis()) {
                            Log.d(STORAGE_TAG,
                                    task.getResult().getName() + " is up to date");
                            progressDialog.incrementProgressBy(1);

                            return;
                        }

                        if (mapFile.getParentFile().mkdirs()) {
                            Log.d(STORAGE_TAG, "mkdirs returned true");
                        } else {
                            Log.d(STORAGE_TAG, "mkdirs returned false");
                        }
                        storageReference.child(path).getFile(mapFile)
                                .addOnSuccessListener(taskSnapshot -> {
                                    progressDialog.incrementProgressBy(1);
                                    Log.d("LOAD", progressDialog.getProgress() + " : " + progressDialog.getMax());
                                    if (progressDialog.getProgress() == floorsPaths.size()) {
                                        progressDialog.dismiss();
                                    }
                                    Log.d(STORAGE_TAG, progressDialog.getProgress() + " get file from storage: " + mapFile.getName());
                                }).addOnFailureListener(e -> {
                            progressDialog.incrementProgressBy(1);
                            Toast.makeText(context, "Can't download map: " + mapFile.getName(), Toast.LENGTH_SHORT).show();
                        });
                    });
        }
    }

    public void deleteReference(@Nullable final String groupName,
                                @Nullable final String buildingName,
                                @Nullable final String mapName) {
        DatabaseReference ref = databaseReference.child(userAccount.getUid());
        if (groupName != null) {
            ref = ref.child("groups")
                    .child(groupName);
            if (buildingName != null) {
                ref = ref.child("buildings")
                        .child(buildingName);
                if (mapName != null) {
                    ref = ref.child("floors")
                            .child(mapName);
                }
            }
        } else {
            return;
        }
        ref.removeValue();
        //TODO remove from storage
    }

    public void getGroupsWhichContainsName(@NonNull final String name,
                                           @NonNull final List<SearchResult> ownersAndGroups,
                                           @NonNull CountDownLatch latch) {
        final String searchedName = name.toLowerCase();
        Log.d("SUPER!", "search " + searchedName);
        databaseReference
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d("SUPER!", "i'm here");
                        for (DataSnapshot user : dataSnapshot.getChildren()) {
                            for (DataSnapshot group : user.child("groups").getChildren()) {
                                Log.d("SUPER!", "in... " + group.getKey());
                                if (group.getKey().toLowerCase().contains(searchedName)) {
                                    Log.d("SUPER!", "op! " + group.getKey());
                                    ownersAndGroups.add(new SearchResult(user.getKey(),
                                            (String) user.child("name").getValue(),
                                            group.getKey()));
                                }
                            }
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        latch.countDown();
                    }
                });
    }

    public void downloadGroup(@NonNull final String owner, @NonNull final String group,
                              @NonNull final ProgressDialog progressDialog) {
        final UsersGroup userGroup = new UsersGroup(group + "_" + owner);
        databaseReference
                .child(owner)
                .child("groups")
                .child(group)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final ArrayList<String> floorsPaths = new ArrayList<>();

                        for (DataSnapshot buildingShot : dataSnapshot.child("buildings").getChildren()) {
                            Building building = new Building(buildingShot.getKey());
                            userGroup.addData(building);
                            for (DataSnapshot floorShot : buildingShot.child("floors").getChildren()) {
                                FloorMap map = new FloorMap(group, building.getName(), floorShot.getKey());
                                building.addData(map);
                                floorsPaths.add((String) floorShot.child("path").getValue());
                            }
                        }

                        downloadByPaths(floorsPaths, progressDialog);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

//    public void renameGroup(@NonNull final String groupName, @NonNull final String newName) {
////        databaseReference.child(userAccount.getUid())
//    }
}