package com.example.telepathy.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telepathy.R;
import com.example.telepathy.model.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerListAdapter extends RecyclerView.Adapter<PlayerListAdapter.PlayerViewHolder> {
    private List<Player> players;
    private boolean showSubmissions = false;
    private Map<String, Integer> duplicateWords = new HashMap<>();

    public PlayerListAdapter(List<Player> players) {
        this.players = players;
    }

    public void setShowSubmissions(boolean showSubmissions) {
        this.showSubmissions = showSubmissions;
    }

    public void setDuplicateWords(Map<String, Integer> duplicateWords) {
        this.duplicateWords = duplicateWords;
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = players.get(position);
        holder.bind(player, showSubmissions, duplicateWords);
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        private TextView usernameTextView;
        private TextView scoreTextView;
        private TextView statusTextView;
        private TextView submissionTextView;  // Add this new TextView to your item_player.xml

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            scoreTextView = itemView.findViewById(R.id.scoreTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            submissionTextView = itemView.findViewById(R.id.submissionTextView);
        }

        public void bind(Player player, boolean showSubmissions, Map<String, Integer> duplicateWords) {
            usernameTextView.setText(player.getUsername());
            scoreTextView.setText(String.valueOf(player.getScore()));

            if (player.isEliminated()) {
                statusTextView.setText(R.string.player_eliminated);
                statusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
            } else {
                statusTextView.setText(itemView.getContext().getString(R.string.lives_left, player.getLives()));
                statusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            }

            // Show submission if needed
            if (showSubmissions && player.getCurrentWord() != null && !player.getCurrentWord().isEmpty()) {
                submissionTextView.setVisibility(View.VISIBLE);
                String word = player.getCurrentWord();

                // Check if this is a duplicate word
                boolean isDuplicate = duplicateWords.getOrDefault(word, 0) > 1;

                if (isDuplicate) {
                    submissionTextView.setText("⚠️ " + word);
                    submissionTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
                } else {
                    submissionTextView.setText("✓ " + word);
                    submissionTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                }
            } else {
                // Hide submission text or show waiting message
                if (showSubmissions) {
                    submissionTextView.setVisibility(View.VISIBLE);
                    submissionTextView.setText("No word submitted");
                    submissionTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                } else {
                    submissionTextView.setVisibility(View.GONE);
                }
            }
        }
    }
}