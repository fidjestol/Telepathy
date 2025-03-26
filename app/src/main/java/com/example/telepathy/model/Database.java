package com.example.telepathy.model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class Database {
    private static Database instance;
    private DatabaseReference databaseReference;

    private Database() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public DatabaseReference getReference() {
        return databaseReference;
    }

    public DatabaseReference getUsersReference() {
        return databaseReference.child("users");
    }

    public DatabaseReference getLobbiesReference() {
        return databaseReference.child("lobbies");
    }

    public DatabaseReference getGamesReference() {
        return databaseReference.child("games");
    }

    public DatabaseReference getWordbankReference() {
        return databaseReference.child("wordbanks");
    }

    public DatabaseReference getUserReference(String userId) {
        return getUsersReference().child(userId);
    }

    public DatabaseReference getLobbyReference(String lobbyId) {
        return getLobbiesReference().child(lobbyId);
    }

    public DatabaseReference getGameReference(String gameId) {
        return getGamesReference().child(gameId);
    }


    public void addCategory() {
        // Add categories to database
        //TODO: avoid duplicate categories + words
        databaseReference.child("category").setValue("Animals");
        List<String> words = Arrays.asList("dog", "cat");
        databaseReference.child("category").child("Animals").setValue(words);
    }
}