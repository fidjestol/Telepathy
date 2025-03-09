package com.example.telepathy.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.telepathy.R;
import com.example.telepathy.controller.FirebaseController;
import com.example.telepathy.model.Player;
import com.example.telepathy.utils.PreferenceManager;
import com.example.telepathy.view.fragments.CreateLobbyFragment;
import com.example.telepathy.view.fragments.JoinLobbyFragment;
import com.example.telepathy.view.fragments.MenuFragment;

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
        preferenceManager = new PreferenceManager(this);

        // Check if user is logged in
        if (!preferenceManager.isLoggedIn()) {
            navigateToLoginActivity();
            return;
        }

        // Get current player
        currentPlayer = preferenceManager.getUserData();

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));

        // Load menu fragment
        loadFragment(new MenuFragment());
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
        args.putString("playerId", currentPlayer.getId());
        args.putString("playerName", currentPlayer.getUsername());
        fragment.setArguments(args);

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
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("lobbyId", lobbyId);
        intent.putExtra("gameId", gameId);
        intent.putExtra("playerId", currentPlayer.getId());
        startActivity(intent);
    }
}