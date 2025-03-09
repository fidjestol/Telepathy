package com.example.telepathy.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telepathy.R;
import com.example.telepathy.model.Lobby;

import java.util.List;

public class LobbyListAdapter extends RecyclerView.Adapter<LobbyListAdapter.LobbyViewHolder> {
    private List<Lobby> lobbies;
    private OnLobbyClickListener listener;

    public interface OnLobbyClickListener {
        void onLobbyClick(Lobby lobby);
    }

    public LobbyListAdapter(List<Lobby> lobbies, OnLobbyClickListener listener) {
        this.lobbies = lobbies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LobbyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lobby, parent, false);
        return new LobbyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LobbyViewHolder holder, int position) {
        Lobby lobby = lobbies.get(position);
        holder.bind(lobby);
    }

    @Override
    public int getItemCount() {
        return lobbies.size();
    }

    class LobbyViewHolder extends RecyclerView.ViewHolder {
        private TextView lobbyNameTextView;
        private TextView hostNameTextView;
        private TextView playerCountTextView;
        private TextView categoryTextView;

        public LobbyViewHolder(@NonNull View itemView) {
            super(itemView);
            lobbyNameTextView = itemView.findViewById(R.id.lobbyNameTextView);
            hostNameTextView = itemView.findViewById(R.id.hostNameTextView);
            playerCountTextView = itemView.findViewById(R.id.playerCountTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onLobbyClick(lobbies.get(position));
                }
            });
        }

        public void bind(Lobby lobby) {
            lobbyNameTextView.setText(lobby.getName());

            // Find host name
            String hostName = "Unknown";
            for (int i = 0; i < lobby.getPlayers().size(); i++) {
                if (lobby.getPlayers().get(i).isHost()) {
                    hostName = lobby.getPlayers().get(i).getUsername();
                    break;
                }
            }

            hostNameTextView.setText(itemView.getContext().getString(R.string.host_name, hostName));
            playerCountTextView.setText(itemView.getContext().getString(
                    R.string.player_count,
                    lobby.getPlayers().size(),
                    lobby.getGameConfig().getMaxPlayers()));

            categoryTextView.setText(itemView.getContext().getString(
                    R.string.category_label,
                    lobby.getGameConfig().getSelectedCategory()));
        }
    }
}