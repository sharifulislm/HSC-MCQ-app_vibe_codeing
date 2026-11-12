const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";
const ADMIN_UID = "admin_uid_1";
const ADMIN_EMAIL = "shariful.islam.coder@gmail.com";

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

const serverTime = () => new Date();

test("Unauthenticated caller cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("subjects").get());
  await assertFails(unauthDb.collection("chapters").get());
  await assertFails(unauthDb.collection("mcqs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("examResults").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("bookmarks").where("userId", "==", ALICE_UID).get());
});

test("Only single ADMIN_EMAIL can write subjects, chapters, and mcqs; students can read only", async () => {
  const now = serverTime();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@student.bd",
      email_verified: true,
    })
    .firestore();

  // Admin can create subject, chapter, and mcq
  await assertSucceeds(
    adminDb.collection("subjects").doc("ict").set({
      authorId: ADMIN_UID,
      name: "ICT",
      code: "275",
      stream: "Compulsory",
      description: "Information and Communication Technology",
      accentKey: "indigo",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("chapters").doc("ict_ch1").set({
      authorId: ADMIN_UID,
      subjectId: "ict",
      title: "Ch 1: Global Village",
      paperLabel: "Ch-1",
      description: "Global Village, VR, AI & Biometrics",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("mcqs").doc("mcq_ict_1").set({
      authorId: ADMIN_UID,
      subjectId: "ict",
      chapterId: "ict_ch1",
      question: "Who introduced Global Village?",
      options: ["Tim Berners-Lee", "Marshall McLuhan", "Charles Babbage", "Alan Turing"],
      correctIndex: 1,
      explanation: "Marshall McLuhan coined the term.",
      difficulty: "Easy",
      boardTag: "Dhaka Board",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student CAN read subjects, chapters, and mcqs
  await assertSucceeds(aliceDb.collection("subjects").get());
  await assertSucceeds(aliceDb.collection("chapters").get());
  await assertSucceeds(aliceDb.collection("mcqs").get());

  // Student CANNOT write, edit, or delete subjects, chapters, or mcqs
  await assertFails(
    aliceDb.collection("subjects").doc("sub_hack").set({
      authorId: ALICE_UID,
      name: "Fake Subject",
      code: "000",
      stream: "Science",
      accentKey: "emerald",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("chapters").doc("ict_ch1").update({ title: "Hacked", updatedAt: now })
  );
  await assertFails(aliceDb.collection("mcqs").doc("mcq_ict_1").delete());

  // Admin CAN read user list for dashboard stats, student CANNOT
  await assertSucceeds(adminDb.collection("users").get());
  await assertFails(aliceDb.collection("users").get());
});

test("Students can read and write only their own users, settings, examResults, and bookmarks", async () => {
  const now = serverTime();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@student.bd",
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "bob@student.bd",
      email_verified: true,
    })
    .firestore();

  // Alice creates her profile, settings, examResult, bookmark
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ayesha",
      email: "alice@student.bd",
      board: "Dhaka",
      college: "NDC",
      preferredStream: "Science",
      role: "student",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student cannot self-assign role = "admin"
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@student.bd",
      preferredStream: "All",
      role: "admin",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").set({
      userId: ALICE_UID,
      themeMode: "dark",
      ttsVoice: "Kore",
      autoReadTts: false,
      compactCards: false,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("examResults").doc("res_1").set({
      userId: ALICE_UID,
      subjectId: "ict",
      subjectName: "ICT",
      chapterTitle: "Quick Exam (10 Q)",
      score: 9,
      totalQuestions: 10,
      wrongCount: 1,
      durationSeconds: 320,
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("bookmarks").doc(`${ALICE_UID}_mcq_ict_1`).set({
      userId: ALICE_UID,
      mcqId: "mcq_ict_1",
      subjectId: "ict",
      chapterId: "ict_ch1",
      question: "Who introduced Global Village?",
      createdAt: now,
    })
  );

  // Alice can list her own examResults and bookmarks
  await assertSucceeds(aliceDb.collection("examResults").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("bookmarks").where("userId", "==", ALICE_UID).get());

  // Bob CANNOT read or write Alice's examResults, bookmarks, or profile
  await assertFails(bobDb.collection("examResults").doc("res_1").get());
  await assertFails(bobDb.collection("examResults").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("bookmarks").doc(`${ALICE_UID}_mcq_ict_1`).get());
  await assertFails(bobDb.collection("bookmarks").doc(`${ALICE_UID}_mcq_ict_1`).delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow write with ghost field fails", async () => {
  const now = serverTime();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@student.bd",
      email_verified: true,
    })
    .firestore();

  await assertFails(
    aliceDb.collection("bookmarks").doc(`${ALICE_UID}_ghost`).set({
      userId: ALICE_UID,
      mcqId: "mcq_1",
      subjectId: "ict",
      chapterId: "ict_ch1",
      question: "Test?",
      isAdmin: true,
      createdAt: now,
    })
  );
});
