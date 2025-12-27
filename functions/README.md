# BetterBlocks Cloud Functions

This folder contains a Firebase Cloud Function that broadcasts highscore/tier updates to an FCM topic `highscores`.

Files:
- `index.js` - Cloud Function `notifyOnLeaderboardUpdate` which triggers on `leaderboards/{userId}` Firestore writes.
- `package.json` - Node 18 dependencies for Firebase Functions and Admin SDK.

Deploy steps (local machine with Firebase CLI installed and authenticated):

1. Install dependencies:

```bash
cd functions
npm install
```

2. Initialize/choose Firebase project (if you haven't already):

```bash
firebase login
firebase use --add
```

3. Deploy functions only:

```bash
firebase deploy --only functions:notifyOnLeaderboardUpdate
```

Testing:
- Write a document to `leaderboards/{userId}` with fields `score`, `playerName`, and `trophyTier`.
- The function will compare the previous and new values; if the score increased or tier changed it will publish
  a message to topic `highscores` with a data payload.
- Devices/subscribers will receive the message via FCM; the Android app subscribes to `highscores` on startup.

Notes:
- Ensure your Firebase project has Cloud Messaging enabled and the Android app is registered in the project (with `google-services.json`).
- Cloud Functions billing: the free Spark tier supports functions but has limits; consider Blaze for production.

