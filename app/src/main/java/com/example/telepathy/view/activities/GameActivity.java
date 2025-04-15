package com.example.telepathy.view.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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
import com.example.telepathy.controller.FirebaseController;
import com.example.telepathy.controller.GameController;
import com.example.telepathy.model.Game;
import com.example.telepathy.model.GameRound;
import com.example.telepathy.model.Lobby;
import com.example.telepathy.model.Player;
import com.example.telepathy.view.adapters.PlayerListAdapter;
import com.example.telepathy.view.adapters.WordHistoryAdapter;
import com.example.telepathy.view.adapters.WordListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private RecyclerView wordHistoryRecyclerView;
    private WordHistoryAdapter wordHistoryAdapter;
    private List<String> usedWords = new ArrayList<>(); // To store words that have been used

    private PlayerListAdapter playerListAdapter;
    private WordListAdapter wordListAdapter;
    private GameController gameController;

    private String lobbyId;
    private String gameId;
    private String playerId;
    private List<Player> players = new ArrayList<>();
    private List<String> validWords = new ArrayList<>();

    private boolean isHost;

    private CountDownTimer countDownTimer;
    private boolean isRoundActive = false;

    private FirebaseController firebaseController;
    private AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Get data from intent
        lobbyId = getIntent().getStringExtra("lobbyId");
        gameId = getIntent().getStringExtra("gameId");
        playerId = getIntent().getStringExtra("playerId");
        isHost = getIntent().getBooleanExtra("isHost", false); // Get host status

        // Initialize Firebase controller
        firebaseController = FirebaseController.getInstance();

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

        wordHistoryRecyclerView = findViewById(R.id.wordHistoryRecyclerView);
        wordHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        wordHistoryAdapter = new WordHistoryAdapter(usedWords);
        wordHistoryRecyclerView.setAdapter(wordHistoryAdapter);

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

            // Add a start game button for the host
            Button startGameButton = findViewById(R.id.startGameButton);
            startGameButton.setVisibility(isHost ? View.VISIBLE : View.GONE);
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
        firebaseController.startGame(lobbyId, new FirebaseController.FirebaseCallback() {
            @Override
            public void onSuccess(Object result) {
                gameId = (String) result;
                gameController = new GameController(gameId, playerId, GameActivity.this);

                // Hide the waiting layout
                waitingLayout.setVisibility(View.GONE);
                gameControlsLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GameActivity.this, "Failed to start game: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitWord() {
        if (!isRoundActive) {
            Toast.makeText(this, "Wait for the round to start", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if player has already submitted a word for this round
        for (Player player : players) {
            if (player.getId().equals(playerId) && player.getCurrentWord() != null && !player.getCurrentWord().isEmpty()) {
                Toast.makeText(this, "You've already submitted a word for this round", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String word = wordInputEditText.getText().toString().trim().toLowerCase();

        if (word.isEmpty()) {
            wordInputEditText.setError("Please enter a word");
            return;
        }

        // Check if word is in the valid words list
        boolean isValidWord = false;
        for (String validWord : validWords) {
            if (validWord.equalsIgnoreCase(word)) {
                isValidWord = true;
                break;
            }
        }

        if (!isValidWord) {
            Toast.makeText(this, "Word is not in the valid words list", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate and submit word
        gameController.validateWord(word);

        // Disable input after submission until next round
        wordInputEditText.setEnabled(false);
        submitButton.setEnabled(false);

        // Show feedback
        Toast.makeText(this, "Word submitted! Waiting for other players...", Toast.LENGTH_SHORT).show();

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
            if (!"gameEnd".equals(game.getStatus())) {
                firebaseController.getLobbyById(lobbyId, new FirebaseController.FirebaseCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Lobby lobby = (Lobby) result;
                        if (lobby != null) {
                            runOnUiThread(() -> lobbyNameTextView.setText(lobby.getName()));
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("GameActivity", "Failed to load lobby name: " + error);
                    }
                });
            }

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

            // Update available words for this round
            validWords.clear();
            validWords.addAll(round.getWords());
            wordListAdapter.notifyDataSetChanged();

            // Reset player submission display
            playerListAdapter.setShowSubmissions(false);
            playerListAdapter.notifyDataSetChanged();

            // Make sure words recycler view is visible and history is hidden during active round
            wordsRecyclerView.setVisibility(View.VISIBLE);
            wordHistoryRecyclerView.setVisibility(View.GONE);

            // Start timer
            long duration = (round.getEndTime() - round.getStartTime());
            startTimer(duration);

            // Clear input and enable it for new round
            wordInputEditText.setText("");
            wordInputEditText.setEnabled(true);
            submitButton.setEnabled(true);
            isRoundActive = true;

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

            // Show player submissions in the UI
            updatePlayerSubmissions();

            // Display round end message
            Toast.makeText(this, "Round ended! Next round starting soon...", Toast.LENGTH_SHORT).show();

            // Add logging
            Log.d("Telepathy", "Round ended. gameId=" + gameId);
            System.out.println("TELEPATHY_DEBUG: Round ended. gameId=" + gameId);

            // Start countdown for next round (visible to all players)
            startNextRoundCountdown();

            // No need to check for host - only the host should trigger the next round from Firebase
            // The Firebase listener in all clients will receive the update and start the round
        });
    }

    // New method to display countdown for next round
    private void startNextRoundCountdown() {
        // Show the timer area for countdown
        timerTextView.setVisibility(View.VISIBLE);

        // Change the color to indicate it's a different countdown
        timerTextView.setBackgroundResource(R.drawable.timer_background);

        // Start a 5-second countdown
        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                timerTextView.setText("Next round in: " + seconds);
            }

            @Override
            public void onFinish() {
                timerTextView.setText("Starting...");
            }
        }.start();
    }

    private void updatePlayerSubmissions() {
        // Find duplicate words
        Map<String, Integer> wordCounts = new HashMap<>();
        for (Player player : players) {
            String word = player.getCurrentWord();
            if (word != null && !word.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                } else {
                    Integer count = wordCounts.get(word);
                    wordCounts.put(word, (count == null) ? 1 : count + 1);
                }
            }
        }

        // Update the player adapter to show submissions
        playerListAdapter.setShowSubmissions(true);
        playerListAdapter.setDuplicateWords(wordCounts);
        playerListAdapter.notifyDataSetChanged();

        // Add submitted words to the word history
        for (Player player : players) {
            String word = player.getCurrentWord();
            if (word != null && !word.isEmpty()) {
                // Check if this word is already in the used words list
                boolean alreadyAdded = false;
                for (String usedWord : usedWords) {
                    if (usedWord.equalsIgnoreCase(word)) {
                        alreadyAdded = true;
                        break;
                    }
                }

                // Only add unique words to the list
                if (!alreadyAdded) {
                    // Add information if this is a duplicate word
                    boolean isDuplicate = wordCounts.getOrDefault(word, 0) > 1;
                    String displayWord = word;
                    if (isDuplicate) {
                        displayWord = "⚠️ " + word + " (" + wordCounts.get(word) + " players)";
                    } else {
                        displayWord = "✓ " + word;
                    }
                    usedWords.add(displayWord);
                }
            }
        }

        // Switch views to show the history
        wordsRecyclerView.setVisibility(View.GONE);
        wordHistoryRecyclerView.setVisibility(View.VISIBLE);

        // Update adapters
        wordListAdapter.notifyDataSetChanged();
        wordHistoryAdapter.notifyDataSetChanged();
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
            if (countDownTimer != null) countDownTimer.cancel();

            Log.d("TELEPATHY", "Game ended. isHost=" + isHost + ", lobbyId=" + lobbyId);

            if (isHost && lobbyId != null) {
                firebaseController.deleteLobby(lobbyId, new FirebaseController.FirebaseCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d("TELEPATHY", "Lobby deleted successfully");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("TELEPATHY", "Failed to delete lobby: " + error);
                    }
                });
            }

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
        if (isFinishing() || isDestroyed()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over!");

        String message;
        if (winner != null) {
            message = winner.getId().equals(playerId)
                    ? "Congratulations! You won the game!"
                    : winner.getUsername() + " has won the game!";
        } else {
            message = "The game has ended.";
        }

        builder.setMessage(message);
        builder.setCancelable(false);

        dialog = builder.create();

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Back to Main Menu", (d, which) -> {
            // Do nothing here – we'll handle finish() in onDismiss
        });

        dialog.setOnDismissListener(d -> {
            // Now it's safe to navigate away
            if (!isFinishing() && !isDestroyed()) {
                Intent intent = new Intent(GameActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        dialog.show();
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
        // Dismiss any dialog if open
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}