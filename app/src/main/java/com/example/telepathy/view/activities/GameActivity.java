package com.example.telepathy.view.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telepathy.R;
import com.example.telepathy.controller.GameController;
import com.example.telepathy.model.Game;
import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Player;
import com.example.telepathy.view.adapters.PlayerListAdapter;
import com.example.telepathy.view.adapters.WordListAdapter;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity implements GameController.GameUpdateListener {
    private TextView lobbyNameTextView;
    private TextView timerTextView;
    private TextView roundTextView;
    private TextView livesTextView;
    private RecyclerView playersRecyclerView;
    private RecyclerView wordsRecyclerView;
    private EditText wordInputEditText;
    private Button submitButton;
    private View gameControlsLayout;
    private View waitingLayout;
    private ProgressBar progressBar;

    private PlayerListAdapter playerListAdapter;
    private WordListAdapter wordListAdapter;
    private GameController gameController;

    private String lobbyId;
    private String gameId;
    private String playerId;
    private List<Player> players = new ArrayList<>();
    private List<String> validWords = new ArrayList<>();

    private CountDownTimer countDownTimer;
    private boolean isRoundActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Get data from intent
        lobbyId = getIntent().getStringExtra("lobbyId");
        gameId = getIntent().getStringExtra("gameId");
        playerId = getIntent().getStringExtra("playerId");

        // Initialize UI components
        lobbyNameTextView = findViewById(R.id.lobbyNameTextView);
        timerTextView = findViewById(R.id.timerTextView);
        roundTextView = findViewById(R.id.roundTextView);
        livesTextView = findViewById(R.id.livesTextView);
        playersRecyclerView = findViewById(R.id.playersRecyclerView);
        wordsRecyclerView = findViewById(R.id.wordsRecyclerView);
        wordInputEditText = findViewById(R.id.wordInputEditText);
        submitButton = findViewById(R.id.submitButton);
        gameControlsLayout = findViewById(R.id.gameControlsLayout);
        waitingLayout = findViewById(R.id.waitingLayout);
        progressBar = findViewById(R.id.progressBar);

        // Set up RecyclerViews
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playerListAdapter = new PlayerListAdapter(players);
        playersRecyclerView.setAdapter(playerListAdapter);

        wordsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        wordListAdapter = new WordListAdapter(validWords);
        wordsRecyclerView.setAdapter(wordListAdapter);

        // Set click listener
        submitButton.setOnClickListener(v -> submitWord());

        // Initialize game controller
        if (gameId != null) {
            // Join existing game
            gameController = new GameController(gameId, playerId, this);
        } else if (lobbyId != null) {
            // Show waiting screen until game starts
            gameControlsLayout.setVisibility(View.GONE);
            waitingLayout.setVisibility(View.VISIBLE);

            // TODO: Add logic to wait for game to start
            // For now, let's add a start game button for the host
            Button startGameButton = findViewById(R.id.startGameButton);
            startGameButton.setVisibility(View.VISIBLE);
            startGameButton.setOnClickListener(v -> startGame());
        } else {
            // Invalid state
            Toast.makeText(this, "Invalid game state", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startGame() {
        progressBar.setVisibility(View.VISIBLE);

        // Call Firebase to start the game
        // This is just a placeholder - implement with your FirebaseController
        gameController = new GameController(lobbyId, playerId, this);

        // For now, hide the waiting layout
        waitingLayout.setVisibility(View.GONE);
        gameControlsLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void submitWord() {
        if (!isRoundActive) {
            Toast.makeText(this, "Wait for the round to start", Toast.LENGTH_SHORT).show();
            return;
        }

        String word = wordInputEditText.getText().toString().trim().toLowerCase();

        if (word.isEmpty()) {
            wordInputEditText.setError("Please enter a word");
            return;
        }

        // Validate and submit word
        gameController.validateWord(word);

        // Clear input field
        wordInputEditText.setText("");
    }

    private void startTimer(long durationMillis) {
        // Cancel any existing timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Start new countdown timer
        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                timerTextView.setText(String.valueOf(seconds));
            }

            @Override
            public void onFinish() {
                timerTextView.setText("0");
                isRoundActive = false;

                // Disable input
                wordInputEditText.setEnabled(false);
                submitButton.setEnabled(false);

                Toast.makeText(GameActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
            }
        }.start();

        // Enable input
        wordInputEditText.setEnabled(true);
        submitButton.setEnabled(true);
        isRoundActive = true;
    }

    @Override
    public void onGameStateChanged(Game game) {
        // Update UI with game state
        runOnUiThread(() -> {
            lobbyNameTextView.setText(game.getLobbyId());

            // Update player list
            players.clear();
            players.addAll(game.getPlayers());
            playerListAdapter.notifyDataSetChanged();

            // Update current player lives
            for (Player player : players) {
                if (player.getId().equals(playerId)) {
                    livesTextView.setText(getString(R.string.lives_left, player.getLives()));
                    break;
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onRoundStart(GameRound round) {
        runOnUiThread(() -> {
            roundTextView.setText(getString(R.string.round_number, round.getRoundNumber()));

            // Update word list
            validWords.clear();
            validWords.addAll(round.getWords());
            wordListAdapter.notifyDataSetChanged();

            // Start timer
            long duration = (round.getEndTime() - round.getStartTime());
            startTimer(duration);

            // Clear input
            wordInputEditText.setText("");

            // Show toast
            Toast.makeText(this, "Round " + round.getRoundNumber() + " started", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRoundEnd(GameRound round) {
        runOnUiThread(() -> {
            // Stop timer
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            isRoundActive = false;

            // Disable input
            wordInputEditText.setEnabled(false);
            submitButton.setEnabled(false);

            Toast.makeText(this, "Round ended", Toast.LENGTH_SHORT).show();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onPlayerEliminated(Player player) {
        runOnUiThread(() -> {
            if (player.getId().equals(playerId)) {
                // Current player eliminated
                showEliminationDialog();
            } else {
                // Another player eliminated
                Toast.makeText(this, player.getUsername() + " has been eliminated!", Toast.LENGTH_SHORT).show();
            }

            // Update player list
            playerListAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onGameEnd(Player winner) {
        runOnUiThread(() -> {
            // Stop timer
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            // Show game end dialog
            showGameEndDialog(winner);
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            // Show error message in UI
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();

            // Log the error to help debugging
            Log.e("TELEPATHY_ERROR", error);
            System.out.println("TELEPATHY_ERROR: " + error);
        });
    }

    private void showEliminationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Eliminated!");
        builder.setMessage("You have been eliminated from the game.");
        builder.setCancelable(false);
        builder.setPositiveButton("Spectate", (dialog, which) -> {
            // Stay in the game as spectator
            dialog.dismiss();
        });
        builder.setNegativeButton("Exit", (dialog, which) -> {
            // Leave the game
            finish();
        });
        builder.show();
    }

    private void showGameEndDialog(Player winner) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over!");

        String message;
        if (winner != null) {
            if (winner.getId().equals(playerId)) {
                message = "Congratulations! You won the game!";
            } else {
                message = winner.getUsername() + " has won the game!";
            }
        } else {
            message = "The game has ended.";
        }

        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("Back to Main Menu", (dialog, which) -> {
            // Go back to main menu
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (gameController != null) {
            gameController.cleanup();
        }
    }


}