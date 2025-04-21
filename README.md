# Telepathy

Telepathy is a multiplayer word game for Android where players try to choose unique words from a category. If two or more players pick the same word, they lose a life. The last player standing wins!

## Features

- **User Authentication**: Create an account or log in to track your game history
- **Create or Join Lobbies**: Host your own game or join existing ones
- **Multiple Word Categories**: Choose from Animals, Countries, Foods, and Sports
- **Real-time Gameplay**: Interact with other players in real-time
- **Customizable Settings**: Adjust time limits, lives, and other game parameters
- **Leaderboards**: See who's winning and track scores

## Installation

### Option 1: Install APK directly

1. Download the latest APK file from the [Releases](https://github.com/yourusername/telepathy/releases) page
2. On your Android device, enable installation from unknown sources:
   - Go to Settings > Security > Install unknown apps
   - Select your file browser and toggle "Allow from this source"
3. Navigate to the downloaded APK file and tap to install
4. Follow the on-screen instructions to complete installation

### Option 2: Build from source

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/telepathy.git
   ```

2. Open the project in Android Studio (2023.3 or newer)

3. Set up Firebase:
   - Create a new Firebase project at [firebase.google.com](https://firebase.google.com)
   - Add an Android app to your Firebase project with package name `com.example.telepathy`
   - Download the `google-services.json` file and place it in the `app/` directory
   - Enable Authentication, Realtime Database and Crashlytics in your Firebase console

4. Build the project:
   - Select "Build > Make Project" from the menu
   - Wait for Gradle to download dependencies and build the project

5. Run on a device or emulator:
   - Connect an Android device to your computer or start an emulator
   - Select "Run > Run 'app'" from the menu
   - Choose your device from the list and click OK

## How to Play

1. **Create an Account**: Sign up with your email and password
2. **Create or Join a Lobby**: 
   - Create your own game lobby with custom settings
   - Or join an existing lobby from the list
3. **Start the Game**: The lobby host can start the game when ready
4. **Each Round**:
   - All players are shown a category (e.g., Animals)
   - Each player submits a word from that category within the time limit
   - If your word is unique (no other player chose it), you're safe
   - If two or more players choose the same word, they each lose a life
   - Players with unique words earn 10 points
5. **Game End**: 
   - Players are eliminated when they lose all lives
   - Last player standing wins!
   - Or highest score when the game ends

## Requirements

- Android 7.0 (API level 24) or higher
- Internet connection for multiplayer functionality
- ~50MB of free storage space

## Troubleshooting

- **Can't connect to a game**: Check your internet connection and ensure the app has network permissions
- **Game freezes**: Try closing and reopening the app
- **Login issues**: Verify your email and password are correct, or use the password reset option

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Icons and design elements from [Material Design](https://material.io)
- Word lists compiled from various public domain sources
