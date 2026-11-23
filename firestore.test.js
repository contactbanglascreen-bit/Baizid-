const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users cannot read or write watchlist, history, or reminders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/watchlist`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/history`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/reminders`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/watchlist/item_1`).set({
      userId: ALICE_UID,
      contentId: "item_1",
      title: "Inception",
      posterUrl: "https://example.com/p.jpg",
      mediaType: "MOVIE",
      source: "moviebox",
      year: "2010",
      rating: "8.8",
      languageBadge: "English",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner: valid watchlist create, update, list, and delete succeed; shadow fields and cross-user access fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/watchlist/mb_101`).set({
      userId: ALICE_UID,
      contentId: "mb_101",
      title: "Squid Game",
      posterUrl: "https://example.com/squid.jpg",
      mediaType: "SERIES",
      source: "moviebox",
      year: "2025",
      rating: "8.5",
      languageBadge: "Hindi",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/watchlist`).get());
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/watchlist/mb_101`).get());

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/watchlist/mb_101`).update({
      rating: "8.9",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject cross-user reads and writes
  await assertFails(bobDb.collection(`users/${ALICE_UID}/watchlist`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/watchlist/mb_101`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/watchlist/mb_101`).update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId id key / future timestamp / invalid mediaType
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/watchlist/mb_102`).set({
      id: "mb_102",
      userId: ALICE_UID,
      contentId: "mb_102",
      title: "Bad Id Field",
      posterUrl: "",
      mediaType: "MOVIE",
      source: "moviebox",
      year: "2026",
      rating: "8.0",
      languageBadge: "EN",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/watchlist/mb_103`).set({
      userId: ALICE_UID,
      contentId: "mb_103",
      title: "Future TS",
      posterUrl: "",
      mediaType: "MOVIE",
      source: "moviebox",
      year: "2026",
      rating: "8.0",
      languageBadge: "EN",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/watchlist/mb_101`).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/watchlist/mb_101`).delete());
});

test("Authenticated owner: watch history create, partial progress update, and bounds enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/history/anime_77`).set({
      userId: ALICE_UID,
      contentId: "anime_77",
      title: "Solo Leveling",
      posterUrl: "https://example.com/solo.jpg",
      mediaType: "ANIME",
      source: "moviebox",
      season: 2,
      episode: 4,
      positionMs: 320000,
      durationMs: 1440000,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/history/anime_77`).update({
      episode: 5,
      positionMs: 480000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/history`).get());

  // Negative bounds check
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/history/anime_77`).update({
      positionMs: -10,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated owner: upcoming reminders create, list, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/reminders/up_99`).set({
      userId: ALICE_UID,
      contentId: "up_99",
      title: "Godzilla Minus Zero",
      posterUrl: "https://example.com/godzilla.jpg",
      releaseDate: "Nov 2026",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/reminders`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/reminders/up_99`).delete());
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/reminders/up_99`).delete());
});
