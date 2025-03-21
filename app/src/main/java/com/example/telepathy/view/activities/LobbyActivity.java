package com.example.telepathy.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telepathy.R;
import com.example.telepathy.controller.FirebaseController;
import com.example.telepathy.model.Lobby;
import com.example.telepathy.model.Player;
import com.example.telepathy.view.adapters.PlayerListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class LobbyActivity extends AppCompatActivity {
    private TextView lobbyNameTextView;
    private TextView hostNameTextView;
    private TextView categoryTextView;
    private RecyclerView playersRecyclerView;
    private Button startGameButton;
    private Button leaveLobbyButton;
    private ProgressBar progressBar;

    private FirebaseController firebaseController;
    private PlayerListAdapter playerListAdapter;
    private ArrayList<Player> players = new ArrayList<>();
    private String lobbyId;
    private String playerId;
    private boolean isHost = false;

    private ValueEventListener lobbyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // Get data from intent
        lobbyId = getIntent().getStringExtra("lobbyId");
        playerId = getIntent().getStringExtra("playerId");
        isHost = getIntent().getBooleanExtra("isHost", false);

        // Initialize Firebase controller
        firebaseController = FirebaseController.getInstance();

        // Initialize UI components
        lobbyNameTextView = findViewById(R.id.lobbyNameTextView);
        hostNameTextView = findViewById(R.id.hostNameTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        playersRecyclerView = findViewById(R.id.playersRecyclerView);
        startGameButton = findViewById(R.id.startGameButton);
        leaveLobbyButton = findViewById(R.id.leaveLobbyButton);
        progressBar = findViewById(R.id.progressBar);

        // Set up RecyclerView
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playerListAdapter = new PlayerListAdapter(players);
        playersRecyclerView.setAdapter(playerListAdapter);

        // Show or hide start game button based on user role
        startGameButton.setVisibility(isHost ? View.VISIBLE : View.GONE);

        // Set click listeners
        startGameButton.setOnClickListener(v -> startGame());
        leaveLobbyButton.setOnClickListener(v -> leaveLobby());

        // Load lobby data
        loadLobbyData();

        setupLobbyListener();
    }

    private void setupLobbyListener() {
        DatabaseReference lobbyRef = FirebaseDatabase.getInstance().getReference()
                .child("lobbies").child(lobbyId);

        lobbyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Lobby lobby = snapshot.getValue(Lobby.class);
                if (lobby != null) {
                    updateUI(lobby);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LobbyActivity.this, "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        lobbyRef.addValueEventListener(lobbyListener);
    }

    private void loadLobbyData() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseController.getLobbyList(new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressBar.setVisibility(View.GONE);

                // Find the current lobby
                ArrayList<Lobby> lobbies = (ArrayList<Lobby>) result;
                for (Lobby lobby : lobbies) {
                    if (lobby.getId().equals(lobbyId)) {
                        updateUI(lobby);
                        break;
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LobbyActivity.this, error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listener to prevent memory leaks
        if (lobbyListener != null) {
            DatabaseReference lobbyRef = FirebaseDatabase.getInstance().getReference()
                    .child("lobbies").child(lobbyId);
            lobbyRef.removeEventListener(lobbyListener);
        }
    }

    private void updateUI(Lobby lobby) {
        lobbyNameTextView.setText(lobby.getName());

        // Find host name and determine if current player is the host
        String hostName = "Unknown";
        boolean currentPlayerIsHost = false;

        for (Player player : lobby.getPlayers()) {
            if (player.isHost()) {
                hostName = player.getUsername();
                if (player.getId().equals(playerId)) {
                    currentPlayerIsHost = true;
                }
                break;
            }
        }

        hostNameTextView.setText(getString(R.string.host_name, hostName));
        categoryTextView.setText(getString(R.string.category_label, lobby.getGameConfig().getSelectedCategory()));

        // Update players list
        players.clear();
        players.addAll(lobby.getPlayers());
        playerListAdapter.notifyDataSetChanged();

        // Only show start button to host
        startGameButton.setVisibility(currentPlayerIsHost ? View.VISIBLE : View.GONE);

        // Enable button only if enough players and current player is host
        startGameButton.setEnabled(currentPlayerIsHost && lobby.canStartGame());
    }

    private void startGame() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseController.startGame(lobbyId, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressBar.setVisibility(View.GONE);
                String gameId = (String) result;
                navigateToGameActivity(gameId);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LobbyActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void leaveLobby() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseController.leaveLobby(lobbyId, playerId, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressBar.setVisibility(View.GONE);
                finish();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LobbyActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToGameActivity(String gameId) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("lobbyId", lobbyId);
        intent.putExtra("gameId", gameId);
        intent.putExtra("playerId", playerId);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Ensure player leaves lobby when back button is pressed
        leaveLobby();
    }
}