const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_1";
const BOB_UID = "bob_viewer_2";
const ADMIN_UID = "admin_janz_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid, email = "alice@example.com") {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    email,
    username: "alice_janz",
    displayName: "Alice Sharma",
    themeMode: "dark",
    preferredLanguage: "Hindi",
    interests: ["Music", "Technology"],
    notifyLikes: true,
    notifyComments: true,
    notifyFollowers: true,
    notifyUploads: true,
    blockedUserIds: [],
    watchLaterVideoIds: [],
    savedVideoIds: [],
    accountStatus: "active",
    createdAt: now,
    updatedAt: now,
  };
}

function validCreatorPayload(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    creatorId: uid,
    displayName: "Alice Sharma",
    username: "alice_janz",
    bio: "Indian tech and music creator",
    avatarUrl: "",
    coverUrl: "",
    socialLink: "",
    verified: false,
    accountStatus: "active",
    followersCount: 0,
    followingCount: 0,
    totalViews: 0,
    totalVideos: 0,
    totalWatchTimeSec: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validVideoPayload(creatorId) {
  const now = new Date(Date.now() - 1000);
  return {
    creatorId,
    creatorName: "Alice Sharma",
    creatorUsername: "alice_janz",
    creatorAvatarUrl: "",
    creatorVerified: false,
    title: "Jaipur Heritage Vlog",
    description: "Exploring Jaipur forts in 4K.",
    videoUri: "https://example.com/video.mp4",
    thumbnailUri: "",
    shortForm: false,
    featured: false,
    trendingPinned: false,
    category: "Travel",
    tags: ["jaipur", "rajasthan"],
    language: "Hindi",
    visibility: "public",
    moderationStatus: "approved",
    processingStatus: "ready",
    durationSec: 120,
    viewsCount: 0,
    likesCount: 0,
    commentsCount: 0,
    savesCount: 0,
    watchTimeTotalSec: 0,
    createdAt: now,
    updatedAt: now,
  };
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("videos").where("visibility", "==", "public").where("moderationStatus", "==", "approved").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("creators").doc(ALICE_UID).get());
});

test("User PII isolation and self-privilege escalation prevention", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("creators").doc(ALICE_UID).set(validCreatorPayload(ALICE_UID)));

  // Bob cannot read Alice's private user PII doc
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  // Bob can read Alice's public creator doc
  await assertSucceeds(bobDb.collection("creators").doc(ALICE_UID).get());

  // Alice cannot self-verify or self-assign ghost fields
  await assertFails(aliceDb.collection("creators").doc(ALICE_UID).update({
    verified: true,
    updatedAt: new Date(Date.now() - 500),
  }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set({
    ...validUserPayload(ALICE_UID),
    role: "admin",
  }));
});

test("Video creation, public feed query, private video isolation, and engagement counters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("creators").doc(ALICE_UID).set(validCreatorPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("videos").doc("vid_pub").set(validVideoPayload(ALICE_UID)));

  // Private video created by Alice
  const privPayload = { ...validVideoPayload(ALICE_UID), visibility: "private", title: "Private Draft" };
  await assertSucceeds(aliceDb.collection("videos").doc("vid_priv").set(privPayload));

  // Unfiltered list on videos must fail
  await assertFails(bobDb.collection("videos").get());

  // Filtered public approved feed succeeds for Bob
  await assertSucceeds(
    bobDb.collection("videos")
      .where("visibility", "==", "public")
      .where("moderationStatus", "==", "approved")
      .get()
  );

  // Bob cannot read Alice's private video directly
  await assertFails(bobDb.collection("videos").doc("vid_priv").get());

  // Bob can like & increment viewsCount, but cannot edit video title
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("likes").doc("vid_pub").set({
      userId: BOB_UID,
      videoId: "vid_pub",
      createdAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    bobDb.collection("videos").doc("vid_pub").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertFails(
    bobDb.collection("videos").doc("vid_pub").update({
      title: "Hijacked Title",
      updatedAt: new Date(Date.now() - 400),
    })
  );
});

test("Comments creation, creator comment moderation, and spoofed admin email rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: "mdayubjanj@gmail.com",
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "mdayubjanj@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(aliceDb.collection("videos").doc("vid_1").set(validVideoPayload(ALICE_UID)));

  const commentPayload = {
    videoId: "vid_1",
    authorId: BOB_UID,
    authorName: "Bob",
    authorAvatarUrl: "",
    text: "Great video!",
    parentCommentId: "",
    likesCount: 0,
    pinned: false,
    createdAt: new Date(Date.now() - 500),
    updatedAt: new Date(Date.now() - 500),
  };
  await assertSucceeds(
    bobDb.collection("videos").doc("vid_1").collection("comments").doc("c_1").set(commentPayload)
  );

  // Query aligned with rules
  await assertSucceeds(
    bobDb.collection("videos").doc("vid_1").collection("comments").where("videoId", "==", "vid_1").get()
  );

  // Video creator Alice can pin or delete Bob's comment
  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_1").collection("comments").doc("c_1").update({
      pinned: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Unverified email spoofing admin fails
  await assertFails(
    spoofedAdminDb.collection("videos").doc("vid_1").update({
      featured: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Verified bootstrapped admin succeeds
  await assertSucceeds(
    realAdminDb.collection("videos").doc("vid_1").update({
      featured: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});
