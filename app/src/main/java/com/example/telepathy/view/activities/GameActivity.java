package com.example.telepathy.view.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.telepathy.model.Player;
import com.example.telepathy.view.adapters.PlayerListAdapter;
import com.example.telepathy.view.adapters.WordHistoryAdapter;
import com.example.telepathy.view.adapters.WordListAdapter;

import java.util.ArrayList;
import java.util.Collections;
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
        wordHistoryAdapter = new WordHistoryAdapter();
        wordHistoryRecyclerView.setAdapter(wordHistoryAdapter);

        // Make sure word history is visible
        wordHistoryRecyclerView.setVisibility(View.VISIBLE);
        View wordHistoryLabel = findViewById(R.id.wordHistoryLabel);
        if (wordHistoryLabel != null) {
            wordHistoryLabel.setVisibility(View.VISIBLE);
        }

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

        // Add word to history
        wordHistoryAdapter.addWord(word);

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

                // Important: Tell the server the round has ended due to timer expiration
                if (gameController != null) {
                    gameController.handleTimerExpired();
                }
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

            // Update available words for this round but hide them from player
            validWords.clear();
            validWords.addAll(round.getWords());

            // Hide available words
            wordsRecyclerView.setVisibility(View.GONE);

            // Add a system message for new round
            wordHistoryAdapter.addSystemMessage("Round " + round.getRoundNumber() + " started");

            // Reset player submission display
            playerListAdapter.setShowSubmissions(false);
            playerListAdapter.notifyDataSetChanged();

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

            // Update player submissions in the UI
            updatePlayerSubmissions();

            // Add other players' words to history
            for (Player player : players) {
                String word = player.getCurrentWord();
                if (word != null && !word.isEmpty() && !player.getId().equals(playerId)) {
                    // Just add the word itself, not who submitted it
                    wordHistoryAdapter.addWord(word);
                }
            }

            wordHistoryAdapter.notifyDataSetChanged();
            // Scroll to latest item
            if (!usedWords.isEmpty()) {
                wordHistoryRecyclerView.scrollToPosition(usedWords.size() - 1);
            }

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

                // Add elimination message to history
                wordHistoryAdapter.addSystemMessage(player.getUsername() + " has been eliminated!");
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

            // Add elimination message to history
            usedWords.add("SYSTEM: You have been eliminated!");
            wordHistoryAdapter.notifyDataSetChanged();
            wordHistoryRecyclerView.scrollToPosition(usedWords.size() - 1);
        });
        builder.setNegativeButton("Exit", (dialog, which) -> {
            // Leave the game
            finish();
        });
        builder.show();
    }

// In GameActivity.java - replace the showGameEndDialog method with this:

    private void showGameEndDialog(Player winner) {
        // Stop any active timers
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Hide the regular game UI
        gameControlsLayout.setVisibility(View.GONE);

        // Inflate the game end screen
        View endScreenView = getLayoutInflater().inflate(R.layout.dialog_game_over, null);

        // Add the end screen to the main container
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(endScreenView);

        // Find views in the end screen
        TextView gameEndTitleTextView = endScreenView.findViewById(R.id.gameEndTitleTextView);
        TextView winnerNameTextView = endScreenView.findViewById(R.id.winnerNameTextView);
        TextView noWinnerTextView = endScreenView.findViewById(R.id.noWinnerTextView);
        TextView finalScoreTextView = endScreenView.findViewById(R.id.finalScoreTextView);
        Button backToMenuButton = endScreenView.findViewById(R.id.backToMenuButton);
        RecyclerView finalLeaderboardRecyclerView = endScreenView.findViewById(R.id.finalLeaderboardRecyclerView);

        // Set up the leaderboard
        finalLeaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create a sorted list of players for the final leaderboard
        List<Player> sortedPlayers = new ArrayList<>(players);
        Collections.sort(sortedPlayers, (p1, p2) -> p2.getScore() - p1.getScore());

        // Use your existing player adapter or create a special one for the end screen
        PlayerListAdapter leaderboardAdapter = new PlayerListAdapter(sortedPlayers);
        finalLeaderboardRecyclerView.setAdapter(leaderboardAdapter);

        // Set appropriate content based on game outcome
        if (winner != null) {
            // We have a winner
            winnerNameTextView.setVisibility(View.VISIBLE);
            noWinnerTextView.setVisibility(View.GONE);

            if (winner.getId().equals(playerId)) {
                // Current player is the winner
                gameEndTitleTextView.setText("YOU WON!");
                winnerNameTextView.setText("Congratulations!");
            } else {
                // Another player is the winner
                gameEndTitleTextView.setText("GAME OVER");
                winnerNameTextView.setText(winner.getUsername() + " is the winner!");
            }

            // Show the winner's score
            finalScoreTextView.setText("Score: " + winner.getScore());
        } else {
            // No winner (everyone eliminated)
            gameEndTitleTextView.setText("GAME OVER");
            winnerNameTextView.setVisibility(View.GONE);
            noWinnerTextView.setVisibility(View.VISIBLE);
            finalScoreTextView.setVisibility(View.GONE);
        }

        // Set up button to return to main menu
        backToMenuButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
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