package ru.spbau.mit.plansnet.dataController;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
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

    void putMapOnServer(@NonNull FloorMap map) {
        //put on database
        DatabaseReference userRef = databaseReference.child(userAccount.getUid());
        userRef.child("mail").setValue(userAccount.getEmail());
        userRef.child("name").setValue(userAccount.getDisplayName());

        DatabaseReference groupRef;

        if (map.getOwner().equals(userAccount.getUid())) {
            groupRef = userRef.child("groups").child(map.getGroupName());

            groupRef.child("isPrivate").setValue(false);
        } else {
            groupRef = userRef.child("downloads").child(map.getGroupName());

            map = new FloorMap(map);
            map.setPath(map.getGroupName().replace(map.getOwner() + "_", ""),
                    map.getBuildingName());
        }

        DatabaseReference buildingsRef = groupRef.child("buildings");

        DatabaseReference floorsRef = buildingsRef
                .child(map.getBuildingName())
                .child("floors")
                .child(map.getName());

        String pathInStorage = "/" + map.getOwner() + "/"
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
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        DataSnapshot groups = dataSnapshot.child("groups");
                        addToFloorsPaths(groups, floorsPaths);

                        groups = dataSnapshot.child("downloads");
                        addToFloorsPaths(groups, floorsPaths);

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

    private void addToFloorsPaths(DataSnapshot groups, List<String> floorsPaths) {
        for (DataSnapshot group : groups.getChildren()) {
            for (DataSnapshot building : group.child("buildings").getChildren()) {
                for (DataSnapshot floor : building.child("floors").getChildren()) {
                    floorsPaths.add((String) floor.child("path").getValue());
                    Log.d("SearchMaps", "found " + floor.getKey());
                }
            }
        }
    }

    void downloadByPaths(@NonNull final List<String> floorsPaths, AtomicInteger mapCount) {
        for (final String path : floorsPaths) {
            storageReference.child(path).getMetadata().addOnCompleteListener(
                    task -> {
                        if (!task.isSuccessful()) {
                            synchronized (mapCount) {
                                mapCount.incrementAndGet();
                                mapCount.notify();
                            }
                            return;
                        }
                        String newPath = path;
                        String owner = path.substring(1, path.substring(1).indexOf('/') + 1);
                        Log.d(STORAGE_TAG, "Owner is " + owner);

                        if (!owner.equals(userAccount.getUid())) {
                            newPath = path.replace(owner + "/",
                                    userAccount.getUid() + "/" + owner + "_");
                            memoriseDownloadedMap(newPath, path);
                            Log.d("Path replace", path + " -> " + newPath);
                        }

                        Log.d(STORAGE_TAG, "downloading file: " + newPath);

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

    private void memoriseDownloadedMap(String newPath, String path) {
        DatabaseReference downloads = databaseReference
                .child(userAccount.getUid())
                .child("downloads");
        StringTokenizer tokenizer = new StringTokenizer(newPath, "/", false);
        tokenizer.nextToken();

        String newGroupName = tokenizer.nextToken();
        String buildingName = tokenizer.nextToken();
        String floorName = tokenizer.nextToken();
        floorName = floorName.subSequence(0, floorName.lastIndexOf(".")).toString();

        Log.d("Memoriser", newGroupName + "; " + buildingName + "; " + floorName);

        downloads.child(newGroupName)
                .child("buildings")
                .child(buildingName)
                .child("floors")
                .child(floorName)
                .child("path")
                .setValue(path);
    }

    void deleteReference(@NonNull final UsersGroup group,
                                @Nullable final Building building,
                                @Nullable final FloorMap map) {
        DatabaseReference ref = databaseReference.child(userAccount.getUid());
        StorageReference storageRef = storageReference.child(userAccount.getUid());
        storageRef = storageRef.child(group.getName());
        if (group.getName().equals(group.toString())) {
            ref = ref.child("groups");
        } else {
            ref = ref.child("downloads");
        }
        ref = ref.child(group.getName());
        if (building != null) {
            storageRef = storageRef.child(building.getName());
            ref = ref.child("buildings")
                    .child(building.getName());
            if (map != null) {
                storageRef = storageRef.child(map.getName() + ".plannet");
                ref = ref.child("floors")
                        .child(map.getName());
            }
        }

        if (!group.getName().equals(group.toString())) {
            ref.removeValue();
            return;
        }

        if (map != null) {
            ref.removeValue();
            storageRef.delete();
            return;
        }

        final DatabaseReference deleteInDatabase = ref;
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> floorsPaths = new ArrayList<>();
                if (building != null) {
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
                                if (!((boolean) group.child("isPrivate").getValue())
                                     && group.getKey().toLowerCase().contains(searchedName)) {
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
        databaseReference
                .child(owner)
                .child("groups")
                .child(group)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot buildingShot : dataSnapshot.child("buildings").getChildren()) {
                            for (DataSnapshot floorShot : buildingShot.child("floors").getChildren()) {
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

    void setIsPrivate(@NonNull UsersGroup group, boolean isPrivate) {
        databaseReference.child(userAccount.getUid())
                .child("groups")
                .child(group.getName())
                .child("isPrivate")
                .setValue(isPrivate);
    }

}