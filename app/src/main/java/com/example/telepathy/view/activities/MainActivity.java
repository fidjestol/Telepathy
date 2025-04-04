package com.example.telepathy.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.telepathy.R;
import com.example.telepathy.controller.FirebaseController;
import com.example.telepathy.model.Lobby;
import com.example.telepathy.model.Player;
import com.example.telepathy.utils.PreferenceManager;
import com.example.telepathy.view.fragments.CreateLobbyFragment;
import com.example.telepathy.view.fragments.JoinLobbyFragment;
import com.example.telepathy.view.fragments.MenuFragment;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private FirebaseController firebaseController;
    private PreferenceManager preferenceManager;
    private Player currentPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase controller
        firebaseController = FirebaseController.getInstance();

        // Test writing to database, message should be displayed in Firebase console
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child("test").setValue("Firebase fungerer!")
                .addOnSuccessListener(aVoid -> Log.d("FirebaseTest", "Data lagt til"))
                        .addOnFailureListener(e -> Log.e("FirebaseTest", "Feil: ", e));

        // Test Firebase connection
        testFirebaseConnection();

        // Initialize Firebase controller
        preferenceManager = new PreferenceManager(this);

        // Check if user is logged in
        /*if (!preferenceManager.isLoggedIn()) {
            navigateToLoginActivity();
            return;
        }*/

        // Temporary: Create a dummy player for testing
        currentPlayer = new Player("Marcello2");
        currentPlayer.setId("marcello2");


        // Load menu fragment
        if (savedInstanceState == null) {
            loadFragment(new MenuFragment());
        }
    }

    private void testFirebaseConnection() {
        FirebaseDatabase.getInstance().getReference(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    Toast.makeText(MainActivity.this, "Connected to Firebase!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Not connected to Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Firebase connection error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logoutUser();
            return true;
        } else if (id == R.id.action_help) {
            showHelpDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        firebaseController.logoutUser();
        preferenceManager.clearUserData();
        navigateToLoginActivity();
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showHelpDialog() {
        // TODO: Implement help dialog
        Toast.makeText(this, "Help coming soon!", Toast.LENGTH_SHORT).show();
    }

    public void loadFragment(Fragment fragment) {
        // Pass current player to fragment if needed
        Bundle args = new Bundle();
        if (currentPlayer != null) {
            args.putString("playerId", currentPlayer.getId());
            args.putString("playerName", currentPlayer.getUsername());
            fragment.setArguments(args);
        }

        // Replace current fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToCreateLobby() {
        loadFragment(new CreateLobbyFragment());
    }

    public void navigateToJoinLobby() {
        loadFragment(new JoinLobbyFragment());
    }

    public void navigateToGameActivity(String lobbyId, String gameId) {
        // Check if player is host
        boolean isHost = false;
        if (lobbyId != null) {
            // Get lobby from Firebase to check if current player is host
            firebaseController.getLobbyById(lobbyId, new FirebaseController.FirebaseCallback() {
                @Override
                public void onSuccess(Object result) {
                    Lobby lobby = (Lobby) result;
                    boolean isPlayerHost = false;

                    for (Player player : lobby.getPlayers()) {
                        if (player.getId().equals(currentPlayer.getId()) && player.isHost()) {
                            isPlayerHost = true;
                            break;
                        }
                    }

                    Intent intent = new Intent(MainActivity.this, gameId == null ? LobbyActivity.class : GameActivity.class);
                    intent.putExtra("lobbyId", lobbyId);
                    if (gameId != null) {
                        intent.putExtra("gameId", gameId);
                    }
                    intent.putExtra("playerId", currentPlayer.getId());
                    intent.putExtra("isHost", isPlayerHost);
                    startActivity(intent);
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // No lobby provided, just start the game activity
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("gameId", gameId);
            intent.putExtra("playerId", currentPlayer.getId());
            startActivity(intent);
        }
    }
}