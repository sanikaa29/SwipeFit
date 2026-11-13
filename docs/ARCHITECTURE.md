# Architecture

SwipeFit is a single-module Android app built with **MVVM**. The UI is Jetpack Compose, state flows through Kotlin `StateFlow`, and Firebase is both the backend and the real-time sync layer. There is no custom server.

```
┌──────────────┐   StateFlow    ┌───────────────┐   Flow / suspend   ┌──────────────────┐
│  Compose UI  │ ◀───────────── │   ViewModel   │ ◀───────────────── │   Repositories   │
│  (Screen.kt) │ ──── events ─▶ │ (per screen)  │ ──── calls ──────▶ │ (data/*.kt)      │
└──────────────┘                └───────────────┘                    └────────┬─────────┘
                                                                              │ Firebase SDK
                                                         ┌────────────────────┼────────────────────┐
                                                         ▼                    ▼                    ▼
                                                   Anonymous Auth         Firestore            Storage
                                                   (who you are)    (groups, votes, live)   (outfit photos)
```

## Layers

| Layer | Location | Responsibility |
|---|---|---|
| **UI** | `ui/<feature>/*Screen.kt` | Stateless-ish composables. They render `UiState` and send user events to the ViewModel. |
| **ViewModel** | `ui/<feature>/*ViewModel.kt` | Combines repository flows into one `UiState`, and runs actions in `viewModelScope`. Survives rotation. |
| **Repository** | `data/*Repository.kt` | The only code that talks to Firebase. Exposes `Flow`s for live data and `suspend` functions for writes. |
| **Model** | `data/Models.kt` | Plain data classes plus `DocumentSnapshot` mappers. |
| **Domain logic** | `data/Leaderboard.kt` | Pure Kotlin ranking function, easy to unit test. |
| **DI** | `SwipeFitApp.kt` | A tiny hand-written `AppContainer` that creates each repository once. |

### Design system

All visual tokens live in `ui/theme/Theme.kt`: colors for light and dark mode, typography, shapes, and the `HotGradient` brand brush. Reusable pieces such as `PrimaryButton`, `SecondaryButton`, `SwipeFitTopBar`, `Avatar`, `IconBubble` and `MessagePane` live in `ui/common/Components.kt`, so every screen looks consistent.

## Navigation

`ui/navigation/NavGraph.kt` defines five routes. Every route except Welcome carries the `groupId`:

```
welcome ──▶ dashboard/{groupId} ──┬──▶ swipe/{groupId} ──▶ leaderboard/{groupId}
                                  ├──▶ leaderboard/{groupId}
                                  └──▶ upload/{groupId}
```

## Data model (Firestore)

| Path | Fields |
|---|---|
| `users/{uid}` | `displayName`, `updatedAt` |
| `groups/{code}` | `name`, `inviteCode`, `ownerId`, `createdAt` (the document ID **is** the invite code) |
| `groups/{code}/members/{uid}` | `userId`, `displayName`, `joinedAt` |
| `groups/{code}/outfits/{id}` | `name`, `imageUrl`, `storagePath`, `uploaderId`, `groupId`, `createdAt` |
| `groups/{code}/votes/{outfitId_uid}` | `userId`, `groupId`, `outfitId`, `vote` (1 = Hot, 0 = Not), `createdAt` |

Photos are stored in Firebase Storage at `groups/{groupId}/outfits/{outfitId}.jpg`. They are resized on the device to at most 1280 px and saved as JPEG at 85% quality before upload.

### Why this shape works

- **One vote per person per outfit, enforced by the server.** The vote document ID is `<outfitId>_<userId>`, and the rules forbid updates and deletes, so a second vote is rejected by Firestore itself, not just by the app.
- **Everything is group-scoped.** All data sits under `groups/{code}`, so every query and listener is naturally limited to one group.
- **Membership is a document.** A member is anyone with a `members/{uid}` doc, and the rules check it with `exists()`.
- **The swipe queue is derived, not stored.** It is *all outfits minus the ones I've voted on*, so already-voted outfits never come back, even after a reinstall on the same account.
- **Live leaderboard.** One listener on `votes` plus one on `outfits` recomputes the ranking whenever anyone votes or uploads.

## Ranking rules

Hot Score = `hotVotes / totalVotes × 100`. Outfits are sorted by:

1. Rated outfits before unrated ones.
2. Higher Hot Score.
3. More total votes (when scores tie).
4. Name, alphabetically (so the order is stable).

The top three rated outfits are shown on the podium.

## Security rules summary

| Collection | Read | Create | Update / Delete |
|---|---|---|---|
| `users/{uid}` | self only | self only, name 1–30 chars | ✗ / ✗ |
| `groups/{code}` | single `get` by code (needed to join), no listing | signed in, code format checked, owner = self | ✗ |
| `members/{uid}` | members | add **yourself** to an existing group | ✗ |
| `outfits/{id}` | members | members, uploader = self, validated fields | ✗ |
| `votes/{id}` | members | members, ID must be `outfitId_myUid`, outfit must exist | ✗ |
| Storage photos | members | members, image/*, < 5 MB | ✗ |

## Known limitations

These are deliberate MVP trade-offs:

- **Leaderboard counts are computed on the device** from the group's vote documents. That's fine for friend-sized groups. For large groups, maintain counters with Cloud Functions.
- **Invite codes are the only gate.** Codes are 6 characters from a 32-character alphabet (about 1 billion combinations). Owner approval could be added later.
- **Photo URLs are shareable.** Firebase download URLs contain a token, so anyone holding the link can view the image. Storage rules still protect uploads and SDK reads.
- **No editing or deleting** of outfits and votes. The rules block it by design.
- **Anonymous identity is per install.** Linking to Google sign-in would let people keep their votes across devices.
