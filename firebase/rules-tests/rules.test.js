'use strict';
// Kiểm thử Security Rules bằng Firestore Emulator.
// Chạy:  cd firebase/rules-tests && npm install && npm test   (cần Java 21+ cho emulator)
// LƯU Ý: bộ kiểm thử này CHƯA được chạy trong môi trường tạo dự án (emulator không tải được) – hãy chạy và sửa nếu cần.
const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, writeBatch, serverTimestamp, Timestamp, updateDoc } = require('firebase/firestore');

let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-gymmanagement',
    firestore: { rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8') },
  });
});
after(async () => env && env.cleanup());
beforeEach(async () => env.clearFirestore());

const ctx = (uid, email) => env.authenticatedContext(uid, { email: email || `${uid}@example.com` }).firestore();

async function seedAdmin(fn) {
  await env.withSecurityRulesDisabled(async (c) => fn(c.firestore()));
}

const userDoc = (uid, role, status) => ({
  uid, fullName: 'Nguoi Dung', email: `${uid}@example.com`, phone: '0900000000', role, accountStatus: status,
  createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
});

test('đăng ký hội viên: tạo hồ sơ member/active của chính mình', async () => {
  await assertSucceeds(setDoc(doc(ctx('u1'), 'users/u1'), userDoc('u1', 'member', 'active')));
});

test('không thể tự cấp quyền gym_owner đang active hoặc platform_admin', async () => {
  await assertFails(setDoc(doc(ctx('u1'), 'users/u1'), userDoc('u1', 'gym_owner', 'active')));
  await assertFails(setDoc(doc(ctx('u1'), 'users/u1'), userDoc('u1', 'platform_admin', 'active')));
  await assertSucceeds(setDoc(doc(ctx('u2'), 'users/u2'), userDoc('u2', 'gym_owner', 'pending')));
});

test('không đọc được hồ sơ người khác', async () => {
  await seedAdmin((db) => setDoc(doc(db, 'users/u2'), userDoc('u2', 'member', 'active')));
  await assertFails(getDoc(doc(ctx('u1'), 'users/u2')));
});

test('người dùng không thể tự đổi role / accountStatus', async () => {
  await seedAdmin((db) => setDoc(doc(db, 'users/u1'), { ...userDoc('u1', 'member', 'active'), createdAt: Timestamp.now(), updatedAt: Timestamp.now() }));
  await assertFails(updateDoc(doc(ctx('u1'), 'users/u1'), { role: 'gym_owner', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(ctx('u1'), 'users/u1'), { accountStatus: 'active', role: 'platform_admin', updatedAt: serverTimestamp() }));
});

test('chỉ Chủ Gym đã được duyệt mới tạo được thương hiệu; tên là duy nhất', async () => {
  await seedAdmin(async (db) => {
    await setDoc(doc(db, 'users/o1'), { ...userDoc('o1', 'gym_owner', 'active'), createdAt: Timestamp.now(), updatedAt: Timestamp.now() });
    await setDoc(doc(db, 'users/o2'), { ...userDoc('o2', 'gym_owner', 'pending'), createdAt: Timestamp.now(), updatedAt: Timestamp.now() });
  });
  const gym = (uid, name) => ({
    name, normalizedName: name.toLowerCase(), ownerUid: uid, ownerName: 'Chu Gym', address: 'Dia chi mau so 1', area: '', phone: '',
    description: '', openingHours: '', logoUrl: '', coverUrl: '', galleryUrls: [], imageCredit: '', status: 'active',
    isSampleData: false, createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
  });
  const create = async (uid, gid, name) => {
    const db = ctx(uid);
    const b = writeBatch(db);
    b.set(doc(db, `gyms/${gid}`), gym(uid, name));
    b.set(doc(db, `gymNameReservations/${name.toLowerCase()}`), { gymId: gid, ownerUid: uid, name, createdAt: serverTimestamp() });
    return b.commit();
  };
  await assertFails(create('o2', 'g0', 'Pending Gym'));
  await assertSucceeds(create('o1', 'g1', 'Alpha Gym'));
  // owner thứ hai (đã duyệt) không thể dùng lại tên
  await seedAdmin((db) => setDoc(doc(db, 'users/o3'), { ...userDoc('o3', 'gym_owner', 'active'), createdAt: Timestamp.now(), updatedAt: Timestamp.now() }));
  await assertFails(create('o3', 'g3', 'Alpha Gym'));
});

test('thông báo: hội viên không thể gửi thông báo giả mạo cho người khác', async () => {
  await seedAdmin((db) => setDoc(doc(db, 'users/u1'), { ...userDoc('u1', 'member', 'active'), createdAt: Timestamp.now(), updatedAt: Timestamp.now() }));
  await assertFails(setDoc(doc(ctx('u1'), 'users/u2/notifications/n1'), {
    recipientUid: 'u2', gymId: '', gymName: '', title: 'Hi', body: 'x', type: 'announcement', relatedId: '', isRead: false,
    senderUid: 'u1', createdAt: serverTimestamp(),
  }));
});

test('platformAdmins không ghi được từ client', async () => {
  await assertFails(setDoc(doc(ctx('u1'), 'platformAdmins/u1'), { uid: 'u1' }));
});

test('exercises chỉ đọc', async () => {
  await seedAdmin((db) => setDoc(doc(db, 'exercises/e1'), { name: 'A', isActive: true }));
  await assertSucceeds(getDoc(doc(ctx('u1'), 'exercises/e1')));
  await assertFails(setDoc(doc(ctx('u1'), 'exercises/e2'), { name: 'B', isActive: true }));
});
