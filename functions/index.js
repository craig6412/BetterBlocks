const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize the admin SDK (the runtime will provide credentials).
admin.initializeApp();

const HIGHSCORE_TOPIC = 'highscores';

/**
 * Cloud Function: notifyOnLeaderboardUpdate
 * Triggers when a document in Firestore collection 'leaderboards' (document id = userId) is created/updated.
 * If the new score is higher than the previous score, or tier increased, it sends an FCM topic message to
 * all devices subscribed to the 'highscores' topic with a data payload containing playerName, score, tier.
 */
exports.notifyOnLeaderboardUpdate = functions.firestore
  .document('leaderboards/{userId}')
  .onWrite(async (change, context) => {
    const before = change.before.exists ? change.before.data() : null;
    const after = change.after.exists ? change.after.data() : null;
    if (!after) return null; // deleted document

    const userId = context.params.userId || '';
    const name = after.playerName || after.name || 'Player';
    const newScore = after.score || 0;
    const newTier = after.trophyTier || after.tier || '';

    let shouldNotify = false;
    let title = 'New Highscore!';
    let body = `${name} scored ${newScore}`;

    if (before) {
      const oldScore = before.score || 0;
      const oldTier = before.trophyTier || before.tier || '';
      if (newScore > oldScore) {
        shouldNotify = true;
        title = `${name} got a new highscore`;
        body = `${name} beat their previous score: ${newScore}`;
      } else if (newTier && oldTier && newTier !== oldTier) {
        // tier names changed (e.g., GOLD -> PLATINUM)
        shouldNotify = true;
        title = `${name} advanced to ${newTier}`;
        body = `${name} reached tier ${newTier} — score: ${newScore}`;
      }
    } else {
      // New entry — optionally notify (we choose NOT to notify on initial create)
      shouldNotify = false;
    }

    if (!shouldNotify) return null;

    const message = {
      topic: HIGHSCORE_TOPIC,
      notification: {
        title: title,
        body: body,
      },
      data: {
        playerName: String(name),
        score: String(newScore),
        tier: String(newTier),
        userId: String(userId),
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log('Sent FCM message:', response);
      return response;
    } catch (err) {
      console.error('Error sending FCM message:', err);
      return null;
    }
  });

