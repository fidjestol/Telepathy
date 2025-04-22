package com.example.telepathy.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telepathy.R;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WordHistoryAdapter extends RecyclerView.Adapter<WordHistoryAdapter.WordViewHolder> {
    private Set<String> uniqueWords; // LinkedHashSet maintains insertion order
    private List<String> displayWords; // Words to display
    private List<String> systemMessages; // System messages

    public WordHistoryAdapter() {
        this.uniqueWords = new LinkedHashSet<>();
        this.displayWords = new ArrayList<>();
        this.systemMessages = new ArrayList<>();
    }

    public void addWord(String word) {
        // Clean the word (remove any player prefixes)
        String cleanWord = word;
        if (word.contains(":")) {
            cleanWord = word.substring(word.indexOf(":") + 1).trim();
        }

        // Only add if it's a new word for display
        if (uniqueWords.add(cleanWord)) {
            // Add to the beginning of the display list
            displayWords.add(0, cleanWord);
            notifyItemInserted(0);

            // Log for debugging
            System.out.println("TELEPATHY: Added word to history: " + cleanWord);
        } else {
            System.out.println("TELEPATHY: Word already in history: " + cleanWord);
        }
    }

    public void addSystemMessage(String message) {
        // System messages are always added
        displayWords.add(0, "SYSTEM: " + message);
        systemMessages.add(message); // Keep track of system messages separately
        notifyItemInserted(0);

        System.out.println("TELEPATHY: Added system message: " + message);
    }

    public void clear() {
        uniqueWords.clear();
        displayWords.clear();
        systemMessages.clear();
        notifyDataSetChanged();
    }

    public Set<String> getUniqueWords() {
        return uniqueWords;
    }

    public List<String> getSystemMessages() {
        return systemMessages;
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        String word = displayWords.get(position);
        holder.wordTextView.setText(word);

        // If it's a system message, style it differently
        if (word.startsWith("SYSTEM:")) {
            holder.wordTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            holder.wordTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
        }
    }

    @Override
    public int getItemCount() {
        return displayWords.size();
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        TextView wordTextView;

        public WordViewHolder(@NonNull View itemView) {
            super(itemView);
            wordTextView = itemView.findViewById(R.id.wordTextView);
        }
    }
}