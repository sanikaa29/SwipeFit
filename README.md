<div align="center">

# ❤️ SwipeFit

**Swipe. Vote. Rank the fits.**

A Tinder-style Android app where friend groups vote **Hot** or **Not** on each other's outfits and watch a live leaderboard crown the best look.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20Storage-FFCA28?logo=firebase&logoColor=black)
![Min SDK](https://img.shields.io/badge/minSdk-28-3DDC84?logo=android&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue)

</div>

---

## Features

- **No sign-up.** Anonymous Firebase Auth gives every install an identity instantly.
- **Private groups.** Create a group and invite friends with a 6-character code you can copy or share.
- **Swipe to vote.** Drag right for Hot or left for Not, with rotating cards, HOT/NOT stamps, a color wash and haptic feedback.
- **One vote per outfit, enforced by the server.** Security rules make double-voting impossible, not just hidden.
- **Live rankings.** A podium for the top three and Hot Score bars that re-rank in real time as anyone votes.
- **Photo uploads.** Uses the system photo picker (no storage permission), then resizes and EXIF-rotates the image on the device before upload.
- **Light and dark mode.** A full Material 3 design system with a pink-to-coral brand gradient.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM with `StateFlow`, repository pattern, manual DI |
| Async | Kotlin Coroutines and Flow (`callbackFlow` around Firestore listeners) |
| Backend | Firebase Anonymous Auth, Cloud Firestore, Cloud Storage |
| Images | Coil (loading), `ImageDecoder` (resize and rotate before upload) |
| Build | Gradle 8.9 (Kotlin DSL), AGP 8.7, compileSdk 35, minSdk 28 |

## Project structure

```
SwipeFit/
├── app/
│   ├── build.gradle.kts                  # App module: SDK levels, dependencies
│   ├── proguard-rules.pro
│   ├── google-services.json              # ← you add this (git-ignored), see docs/SETUP.md
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── drawable/                 # Adaptive icon layers (gradient + heart)
│       │   ├── mipmap-anydpi-v26/        # Adaptive / themed launcher icon
│       │   ├── values/                   # Strings, colors, light window theme
│       │   └── values-night/             # Dark window theme
│       └── java/com/swipefit/
│           ├── SwipeFitApp.kt            # Application + AppContainer (creates repositories)
│           ├── MainActivity.kt           # Edge-to-edge host for Compose
│           ├── data/                     # ── Data layer (only place that touches Firebase)
│           │   ├── Models.kt             #    Group, Member, Outfit, Vote + Firestore mappers
│           │   ├── AuthRepository.kt     #    Anonymous sign-in
│           │   ├── GroupRepository.kt    #    Create/join groups, invite codes, members
│           │   ├── OutfitRepository.kt   #    Upload photos, list outfits, sample data
│           │   ├── VoteRepository.kt     #    Cast votes, live vote stream
│           │   ├── Leaderboard.kt        #    Pure ranking logic (Hot Score)
│           │   ├── FirestoreFlows.kt     #    Snapshot listeners → Kotlin Flow
│           │   └── ImageUtils.kt         #    Resize, rotate and compress to JPEG
│           ├── util/
│           │   └── Errors.kt             # Friendly error messages + network timeouts
│           └── ui/                       # ── UI layer (one folder per feature)
│               ├── theme/Theme.kt        #    Colors (light/dark), typography, shapes, gradient
│               ├── common/Components.kt  #    Buttons, top bar, avatar, empty/error states
│               ├── navigation/NavGraph.kt#    Routes and ViewModel factories
│               ├── welcome/              #    Name entry, create/join group
│               ├── dashboard/            #    Invite card, stats, members, actions
│               ├── upload/               #    Photo picker + upload progress
│               ├── swipe/                #    Swipeable card deck
│               └── leaderboard/          #    Podium + live rankings
├── docs/
│   ├── SETUP.md                          # Full Firebase + Android Studio setup
│   └── ARCHITECTURE.md                   # Layers, data model, rules, trade-offs
├── gradle/wrapper/                       # Gradle wrapper (pins Gradle 8.9)
├── firestore.rules                       # Firestore security rules
├── storage.rules                         # Storage security rules
├── build.gradle.kts                      # Root: plugin versions
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── CHANGELOG.md
├── CONTRIBUTING.md
└── LICENSE
```

Every feature follows the same pattern: **`XScreen.kt`** (Compose UI) plus **`XViewModel.kt`** (state and actions).

## Getting started

**Short version:**

1. Clone the repo and open it in Android Studio.
2. Create a Firebase project and add an Android app with package name `com.swipefit`.
3. Put the downloaded `google-services.json` in `app/`.
4. In Firebase, enable **Anonymous Auth**, **Firestore** and **Storage**, then publish `firestore.rules` and `storage.rules`.
5. Sync Gradle and press **Run ▶**.

📖 **Full step-by-step guide, including multiplayer testing and troubleshooting:** [docs/SETUP.md](docs/SETUP.md)

## How it works

```
Create / join group  ──▶  Upload outfits  ──▶  Everyone swipes  ──▶  Live leaderboard
   (invite code)         (photo → Storage)    (1 vote each, rules)    (podium + scores)
```

- Each vote is stored at `groups/{code}/votes/{outfitId}_{userId}`. The document ID makes duplicate votes impossible, and the rules forbid editing or deleting votes.
- The swipe queue is *all outfits minus the ones you've voted on*, worked out live, so finished outfits never come back.
- **Hot Score** = Hot votes ÷ total votes × 100. Ties are broken by vote count, then by name.

More detail, including the data model and security rules: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Roadmap

- [ ] Google sign-in, so votes follow you across devices
- [ ] Group owner can remove outfits or members
- [ ] Push notifications when new outfits are posted
- [ ] Cloud Functions for vote counters in large groups
- [ ] Unit tests for `Leaderboard.kt` and the ViewModels

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first.

## License

Released under the [MIT License](LICENSE). © 2026 Sanika Ajgaonkar
