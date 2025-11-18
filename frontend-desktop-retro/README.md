# 🎵 MoodTunes - JavaFX Desktop Application

A mood-based music recommender desktop application built with JavaFX.

## 📋 Features

- ✅ Retro-styled welcome screen
- ✅ 6 mood categories (Happy, Sad, Energetic, Calm, Romantic, Focus)
- ✅ Dynamic mood selection with gradient cards
- ✅ Playlist generation based on mood
- ✅ Mini player with play/pause/skip controls
- ✅ Smooth transitions and animations

## 🛠️ Technologies

- **Java 17+**
- **JavaFX 21**
- **Maven** (build tool)
- **FXML** (UI markup)
- **CSS** (styling)

## 📁 Project Structure

```
MoodTunes-JavaFX/
│
├── 📄 pom.xml                           # Maven configuration
├── 📄 README.md                         # Project documentation
│
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── moodtunes/
        │           ├── 📄 Main.java                    # Application entry point
        │           │
        │           ├── controllers/
        │           │   ├── 📄 WelcomeController.java         # Welcome screen logic
        │           │   ├── 📄 MoodSelectionController.java   # Mood selection logic
        │           │   ├── 📄 PlaylistController.java        # Playlist & player logic
        │           │   └── 📄 SettingsController.java        # Settings dialog logic
        │           │
        │           ├── models/
        │           │   ├── 📄 Mood.java                      # Mood data model
        │           │   └── 📄 Song.java                      # Song data model
        │           │
        │           └── utils/
        │               └── 📄 SceneManager.java              # Scene navigation manager
        │
        └── resources/
            ├── views/
            │   ├── 📄 welcome.fxml                  # Welcome screen UI
            │   ├── 📄 mood-selection.fxml           # Mood selection UI
            │   ├── 📄 playlist.fxml                 # Playlist screen UI
            │   └── 📄 settings.fxml                 # Settings dialog UI
            │
            └── css/
                └── 📄 styles.css                    # Global CSS styles

```

## 🚀 How to Run

### Prerequisites

1. **Java JDK 17 or higher**
   ```bash
   java -version
   ```

2. **Maven**
   ```bash
   mvn -version
   ```

3. **JavaFX SDK** (Maven will download automatically)

### Running the Application

#### Option 1: Using Maven

```bash
# Navigate to project directory
cd MoodTunes-JavaFX

# Clean and compile
mvn clean compile

# Run the application
mvn javafx:run
```

#### Option 2: Using IntelliJ IDEA

1. Open IntelliJ IDEA
2. File → Open → Select `MoodTunes-JavaFX` folder
3. Wait for Maven to import dependencies
4. Right-click on `Main.java`
5. Run 'Main.main()'

#### Option 3: Build JAR and Run

```bash
# Build executable JAR
mvn clean package

# Run the JAR
java -jar target/moodtunes-javafx-1.0.0.jar
```

## 🎨 Design Theme

- **Color Palette:**
  - Primary: Hot Pink (#FF69B4)
  - Secondary: Purple (#B565D8)
  - Accents: Gold, Blue, Teal, etc.
  
- **Style:** Retro/Vintage 80s-90s aesthetic with gradients

## 📱 Screens

### 1. Welcome Screen
- Retro window design
- "MOODTUNES.EXE" title
- START button
- Fade-in animation

### 2. Mood Selection
- 6 mood cards with emojis
- Gradient backgrounds
- Selection highlighting
- Generate Playlist button

### 3. Playlist Screen
- Song list with artist and duration
- Click to play functionality
- Mini player at bottom
- Play/Pause/Skip controls

## 🔧 Customization

### Change Colors

Edit `styles.css` or inline styles in FXML files.

### Add New Moods

Edit `MoodSelectionController.java`:
```java
moods.add(new Mood("YourMood", "🎸", "Description",
    Color.web("#COLOR1"), Color.web("#COLOR2")));
```

### Modify Playlists

Edit `PlaylistController.java` in `generatePlaylistForMood()` method.

## 🎯 Current Status

**Phase 1: UI Complete** ✅
- All screens designed and functional
- Navigation working
- Retro aesthetic implemented

**Phase 2: Audio Integration** ⏳
- TODO: Implement actual audio playback
- TODO: Add music API integration
- TODO: Implement mood-matching algorithm

## 📝 Code Structure

### Main.java
- Application entry point
- Stage setup

### SceneManager.java
- Handles scene transitions
- Manages navigation

### Controllers
- `WelcomeController.java` - Welcome screen logic
- `MoodSelectionController.java` - Mood selection and card creation
- `PlaylistController.java` - Playlist display and mini player

### Models
- `Mood.java` - Mood data model
- `Song.java` - Song data model

## 🐛 Troubleshooting

### Issue: "Module not found" error
**Solution:** Make sure Java 17+ with module support

### Issue: FXML not loading
**Solution:** Check resource paths in `SceneManager.java`

### Issue: CSS not applying
**Solution:** Verify CSS file location in resources folder

### Issue: Maven dependencies not downloading
**Solution:**
```bash
mvn clean install -U
```

## 📚 Dependencies

All dependencies are managed by Maven:
- JavaFX Controls
- JavaFX FXML
- JavaFX Media (for future audio)
- JavaFX Web (optional)

## 🔮 Future Enhancements

- [ ] Real audio playback with JavaFX Media
- [ ] Spotify/Apple Music API integration
- [ ] User preferences and favorites
- [ ] Playlist saving
- [ ] Dark mode theme
- [ ] Custom mood creation
- [ ] Audio visualizer
- [ ] Shuffle and repeat modes

## 👥 Team Members

- Member 1: UI/Frontend (JavaFX screens and styling)
- Member 2: Backend/Audio (Audio playback and API integration)

## 📄 License

Educational project for CS4750

## 🙏 Acknowledgments

- JavaFX community
- Retro UI design inspiration
- Open source libraries

---

**Built with ❤️ using JavaFX**

Version: 1.0.0 - Phase 1 Complete
Last Updated: January 2025
