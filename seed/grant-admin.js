#!/usr/bin/env node
'use strict';
/**
 * Cấp quyền QUẢN TRỊ NỀN TẢNG cho một tài khoản đã có trong Firebase Authentication.
 * Tài khoản này nên đã đăng ký trong ứng dụng (để có hồ sơ users/{uid}).
 *
 *   node grant-admin.js admin@example.com            # cấp quyền
 *   node grant-admin.js admin@example.com --revoke   # thu hồi
 *
 * Cần GOOGLE_APPLICATION_CREDENTIALS (khóa service account NẰM NGOÀI repo) và GCLOUD_PROJECT.
 */
const email = (process.argv[2] || '').trim().toLowerCase();
const revoke = process.argv.includes('--revoke');
if (!email.includes('@')) {
  console.error('Cách dùng: node grant-admin.js <email> [--revoke]');
  process.exit(1);
}
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS && !process.env.FIRESTORE_EMULATOR_HOST) {
  console.error('Chưa đặt GOOGLE_APPLICATION_CREDENTIALS (đường dẫn khóa service account, để ngoài repo).');
  process.exit(1);
}
const admin = require('firebase-admin');
admin.initializeApp({ projectId: process.env.GCLOUD_PROJECT || 'gymmanagement-tdmu' });
(async () => {
  const user = await admin.auth().getUserByEmail(email);
  const ref = admin.firestore().doc(`platformAdmins/${user.uid}`);
  if (revoke) {
    await ref.delete();
    console.log(`Đã thu hồi quyền quản trị của ${email}.`);
  } else {
    await ref.set({ uid: user.uid, email, createdAt: admin.firestore.Timestamp.now() }, { merge: true });
    console.log(`Đã cấp quyền quản trị nền tảng cho ${email} (uid ${user.uid}). Đăng xuất/đăng nhập lại trong ứng dụng.`);
  }
})().catch((e) => {
  console.error('Thất bại:', e.message);
  process.exit(1);
});
