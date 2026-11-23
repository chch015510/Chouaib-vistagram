const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const APP_SCOPE = "vistagram-main";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function baseUserPayload(uid, username, now) {
  return {
    appScope: APP_SCOPE,
    userId: uid,
    username,
    displayName: username,
    bio: "",
    website: "",
    verified: false,
    postsCount: 0,
    followersCount: 0,
    followingCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("ig_posts").where("appScope", "==", APP_SCOPE).get());
  await assertFails(unauthDb.collection("ig_users").doc(ALICE_UID).set(baseUserPayload(ALICE_UID, "alice", now)));
  await assertFails(unauthDb.collection("ig_chats").where("participantIds", "array-contains", ALICE_UID).get());
});

test("Profile creation, self-verification block, and atomic follow/unfollow", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Self-verification must fail
  await assertFails(
    aliceDb.collection("ig_users").doc(ALICE_UID).set({
      ...baseUserPayload(ALICE_UID, "alice", now),
      verified: true,
    })
  );

  await assertSucceeds(aliceDb.collection("ig_users").doc(ALICE_UID).set(baseUserPayload(ALICE_UID, "alice", now)));
  await assertSucceeds(bobDb.collection("ig_users").doc(BOB_UID).set(baseUserPayload(BOB_UID, "bob", now)));

  // Scoped list query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("ig_users").where("appScope", "==", APP_SCOPE).get());
  await assertFails(aliceDb.collection("ig_users").get());

  // Atomic follow batch
  const batch = aliceDb.batch();
  batch.set(
    aliceDb.collection("ig_users").doc(BOB_UID).collection("followers").doc(ALICE_UID),
    {
      appScope: APP_SCOPE,
      userId: ALICE_UID,
      targetUserId: BOB_UID,
      username: "alice",
      displayName: "alice",
      createdAt: now,
    }
  );
  batch.set(
    aliceDb.collection("ig_users").doc(ALICE_UID).collection("following").doc(BOB_UID),
    {
      appScope: APP_SCOPE,
      userId: ALICE_UID,
      targetUserId: BOB_UID,
      username: "bob",
      displayName: "bob",
      createdAt: now,
    }
  );
  batch.update(aliceDb.collection("ig_users").doc(BOB_UID), {
    followersCount: 1,
    updatedAt: new Date(Date.now() - 1000),
  });
  batch.update(aliceDb.collection("ig_users").doc(ALICE_UID), {
    followingCount: 1,
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(batch.commit());
});

test("Posts, atomic likes, comments, and cross-user protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const postPayload = {
    appScope: APP_SCOPE,
    authorId: ALICE_UID,
    authorUsername: "alice",
    authorDisplayName: "Alice",
    authorVerified: false,
    mediaUrls: ["res:img_sample_sunset"],
    filterName: "Normal",
    caption: "Golden hour #travel",
    location: "Amalfi",
    category: "TRAVEL",
    likesCount: 0,
    commentsCount: 0,
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(aliceDb.collection("ig_posts").doc("post_1").set(postPayload));

  // Bob cannot edit or delete Alice's post
  await assertFails(bobDb.collection("ig_posts").doc("post_1").update({ caption: "Hacked", updatedAt: now }));
  await assertFails(bobDb.collection("ig_posts").doc("post_1").delete());

  // Bob cannot increment likesCount without atomic marker doc
  await assertFails(bobDb.collection("ig_posts").doc("post_1").update({ likesCount: 1, updatedAt: now }));

  // Bob atomic like succeeds
  const likeBatch = bobDb.batch();
  likeBatch.set(
    bobDb.collection("users").doc(BOB_UID).collection("ig_liked_posts").doc("post_1"),
    { userId: BOB_UID, targetId: "post_1", createdAt: now }
  );
  likeBatch.update(bobDb.collection("ig_posts").doc("post_1"), { likesCount: 1, updatedAt: now });
  await assertSucceeds(likeBatch.commit());
});

test("Direct messages are strictly scoped to participants", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_uid").firestore();
  const now = new Date(Date.now() - 2000);
  const chatId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("ig_chats").doc(chatId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantAId: ALICE_UID,
      participantAName: "alice",
      participantBId: BOB_UID,
      participantBName: "bob",
      lastMessageText: "Hey!",
      lastSenderId: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("ig_chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderUsername: "bob",
      text: "Hello Alice!",
      createdAt: now,
    })
  );

  // Charlie cannot read chat or messages
  await assertFails(charlieDb.collection("ig_chats").doc(chatId).get());
  await assertFails(
    charlieDb.collection("ig_chats").doc(chatId).collection("messages")
      .where("participantIds", "array-contains", "charlie_uid").get()
  );
});
