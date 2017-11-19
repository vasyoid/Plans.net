package ru.spbau.mit.plansnet.dataController;

import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import ru.spbau.mit.plansnet.data.FloorMap;


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

    /*
    /TODO LIST:
    * saveMap();
    * loadMap();
    * getListOfAllMaps();
    * checkLastMapModifying();
    * deleteAnything();
    * checkAnything();
    * checkExisting();
     */

    public NetworkDataManager(@NonNull final FirebaseUser currentUser) {
        userAccount = currentUser;

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
    }

    public void putMapOnServer(@NonNull final FloorMap map) {
        DatabaseReference userRef = databaseReference.child(userAccount.getUid());
        userRef.child("mail").setValue(userAccount.getEmail());
        userRef.child("name").setValue(userAccount.getDisplayName());
        DatabaseReference buildingsRef = userRef.child(map.getGroupName()).child("buildings");

        buildingsRef.child("isPublic").setValue(true);
        DatabaseReference floorsRef = buildingsRef.child(map.getBuildingName())
                .child("floors")//need to add some order in future
                .child(map.getName());

        String pathInStorage = "somePath";//TODO doing something with this stub
        floorsRef.child("path").setValue("somePath TODO");

        //TODO put on stoarage
    }

}
//    //TODO delete this
//    public void testSome() {
////        databaseRef = database.getReference("ads");
////        databaseRef.setValue("something");
//    }
//}
//
//    FirebaseUser currentUser = mAuth.getCurrentUser();
//        Log.d("MYTEST", "doSome");
//
//                if (currentUser != null) {
//                Log.d("MYTEST", currentUser.getDisplayName());
//                } else {
//                Log.d("MYTEST", "can't get name2");
//                }
//
//                FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
//
//
//
////        DatabaseReference dRef = firebaseDatabase.getReference(currentUser.getUid());
////        DatabaseReference dRef = firebaseDatabase.getReference();
////
////        DatabaseReference m = dRef.child("users").child("asdiasjodas");
////        m.child("name").setValue("David");
////        m.child("age").setValue("more than 21");
////        m.child("boobs").setValue("2");
////
////        m = dRef.child("urers").child("asdkasfksa");
////        m.child("name").setValue("Not David");
////        m.child("age").setValue("less than 21");
////        m.child("boobs").setValue("-1");
////        m.removeValue();
////
//                //================ Storage ================
//
//                FirebaseStorage fbStorage = FirebaseStorage.getInstance();
//
////        dRef.setValue(currentUser.getEmail());
//                StorageReference hwRef = fbStorage.getReference().child("fold1/hw.tex");
//
//                Context context = new ContextWrapper(this);
//final File filesDir = new File(getApplicationContext().getFilesDir().getAbsolutePath());
//        File localFile  = new File(filesDir.getAbsolutePath(), "hw2.tex");
////        localFile
////        try {
//        Toast.makeText(IdTokenActivity.this, Boolean.toString(localFile.mkdirs()), Toast.LENGTH_SHORT).show();
//        Toast.makeText(IdTokenActivity.this, Boolean.toString(localFile.exists()), Toast.LENGTH_SHORT).show();
//        Toast.makeText(IdTokenActivity.this, Boolean.toString(localFile.setWritable(true)), Toast.LENGTH_SHORT).show();
//
////        } catch (IOException e) {
////            e.printStackTrace();
////            Toast.makeText(IdTokenActivity.this, "error1", Toast.LENGTH_SHORT).show();
////        }
//
//        hwRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//@Override
//public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
////                taskSnapshot.
//        Toast.makeText(IdTokenActivity.this, "complete", Toast.LENGTH_SHORT).show();
//        Toast.makeText(IdTokenActivity.this, filesDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
////                Toast.makeText(IdTokenActivity.this, filesDir.listFiles()[0].getName(), Toast.LENGTH_SHORT).show();
//        }
//        }).addOnFailureListener(new OnFailureListener() {
//@Override
//public void onFailure(@NonNull Exception exception) {
//        Toast.makeText(IdTokenActivity.this, "error2", Toast.LENGTH_SHORT).show();
//        // Handle any errors
//        }
//        });

