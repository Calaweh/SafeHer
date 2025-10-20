// functions/src/index.ts

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Initialize the Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();

/**
 * Creates a friend request. Writes to both the sender's and receiver's
 * friends subcollection.
 */
export const createFriendRequest = functions.https.onCall(async (data, context) => {
  // FINAL FIX: Use optional chaining to safely check context AND context.auth.
  if (!context?.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "You must be logged in to send a friend request.",
    );
  }
  const senderUid = context.auth.uid;

  const receiverEmail = data.email;
  if (!receiverEmail || typeof receiverEmail !== "string") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Email is required and must be a string.",
    );
  }

  const senderDoc = await db.collection("users").doc(senderUid).get();
  const senderData = senderDoc.data();

  const receiverQuery = await db.collection("users")
    .where("email", "==", receiverEmail).limit(1).get();

  if (receiverQuery.empty) {
    throw new functions.https.HttpsError(
      "not-found", `User with email ${receiverEmail} not found.`
    );
  }

  const receiverDoc = receiverQuery.docs[0];
  const receiverData = receiverDoc.data();
  const receiverUid = receiverDoc.id;

  if (!senderData || !receiverData) {
    throw new functions.https.HttpsError("internal", "User data is incomplete.");
  }
  if (senderUid === receiverUid) {
    throw new functions.https.HttpsError(
      "invalid-argument", "You cannot send a request to yourself."
    );
  }

  const batch = db.batch();

  const senderFriendRef = db.doc(`users/${senderUid}/friends/${receiverUid}`);
  batch.set(senderFriendRef, {
    id: receiverUid,
    displayName: receiverData.displayName,
    imageUrl: receiverData.imageUrl,
    status: "requesting",
    direction: "sent",
    fromUserId: senderUid,
    toUserId: receiverUid,
    deletedAt: null,
  });

  const receiverFriendRef = db.doc(`users/${receiverUid}/friends/${senderUid}`);
  batch.set(receiverFriendRef, {
    id: senderUid,
    displayName: senderData.displayName,
    imageUrl: senderData.imageUrl,
    status: "requesting",
    direction: "received",
    fromUserId: senderUid,
    toUserId: receiverUid,
    deletedAt: null,
  });

  await batch.commit();

  return {success: true, message: "Friend request sent."};
});


/**
 * Accepts a friend request. Updates both documents to "accepted".
 */
export const acceptFriendRequest = functions.https.onCall(async (data, context) => {
  // FINAL FIX: Use optional chaining to safely check context AND context.auth.
  if (!context?.auth) {
    throw new functions.https.HttpsError("unauthenticated", "You must be logged in.");
  }
  const myUid = context.auth.uid;

  const friendUid = data.friendId;
  if (!friendUid || typeof friendUid !== "string") {
    throw new functions.https.HttpsError(
      "invalid-argument", "Friend ID is required and must be a string."
    );
  }

  const myFriendRef = db.doc(`users/${myUid}/friends/${friendUid}`);
  const theirFriendRef = db.doc(`users/${friendUid}/friends/${myUid}`);

  const batch = db.batch();
  batch.update(myFriendRef, {status: "accepted"});
  batch.update(theirFriendRef, {status: "accepted"});

  await batch.commit();

  return {success: true, message: "Friend request accepted."};
});


/**
 * Deletes a friendship or rejects/cancels a request.
 * Performs a soft delete by setting the `deletedAt` timestamp.
 */
export const deleteFriend = functions.https.onCall(async (data, context) => {
  // FINAL FIX: Use optional chaining to safely check context AND context.auth.
  if (!context?.auth) {
    throw new functions.https.HttpsError("unauthenticated", "You must be logged in.");
  }
  const myUid = context.auth.uid;

  const friendUid = data.friendId;
  if (!friendUid || typeof friendUid !== "string") {
    throw new functions.https.HttpsError(
      "invalid-argument", "Friend ID is required and must be a string."
    );
  }

  const myFriendRef = db.doc(`users/${myUid}/friends/${friendUid}`);
  const theirFriendRef = db.doc(`users/${friendUid}/friends/${myUid}`);

  const batch = db.batch();
  const deleteTimestamp = admin.firestore.FieldValue.serverTimestamp();
  batch.update(myFriendRef, {deletedAt: deleteTimestamp});
  batch.update(theirFriendRef, {deletedAt: deleteTimestamp});

  await batch.commit();

  return {success: true, message: "Friend removed."};
});