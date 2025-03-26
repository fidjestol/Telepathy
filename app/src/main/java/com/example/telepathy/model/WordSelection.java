package com.example.telepathy.model;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordSelection {

    private static final Random random = new Random();
    private static final DatabaseReference categoryRef =
            FirebaseDatabase.getInstance().getReference("category");

    /**
     * Get a list of random words from the specified category (asynchronously).
     * @param category The category to select words from
     * @param count Number of words to select
     * @param callback Callback to return selected words
     */
    public static void getRandomWords(String category, int count, WordCallback callback) {
        categoryRef.child(category).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> wordList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String word = child.getValue(String.class);
                    if (word != null) wordList.add(word);
                }

                List<String> selectedWords = new ArrayList<>();
                for (int i = 0; i < count && !wordList.isEmpty(); i++) {
                    int index = random.nextInt(wordList.size());
                    selectedWords.add(wordList.remove(index));
                }

                callback.onWordsSelected(selectedWords);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onWordsSelected(new ArrayList<>()); // Return empty list on error
            }
        });
    }

    /**
     * Get all available categories from Firebase (asynchronously).
     * @param callback Callback to return list of categories
     */

    // Callback interfaces

    public interface WordCallback {
        void onWordsSelected(List<String> words);
    }

    public interface CategoryCallback {
        void onCategoriesLoaded(List<String> categories);
    }
}
