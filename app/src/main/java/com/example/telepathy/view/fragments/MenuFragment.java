package com.example.telepathy.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.telepathy.R;
import com.example.telepathy.view.activities.MainActivity;
import com.example.telepathy.view.activities.VideoActivity;

public class MenuFragment extends Fragment {
    private String playerName;
    private Button createLobbyButton;
    private Button joinLobbyButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        // Get player name from arguments
        if (getArguments() != null) {
            playerName = getArguments().getString("playerName", "Player");
        }

        // Initialize UI components
        TextView welcomeTextView = view.findViewById(R.id.welcomeTextView);
        welcomeTextView.setText(getString(R.string.welcome_message, playerName));

        createLobbyButton = view.findViewById(R.id.createLobbyButton);
        joinLobbyButton = view.findViewById(R.id.joinLobbyButton);
        Button watchVideoButton = view.findViewById(R.id.watchVideoButton);

        // Set click listeners
        createLobbyButton.setOnClickListener(v -> navigateToCreateLobby());
        joinLobbyButton.setOnClickListener(v -> navigateToJoinLobby());
        watchVideoButton.setOnClickListener(v -> navigateToVideo());

        return view;
    }

    private void navigateToVideo() {
        Intent intent = new Intent(getActivity(), VideoActivity.class);
        startActivity(intent);
    }

    private void navigateToCreateLobby() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToCreateLobby();
        }
    }

    private void navigateToJoinLobby() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToJoinLobby();
        }
    }
}