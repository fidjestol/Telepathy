package com.example.telepathy.controller;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.telepathy.model.GameConfig;
import com.example.telepathy.model.Lobby;
import com.example.telepathy.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseController {
    private static FirebaseController instance;
    private FirebaseAuth auth;
    private DatabaseReference database;

    // Interface for callbacks
    public interface FirebaseCallback {
        void onSuccess(Object result);
        void onFailure(String error);
    }

    private FirebaseController() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized FirebaseController getInstance() {
        if (instance == null) {
            instance = new FirebaseController();
        }
        return instance;
    }

    // Authentication methods
    public void registerUser(String email, String password, String username, FirebaseCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Create player profile
                            Player newPlayer = new Player(username);
                            newPlayer.setId(user.getUid());

                            // Save to database
                            database.child("users").child(user.getUid()).setValue(newPlayer)
                                    .addOnCompleteListener(saveTask -> {
                                        if (saveTask.isSuccessful()) {
                                            callback.onSuccess(newPlayer);
                                        } else {
                                            callback.onFailure("Failed to save user data");
                                        }
                                    });
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    public void loginUser(String email, String password, FirebaseCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            database.child("users").child(user.getUid()).get()
                                    .addOnCompleteListener(dataTask -> {
                                        if (dataTask.isSuccessful() && dataTask.getResult() != null) {
                                            Player player = dataTask.getResult().getValue(Player.class);
                                            callback.onSuccess(player);
                                        } else {
                                            callback.onFailure("Failed to get user data");
                                        }
                                    });
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Login failed");
                    }
                });
    }

    public void logoutUser() {
        auth.signOut();
    }

    // Lobby methods
    public void createLobby(String lobbyName, Player host, FirebaseCallback callback) {
        Lobby lobby = new Lobby(lobbyName, host);

        database.child("lobbies").child(lobby.getId()).setValue(lobby)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(lobby);
                    } else {
                        callback.onFailure("Failed to create lobby");
                    }
                });
    }

    public void getLobbyList(FirebaseCallback callback) {
        database.child("lobbies").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Lobby> lobbies = new ArrayList<>();
                for (DataSnapshot lobbySnapshot : snapshot.getChildren()) {
                    Lobby lobby = lobbySnapshot.getValue(Lobby.class);
                    if (lobby != null && lobby.isOpen()) {
                        lobbies.add(lobby);
                    }
                }
                callback.onSuccess(lobbies);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    public void joinLobby(String lobbyId, Player player, FirebaseCallback callback) {
        database.child("lobbies").child(lobbyId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Lobby lobby = task.getResult().getValue(Lobby.class);
                        if (lobby != null && lobby.isOpen() && !lobby.isLobbyFull()) {
                            // Add player to lobby
                            lobby.addPlayer(player);

                            // Update lobby in database
                            database.child("lobbies").child(lobbyId).setValue(lobby)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            callback.onSuccess(lobby);
                                        } else {
                                            callback.onFailure("Failed to join lobby");
                                        }
                                    });
                        } else {
                            callback.onFailure("Lobby is full or closed");
                        }
                    } else {
                        callback.onFailure("Lobby not found");
                    }
                });
    }

    public void leaveLobby(String lobbyId, String playerId, FirebaseCallback callback) {
        database.child("lobbies").child(lobbyId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Lobby lobby = task.getResult().getValue(Lobby.class);
                        if (lobby != null) {
                            // Remove player from lobby
                            boolean removed = lobby.removePlayer(playerId);

                            if (removed) {
                                // If no players left, delete lobby
                                if (lobby.getPlayers().isEmpty()) {
                                    database.child("lobbies").child(lobbyId).removeValue()
                                            .addOnCompleteListener(deleteTask -> {
                                                if (deleteTask.isSuccessful()) {
                                                    callback.onSuccess(null);
                                                } else {
                                                    callback.onFailure("Failed to delete empty lobby");
                                                }
                                            });
                                } else {
                                    // Update lobby in database
                                    database.child("lobbies").child(lobbyId).setValue(lobby)
                                            .addOnCompleteListener(updateTask -> {
                                                if (updateTask.isSuccessful()) {
                                                    callback.onSuccess(lobby);
                                                } else {
                                                    callback.onFailure("Failed to update lobby");
                                                }
                                            });
                                }
                            } else {
                                callback.onFailure("Player not found in lobby");
                            }
                        } else {
                            callback.onFailure("Lobby not found");
                        }
                    } else {
                        callback.onFailure("Lobby not found");
                    }
                });
    }

    public void startGame(String lobbyId, FirebaseCallback callback) {
        database.child("lobbies").child(lobbyId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Lobby lobby = task.getResult().getValue(Lobby.class);
                        if (lobby != null && lobby.canStartGame()) {
                            // Close the lobby
                            lobby.setOpen(false);

                            // Create a new game instance
                            Map<String, Object> gameData = new HashMap<>();
                            gameData.put("lobbyId", lobbyId);
                            gameData.put("players", lobby.getPlayers());
                            gameData.put("config", lobby.getGameConfig());
                            gameData.put("currentRound", 1);
                            gameData.put("status", "active");

                            // Save game data
                            String gameId = database.child("games").push().getKey();
                            if (gameId != null) {
                                database.child("games").child(gameId).setValue(gameData)
                                        .addOnCompleteListener(gameTask -> {
                                            if (gameTask.isSuccessful()) {
                                                // Update lobby with game reference
                                                Map<String, Object> updates = new HashMap<>();
                                                updates.put("isOpen", false);
                                                updates.put("gameId", gameId);

                                                database.child("lobbies").child(lobbyId).updateChildren(updates)
                                                        .addOnCompleteListener(updateTask -> {
                                                            if (updateTask.isSuccessful()) {
                                                                callback.onSuccess(gameId);
                                                            } else {
                                                                callback.onFailure("Failed to update lobby");
                                                            }
                                                        });
                                            } else {
                                                callback.onFailure("Failed to create game");
                                            }
                                        });
                            } else {
                                callback.onFailure("Failed to generate game ID");
                            }
                        } else {
                            callback.onFailure("Cannot start game: not enough players");
                        }
                    } else {
                        callback.onFailure("Lobby not found");
                    }
                });
    }

    // Game methods
    public void submitWord(String gameId, String playerId, String word, FirebaseCallback callback) {
        database.child("games").child(gameId).child("players").child(playerId).child("currentWord")
                .setValue(word)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("Failed to submit word");
                    }
                });
    }

    public void listenForGameUpdates(String gameId, ValueEventListener listener) {
        database.child("games").child(gameId).addValueEventListener(listener);
    }

    public void removeGameListener(String gameId, ValueEventListener listener) {
        database.child("games").child(gameId).removeEventListener(listener);
    }
}