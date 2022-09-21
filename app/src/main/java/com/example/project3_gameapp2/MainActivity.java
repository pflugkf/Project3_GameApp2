package com.example.project3_gameapp2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, LoginFragment.LoginFragmentListener,
        RegistrationFragment.RegistrationFragmentListener, GameLobbyFragment.GameLobbyFragmentListener, GameRoomFragment.GameRoomFragmentListener{

    private static final String TAG = "main activity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.navDrawerLayout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (user == null) {
            goToLogin();
        } else {
            goToGameLobby();
        }
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_games:
                goToGameLobby();
                break;
            case R.id.nav_logout:
                FirebaseAuth.getInstance().signOut();
                goToLogin();
                break;

        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void goToLogin() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.rootView, new LoginFragment(), "login-fragment")
                .commit();
    }

    @Override
    public void goToRegistration() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.rootView, new RegistrationFragment(), "registration-fragment")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goToGameLobby() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.rootView, new GameLobbyFragment(), "game-lobby-fragment")
                .commit();
    }

    @Override
    public void backToLogin() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void joinGame(String gameID) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.rootView, GameRoomFragment.newInstance(gameID), "new-game-fragment")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goBackToLobby(String gameID, String player1ID, String player2ID) {
        Log.d("qq", "goBackToLobby");
        getSupportFragmentManager().popBackStack();

        db.collection("games").document(gameID)
                .collection("gameStatus").document("current").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            String winName = task.getResult().getString("winner");
                            String winID = task.getResult().getString("winnerID");

                            Log.d(TAG, "winner is: " + winName + " " + winID);

                            AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                            b.setTitle("Game Over")
                                    .setMessage(winName + " Wins!")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if(winID.equals(player1ID)){
                                                Log.d(TAG, "p1 won, deleting p2");
                                                CollectionReference cr = db.collection("games").document(gameID).collection("hand-"+ player2ID);
                                                cr.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            for(QueryDocumentSnapshot doc : task.getResult()) {
                                                                cr.document(doc.getId()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        Log.d(TAG, "p2 card deleted");
                                                                    }
                                                                });
                                                            }
                                                            Log.d(TAG, "p2 deleted, deleting other records");
                                                            clearGame(gameID);
                                                        }

                                                    }
                                                });
                                            } else {
                                                Log.d(TAG, "p2 won, deleting p1");
                                                CollectionReference cr = db.collection("games").document(gameID).collection("hand-"+ player1ID);
                                                cr.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            for(QueryDocumentSnapshot doc : task.getResult()) {
                                                                cr.document(doc.getId()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        Log.d(TAG, "p1 card deleted");
                                                                    }
                                                                });
                                                            }
                                                            Log.d(TAG, "p1 deleted, deleting other records");
                                                            clearGame(gameID);
                                                        }

                                                    }
                                                });
                                            }

                                        }
                                    });
                            b.create().show();
                        }
                    }
                });
    }

    private void clearGame(String gameID){
        DocumentReference gameRef = db.collection("games").document(gameID);
        if(gameRef.get() != null) {
            gameRef.collection("gameStatus").document("current").delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Log.d(TAG, "gameStatus deleted");
                    gameRef.collection("topCard").document("current").delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "topCard deleted");
                            gameRef.collection("turn").document("current").delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d(TAG, "topCard deleted");
                                    gameRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Log.d(TAG, "Game deleted");
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
    }
}