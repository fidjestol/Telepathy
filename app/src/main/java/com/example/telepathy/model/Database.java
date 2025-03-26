package com.example.telepathy.model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import androidx.annotation.NonNull;


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
// Not necessary if everything lives under "category" instead of "wordbanks"
//    public DatabaseReference getWordbankReference() {
//        return databaseReference.child("wordbanks");
//    }

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
        DatabaseReference categoryRef = databaseReference.child("category");

        // Define categories and word lists
        Map<String, List<String>> newCategories = new HashMap<>();
        newCategories.put("Animals", Arrays.asList("dog", "cat"));
        newCategories.put("Foods", Arrays.asList(
                "pizza", "burger", "pasta", "rice", "bread", "potato", "tomato", "onion",
                "carrot", "broccoli", "apple", "banana", "orange", "strawberry", "grape",
                "chicken", "beef", "pork", "fish", "egg", "milk", "cheese", "yogurt",
                "ice cream", "chocolate", "cake", "cookie", "pie", "soup", "salad"
        ));

        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Map.Entry<String, List<String>> entry : newCategories.entrySet()) {
                    String categoryName = entry.getKey();
                    List<String> newWords = entry.getValue();

                    // If category doesn't exist, add it directly
                    if (!snapshot.hasChild(categoryName)) {
                        categoryRef.child(categoryName).setValue(newWords);
                    } else {
                        // Category exists — get current words and merge with new ones
                        List<String> existingWords = new ArrayList<>();
                        for (DataSnapshot wordSnap : snapshot.child(categoryName).getChildren()) {
                            String word = wordSnap.getValue(String.class);
                            if (word != null) existingWords.add(word);
                        }

                        // Merge and remove duplicates
                        Set<String> mergedWords = new HashSet<>(existingWords);
                        mergedWords.addAll(newWords);

                        categoryRef.child(categoryName).setValue(new ArrayList<>(mergedWords));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors if needed
            }
        });
    }

}