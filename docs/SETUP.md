# Setup guide

Everything you need to build SwipeFit and run it against your own Firebase project.

## Requirements

| Tool | Version |
|---|---|
| Android Studio | Latest stable (Ladybug or newer) |
| JDK | 17+ (bundled with Android Studio) |
| Android SDK | API 35 (compile), device/emulator on API 28+ |
| Firebase | Free Spark plan works for everything except photo upload (see note below) |

## 1. Open the project

1. Clone the repo:
   ```bash
   git clone https://github.com/<your-username>/SwipeFit.git
   ```
2. In Android Studio choose **File → Open** and select the `SwipeFit` folder.
3. Let Gradle sync. The wrapper downloads Gradle 8.9 automatically.
4. The first sync fails with *"File google-services.json is missing"*. That is expected. Do step 2, then sync again.

## 2. Create a Firebase project

1. Go to [console.firebase.google.com](https://console.firebase.google.com) and click **Add project**.
2. **Project overview → Add app → Android**
   - Package name: `com.swipefit`
   - Download `google-services.json` and place it at `app/google-services.json`.
   - This file is listed in `.gitignore` and must **not** be committed.
3. **Build → Authentication → Get started → Sign-in method → Anonymous → Enable.**
4. **Build → Firestore Database → Create database** (production mode).
   Open the **Rules** tab, paste the contents of [`firestore.rules`](../firestore.rules), and click **Publish**.
5. **Build → Storage → Get started.**
   Open the **Rules** tab, paste the contents of [`storage.rules`](../storage.rules), and click **Publish**.

> **Storage and billing:** new Firebase projects may need the Blaze (pay-as-you-go) plan before the default Storage bucket can be created. It has a generous free monthly allowance but requires a billing account. Everything except photo upload (groups, sample outfits, swiping, voting, rankings) works without Storage.

6. Back in Android Studio, **Sync Project with Gradle Files**.

### Optional: deploy rules from the command line

If you use the Firebase CLI you can deploy the rules instead of pasting them:

```bash
npm install -g firebase-tools
firebase login
firebase use --add            # pick your project
firebase deploy --only firestore:rules,storage
```

(This needs a `firebase.json` that points at the two rule files; `firebase init` can create one.)

## 3. Run on an emulator

1. **Tools → Device Manager → Create Device**, for example a Pixel 8 with an API 34 *Google APIs* image.
2. Select it in the toolbar and press **Run ▶**.
3. On the welcome screen enter a display name, tap **Create a group**, and name it.
4. On the dashboard tap **Add sample outfits (for testing)** (these are random stock photos), then **Start swiping**.
5. To test real uploads, drag a photo onto the emulator window (it lands in *Downloads*), then go to **Upload → Tap to choose a photo**.

A good order to verify things in:

1. Creating a group works.
2. Sample outfits appear and you can swipe through them.
3. Votes survive an app restart (outfits you already swiped don't come back).
4. Rankings and the podium update.
5. Photo upload works.

## 4. Test multiplayer

1. Run the app on two emulators, or on an emulator and a real phone.
2. **Device A:** create a group, add sample outfits, and tap **Share** or **Copy** on the invite card.
3. **Device B:** enter a different name, tap **Join with a code**, and type the code. Device A's member count goes to 2.
4. Swipe differently on each device. Open **Rankings** on one and keep swiping on the other: the list re-ranks live.

Each install gets its own anonymous identity, so clearing app data or reinstalling creates a new voter.

## 5. Build a release APK

```bash
./gradlew assembleRelease
```

The unsigned APK is written to `app/build/outputs/apk/release/`. To publish on Google Play, set up [app signing](https://developer.android.com/studio/publish/app-signing) and keep your keystore out of git (it's already covered by `.gitignore`).

## Troubleshooting

| Symptom | Fix |
|---|---|
| `File google-services.json is missing` | Download it from the Firebase console into `app/`. |
| "Sign-in failed … Anonymous sign-in" | Enable **Anonymous** in Firebase Authentication. |
| "Permission denied" | Publish `firestore.rules`, and make sure you joined the group. |
| Upload fails or hangs | Create the Storage bucket and publish `storage.rules`. |
| Images don't load on the emulator | Make sure the emulator has internet access (try opening a site in Chrome). |
| Gradle sync fails on a fresh machine | **File → Invalidate Caches → Restart**, then sync again. |
