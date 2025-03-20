package com.example.telepathy.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telepathy.R;
import com.example.telepathy.controller.FirebaseController;
import com.example.telepathy.model.Lobby;
import com.example.telepathy.model.Player;
import com.example.telepathy.view.adapters.PlayerListAdapter;

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

    private void updateUI(Lobby lobby) {
        lobbyNameTextView.setText(lobby.getName());

        // Find host name
        String hostName = "Unknown";
        for (Player player : lobby.getPlayers()) {
            if (player.isHost()) {
                hostName = player.getUsername();
                break;
            }
        }

        hostNameTextView.setText(getString(R.string.host_name, hostName));
        categoryTextView.setText(getString(R.string.category_label, lobby.getGameConfig().getSelectedCategory()));

        // Update players list
        players.clear();
        players.addAll(lobby.getPlayers());
        playerListAdapter.notifyDataSetChanged();

        // Enable start button if enough players
        startGameButton.setEnabled(lobby.canStartGame());
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