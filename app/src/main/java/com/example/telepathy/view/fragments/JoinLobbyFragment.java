package com.example.telepathy.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.telepathy.R;
import com.example.telepathy.controller.FirebaseController;
import com.example.telepathy.model.Lobby;
import com.example.telepathy.model.Player;
import com.example.telepathy.view.activities.MainActivity;
import com.example.telepathy.view.adapters.LobbyListAdapter;

import java.util.ArrayList;
import java.util.List;

public class JoinLobbyFragment extends Fragment implements LobbyListAdapter.OnLobbyClickListener {
    private RecyclerView lobbiesRecyclerView;
    private LobbyListAdapter lobbyListAdapter;
    private ProgressBar progressBar;
    private TextView noLobbiesTextView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseController firebaseController;
    private String playerId;
    private String playerName;
    private List<Lobby> lobbies = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_lobby, container, false);

        // Get player data from arguments
        if (getArguments() != null) {
            playerId = getArguments().getString("playerId");
            playerName = getArguments().getString("playerName");
        }

        // Initialize Firebase controller
        firebaseController = FirebaseController.getInstance();

        // Initialize UI components
        lobbiesRecyclerView = view.findViewById(R.id.lobbiesRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        noLobbiesTextView = view.findViewById(R.id.noLobbiesTextView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Set up RecyclerView
        lobbiesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        lobbyListAdapter = new LobbyListAdapter(lobbies, this);
        lobbiesRecyclerView.setAdapter(lobbyListAdapter);

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadLobbies);

        // Load lobbies
        loadLobbies();

        return view;
    }

    private void loadLobbies() {
        progressBar.setVisibility(View.VISIBLE);
        noLobbiesTextView.setVisibility(View.GONE);

        firebaseController.getLobbyList(new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                List<Lobby> loadedLobbies = (List<Lobby>) result;
                lobbies.clear();
                lobbies.addAll(loadedLobbies);
                lobbyListAdapter.notifyDataSetChanged();

                if (lobbies.isEmpty()) {
                    noLobbiesTextView.setVisibility(View.VISIBLE);
                } else {
                    noLobbiesTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();

                if (lobbies.isEmpty()) {
                    noLobbiesTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onLobbyClick(Lobby lobby) {
        progressBar.setVisibility(View.VISIBLE);

        // Create player instance
        Player player = new Player(playerId, playerName);
        player.setId(playerId);

        // Set lives based on game mode
        if (lobby.getGameConfig().isMatchingMode()) {
            player.setLives(1); // Matching mode always has 1 life
        } else {
            player.setLives(lobby.getGameConfig().getLivesPerPlayer()); // Use the lobby's configured lives
        }

        // Join lobby
        firebaseController.joinLobby(lobby.getId(), player, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressBar.setVisibility(View.GONE);
                Lobby joinedLobby = (Lobby) result;

                // Navigate to game activity
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToGameActivity(joinedLobby.getId(), joinedLobby.getGameId());
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();

                // Refresh lobbies list
                loadLobbies();
            }
        });
    }
}