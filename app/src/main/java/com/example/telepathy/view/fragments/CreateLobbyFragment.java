package com.example.telepathy.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.telepathy.R;
import com.example.telepathy.controller.FirebaseController;
import com.example.telepathy.model.Lobby;
import com.example.telepathy.model.Player;
import com.example.telepathy.utils.Constants;
import com.example.telepathy.view.activities.MainActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CreateLobbyFragment extends Fragment {
    private EditText lobbyNameEditText;
    private EditText timeLimitEditText;
    private EditText livesEditText;
    private Spinner categorySpinner;
    private ProgressBar progressBar;

    private FirebaseController firebaseController;
    private String playerId;
    private String playerName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_lobby, container, false);

        if (getArguments() != null) {
            playerId = getArguments().getString("playerId");
            playerName = getArguments().getString("playerName");
        }

        firebaseController = FirebaseController.getInstance();

        lobbyNameEditText = view.findViewById(R.id.lobbyNameEditText);
        timeLimitEditText = view.findViewById(R.id.timeLimitEditText);
        livesEditText = view.findViewById(R.id.livesEditText);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        Button createButton = view.findViewById(R.id.createButton);
        progressBar = view.findViewById(R.id.progressBar);

        timeLimitEditText.setText(String.valueOf(Constants.DEFAULT_TIME_LIMIT));
        livesEditText.setText(String.valueOf(Constants.DEFAULT_LIVES));

        loadCategoriesFromFirebase();

        createButton.setOnClickListener(v -> createLobby());

        return view;
    }

    private void createLobby() {
        String lobbyName = lobbyNameEditText.getText().toString().trim();
        String timeLimitStr = timeLimitEditText.getText().toString().trim();
        String livesStr = livesEditText.getText().toString().trim();
        String selectedCategory = categorySpinner.getSelectedItem() != null
                ? categorySpinner.getSelectedItem().toString()
                : null;

        if (lobbyName.isEmpty()) {
            lobbyNameEditText.setError("Lobby name is required");
            lobbyNameEditText.requestFocus();
            return;
        }

        if (timeLimitStr.isEmpty()) {
            timeLimitEditText.setError("Time limit is required");
            timeLimitEditText.requestFocus();
            return;
        }

        if (livesStr.isEmpty()) {
            livesEditText.setError("Lives are required");
            livesEditText.requestFocus();
            return;
        }

        int timeLimit = Integer.parseInt(timeLimitStr);
        int lives = Integer.parseInt(livesStr);

        if (timeLimit < 10 || timeLimit > 120) {
            timeLimitEditText.setError("Time limit must be between 10 and 120 seconds");
            timeLimitEditText.requestFocus();
            return;
        }

        if (lives < 1 || lives > 5) {
            livesEditText.setError("Lives must be between 1 and 5");
            livesEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Player host = new Player(playerName);
        host.setId(playerId);
        host.setHost(true);

        firebaseController.createLobby(lobbyName, host, selectedCategory, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                progressBar.setVisibility(View.GONE);
                Lobby createdLobby = (Lobby) result;
                Toast.makeText(requireContext(), "Lobby created successfully!", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToGameActivity(createdLobby.getId(), null);
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCategoriesFromFirebase() {
        FirebaseDatabase.getInstance().getReference("category")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> categories = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            categories.add(child.getKey());
                        }

                        if (getContext() != null && !categories.isEmpty()) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_spinner_item,
                                    categories
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            categorySpinner.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
