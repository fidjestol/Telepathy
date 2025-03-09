package com.example.telepathy.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telepathy.R;
import com.example.telepathy.model.Player;

import java.util.List;

public class PlayerListAdapter extends RecyclerView.Adapter<PlayerListAdapter.PlayerViewHolder> {
    private List<Player> players;

    public PlayerListAdapter(List<Player> players) {
        this.players = players;
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
        holder.bind(player);
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        private TextView usernameTextView;
        private TextView scoreTextView;
        private TextView statusTextView;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            scoreTextView = itemView.findViewById(R.id.scoreTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }

        public void bind(Player player) {
            usernameTextView.setText(player.getUsername());
            scoreTextView.setText(String.valueOf(player.getScore()));

            if (player.isEliminated()) {
                statusTextView.setText(R.string.player_eliminated);
                statusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
            } else {
                statusTextView.setText(R.string.player_active);
                statusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }
}