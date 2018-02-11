package ru.spbau.mit.plansnet.dataController;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ru.spbau.mit.plansnet.MainActivity.SearchResult;
import ru.spbau.mit.plansnet.data.Building;
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.UsersGroup;

/**
 * Manager of network data
 */

class NetworkDataManager {
    @NonNull
    private Context context;
    @NonNull
    private FirebaseUser userAccount;
    @NonNull
    private StorageReference storageReference;
    @NonNull
    private DatabaseReference databaseReference;

    private static final String STORAGE_TAG = "FIREBASE_STORAGE";

    NetworkDataManager(@NonNull final Context context,
                              @NonNull final FirebaseUser currentUser) {
        this.context = context;
        userAccount = currentUser;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
    }

    void putMapOnServer(@NonNull final FloorMap map) {
        //put on database
        DatabaseReference userRef = databaseReference.child(userAccount.getUid());
        userRef.child("mail").setValue(userAccount.getEmail());
        userRef.child("name").setValue(userAccount.getDisplayName());
        DatabaseReference groupRef = userRef.child("groups").child(map.getGroupName());

        groupRef.child("isPublic").setValue(true);

        DatabaseReference buildingsRef = groupRef.child("buildings");

        DatabaseReference floorsRef = buildingsRef
                .child(map.getBuildingName())
                .child("floors")//need to add some order in future
                .child(map.getName());

        String pathInStorage = "/" + userAccount.getUid() + "/"
                + map.getGroupName() + "/"
                + map.getBuildingName() + "/"
                + map.getName() + ".plannet";
        floorsRef.child("path").setValue(pathInStorage);

        Log.d("Storage upload", pathInStorage);

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
     * Searches map on server and add it to list of paths
     */
    void searchMaps(@NonNull final List<String> floorsPaths,
                    @NonNull final AtomicBoolean isFinished) {
        Log.d("SearchMaps", "Start Searching in network manager");
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
                                    Log.d("SearchMaps", "found " + floor.getKey());
                                }
                            }
                        }
                        synchronized (isFinished) {
                            isFinished.set(true);
                            isFinished.notify();
                            Log.d("SearchMaps", "finish!");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("Search Maps", databaseError.getMessage());
                        synchronized (isFinished) {
                            isFinished.set(true);
                            isFinished.notify();
                        }
                    }
                });
    }

    void downloadByPaths(@NonNull final List<String> floorsPaths, AtomicInteger mapCount, String owner) {
        for (final String path : floorsPaths) {
            storageReference.child(path).getMetadata().addOnCompleteListener(
                    task -> {
                        String newPath = path.replace(owner, userAccount.getUid());

                        Log.d(STORAGE_TAG, "downloading file: " + path);
                        final File mapFile = new File(context.getApplicationContext()
                                .getFilesDir(), newPath);

                        if (mapFile.exists() && mapFile.lastModified() > task.getResult().getUpdatedTimeMillis()) {
                            Log.d(STORAGE_TAG,task.getResult().getName() + " is up to date");
                            synchronized (mapCount) {
                                mapCount.incrementAndGet();
                                mapCount.notify();
                            }
                            return;
                        }

                        if (mapFile.getParentFile().mkdirs()) {
                            Log.d(STORAGE_TAG, "mkdirs returned true");
                        } else {
                            Log.d(STORAGE_TAG, "mkdirs returned false");
                        }

                        storageReference.child(path).getFile(mapFile)
                                .addOnSuccessListener(taskSnapshot -> {
                                    synchronized (mapCount) {
                                        mapCount.incrementAndGet();
                                        mapCount.notify();
                                        Log.d(STORAGE_TAG, mapCount.get()
                                                + " get file from storage: " + mapFile.getName());
                                    }
                                }).addOnFailureListener(e -> {
                                    synchronized (mapCount) {
                                        mapCount.incrementAndGet();
                                        mapCount.notify();
                                        Toast.makeText(context, "Can't download map: "
                                                + mapFile.getName(), Toast.LENGTH_SHORT).show();
                                    }
                        });
                    });
        }
    }

    void deleteReference(@Nullable final String groupName,
                                @Nullable final String buildingName,
                                @Nullable final String mapName) {
        DatabaseReference ref = databaseReference.child(userAccount.getUid());
        StorageReference storageRef = storageReference.child(userAccount.getUid());
        if (groupName != null) {
            storageRef = storageRef.child(groupName);
            ref = ref.child("groups")
                    .child(groupName);
            if (buildingName != null) {
                storageRef = storageRef.child(buildingName);
                ref = ref.child("buildings")
                        .child(buildingName);
                if (mapName != null) {
                    storageRef = storageRef.child(mapName);
                    ref = ref.child("floors")
                            .child(mapName);
                }
            }
        } else {
            return;
        }

        if (mapName != null) {
            ref.removeValue();
            storageRef.delete();
            return;
        }
        final StorageReference deleteInStorage = storageRef;
        final DatabaseReference deleteInDatabase = ref;
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> floorsPaths = new ArrayList<>();
                if (buildingName != null) {
                    Log.d("delete storage", "delete building");
                    for (DataSnapshot floor : dataSnapshot.child("floors").getChildren()) {
                        floorsPaths.add((String)floor.child("path").getValue());
                    }
                } else {
                    Log.d("delete storage", "delete group");
                    for (DataSnapshot building : dataSnapshot.child("buildings").getChildren()) {
                        for (DataSnapshot floor : building.child("floors").getChildren()) {
                            floorsPaths.add((String)floor.child("path").getValue());
                            Log.d("delete storage", "added path:" + floorsPaths.get(floorsPaths.size() - 1));
                        }
                    }
                }
                for (String path : floorsPaths) {
                    Log.d("storage delete", "delete: " + path);
                    storageReference.child(path).delete();
                }
                deleteInDatabase.removeValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("storage delete", "OOPS: " + databaseError.getMessage());
            }
        });
    }

    void getGroupsWhichContainsName(@NonNull final String name,
                                           @NonNull final List<SearchResult> ownersAndGroups,
                                           @NonNull CountDownLatch latch) {
        final String searchedName = name.toLowerCase();
        databaseReference
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot user : dataSnapshot.getChildren()) {
                            for (DataSnapshot group : user.child("groups").getChildren()) {
                                if (group.getKey().toLowerCase().contains(searchedName)) {
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

    void searchGroupMaps(@NonNull final String owner, @NonNull final String group,
                         @NonNull final List<String> floorsPaths,
                         @NonNull final AtomicBoolean isFinished) {
        final UsersGroup userGroup = new UsersGroup(group + "_" + owner);
        databaseReference
                .child(owner)
                .child("groups")
                .child(group)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot buildingShot : dataSnapshot.child("buildings").getChildren()) {
                            Building building = new Building(buildingShot.getKey());
                            userGroup.addData(building);
                            for (DataSnapshot floorShot : buildingShot.child("floors").getChildren()) {
                                FloorMap map = new FloorMap(group, building.getName(), floorShot.getKey());
                                building.addData(map);
                                String path = (String) floorShot.child("path").getValue();
                                floorsPaths.add(path);
                            }
                        }
                        synchronized (isFinished) {
                            isFinished.set(true);
                            isFinished.notify();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("SearchGroups", databaseError.getMessage());
                        synchronized (isFinished) {
                            isFinished.set(true);
                            isFinished.notify();
                        }
                    }
                });
    }


}