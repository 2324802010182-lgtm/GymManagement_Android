#!/usr/bin/env node
'use strict';
/**
 * Seed dữ liệu mẫu cho GymManagement_Android bằng Firebase Admin SDK.
 *
 *  - Idempotent: chạy lại nhiều lần không tạo trùng (ID cố định; dữ liệu giao dịch chỉ tạo nếu chưa có).
 *  - Mọi bản ghi mẫu có isSampleData = true (trừ tài khoản Auth/người dùng, nhận biết bằng email seed.*@example.com).
 *  - KHÔNG tạo video/ảnh giả: videoUrl/imageUrl để rỗng trừ khi bạn cung cấp seed/media.json (URL https đã tự kiểm tra).
 *  - Mật khẩu tài khoản mẫu lấy từ biến môi trường SEED_PASSWORD (không có giá trị mặc định, không lưu trong repo).
 *
 * Cách dùng:  node seed.js [--dry-run] [--delete-samples]
 */
const fs = require('fs');
const path = require('path');

const args = new Set(process.argv.slice(2));
const DRY = args.has('--dry-run');
const DELETE = args.has('--delete-samples');

// ---- nạp seed/.env đơn giản (không cần thêm thư viện) ----
const envFile = path.join(__dirname, '.env');
if (fs.existsSync(envFile)) {
  for (const line of fs.readFileSync(envFile, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (m && !line.trim().startsWith('#') && process.env[m[1]] === undefined) process.env[m[1]] = m[2].replace(/^"|"$/g, '');
  }
}

const PROJECT_ID = process.env.GCLOUD_PROJECT || process.env.FIREBASE_PROJECT_ID || 'gymmanagement-tdmu';
const PASSWORD = process.env.SEED_PASSWORD || '';
const ADMIN_EMAIL = (process.env.SEED_ADMIN_EMAIL || '').trim().toLowerCase();

function fail(msg) {
  console.error('\n✖ ' + msg + '\n');
  process.exit(1);
}

const exercises = require('./data/exercises.js');

// Tùy chọn: media.json = { "<exerciseId>": { videoUrl, videoType, videoThumbnailUrl, videoDurationSeconds, imageUrl } }
function loadMedia() {
  const f = path.join(__dirname, 'media.json');
  if (!fs.existsSync(f)) return {};
  const media = JSON.parse(fs.readFileSync(f, 'utf8'));
  const ids = new Set(exercises.map((e) => e.id));
  for (const [id, m] of Object.entries(media)) {
    if (id.startsWith('_')) continue; // khóa chú thích
    if (!ids.has(id)) fail(`media.json: không có bài tập "${id}".`);
    for (const k of ['videoUrl', 'videoThumbnailUrl', 'imageUrl']) {
      if (m[k] && !/^https:\/\/.{4,}/.test(m[k])) fail(`media.json: ${id}.${k} phải là URL https.`);
    }
    if (m.videoUrl && !['mp4', 'hls', 'dash'].includes(m.videoType)) fail(`media.json: ${id}.videoType phải là mp4 | hls | dash.`);
  }
  return media;
}

// ----- tiện ích thời gian (giờ Việt Nam) -----
const DAY = 86400000;
function vnKey(date) {
  const p = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' })
    .formatToParts(date).reduce((a, x) => ((a[x.type] = x.value), a), {});
  return p.year + p.month + p.day;
}
function normalizeName(n) {
  return n.trim().replace(/\s+/g, ' ').toLowerCase();
}

// ======================= DỮ LIỆU MẪU =======================
const NOW = Date.now();
const SAMPLE_NOTE = ' (Dữ liệu mẫu minh họa, không phải cơ sở thật.)';

const OWNERS = [
  { key: 'quocanh', email: 'seed.quocanh.owner@example.com', name: 'Nguyễn Quốc Anh', phone: '0900000001' },
  { key: 'fitlife', email: 'seed.fitlife.owner@example.com', name: 'Trần Minh Khoa', phone: '0900000002' },
  { key: 'tdmu', email: 'seed.tdmu.owner@example.com', name: 'Lê Thu Hà', phone: '0900000003' },
];
const PENDING_OWNER = { key: 'pending', email: 'seed.pending.owner@example.com', name: 'Phạm Gia Bảo', phone: '0900000009', proposedGymName: 'Titan Gym (mẫu)', note: 'Muốn mở phòng tập tại TP. Thủ Dầu Một.' };
const MEMBERS = [
  { key: 'm1', email: 'seed.member1@example.com', name: 'Võ Thanh An', phone: '0911000001' },
  { key: 'm2', email: 'seed.member2@example.com', name: 'Đặng Hoài Nam', phone: '0911000002' },
  { key: 'm3', email: 'seed.member3@example.com', name: 'Bùi Ngọc Mai', phone: '0911000003' },
  { key: 'm4', email: 'seed.member4@example.com', name: 'Hồ Quang Huy', phone: '0911000004' },
];

const GYMS = [
  {
    id: 'gym_quocanh', ownerKey: 'quocanh', name: 'Quốc Anh Gym',
    address: 'Đường mẫu số 1, Phường Phú Hòa, TP. Thủ Dầu Một, Bình Dương', area: 'Thủ Dầu Một, Bình Dương',
    phone: '0274000001', hours: '05:30 – 21:30 (T2–CN)',
    description: 'Phòng tập tổng hợp với khu tạ tự do, máy tập và lớp nhóm.' + SAMPLE_NOTE,
    packages: [
      ['pkg_qa_1m', 'Gói 1 tháng', 300000, 30, 'Tập không giới hạn trong 1 tháng.', ['Khu tạ tự do', 'Tủ đồ', 'Hướng dẫn cơ bản'], true],
      ['pkg_qa_3m', 'Gói 3 tháng', 800000, 90, 'Tiết kiệm hơn cho người tập đều đặn.', ['Khu tạ tự do', 'Tủ đồ', '1 buổi đánh giá thể lực'], true],
      ['pkg_qa_6m', 'Gói 6 tháng', 1400000, 180, 'Cam kết dài hạn, giá ưu đãi.', ['Khu tạ tự do', 'Tủ đồ', '2 buổi đánh giá thể lực'], true],
      ['pkg_qa_old', 'Gói khuyến mãi cũ', 250000, 30, 'Gói đã ngừng bán (dùng để kiểm thử trạng thái tạm ẩn).', [], false],
    ],
  },
  {
    id: 'gym_fitlife', ownerKey: 'fitlife', name: 'FitLife Gym',
    address: 'Đường mẫu số 2, Quận 7, TP. Hồ Chí Minh', area: 'Quận 7, TP. Hồ Chí Minh',
    phone: '0280000002', hours: '06:00 – 22:00 (T2–CN)',
    description: 'Phòng tập hiện đại tập trung vào cardio và sức mạnh.' + SAMPLE_NOTE,
    packages: [
      ['pkg_fl_1m', 'Gói tháng', 450000, 30, 'Tập tự do 1 tháng.', ['Khu cardio', 'Khu tạ', 'Phòng thay đồ'], true],
      ['pkg_fl_3m', 'Gói quý', 1200000, 90, 'Gói 3 tháng phổ biến.', ['Khu cardio', 'Khu tạ', 'Phòng thay đồ', '1 buổi PT thử'], true],
      ['pkg_fl_12m', 'Gói năm', 4000000, 365, 'Dành cho khách hàng gắn bó lâu dài.', ['Toàn bộ khu tập', 'Ưu tiên đặt lịch'], true],
    ],
  },
  {
    id: 'gym_tdmu', ownerKey: 'tdmu', name: 'TDMU Fitness',
    address: 'Gần Đại học Thủ Dầu Một (địa chỉ minh họa), Bình Dương', area: 'Thủ Dầu Một, Bình Dương',
    phone: '0274000003', hours: '06:00 – 20:30 (T2–T7)',
    description: 'Phòng tập dành cho sinh viên và cán bộ với mức giá thân thiện.' + SAMPLE_NOTE,
    packages: [
      ['pkg_td_sv1m', 'Gói sinh viên 1 tháng', 200000, 30, 'Ưu đãi cho sinh viên (cần thẻ sinh viên).', ['Khu tạ', 'Tủ đồ'], true],
      ['pkg_td_sv3m', 'Gói sinh viên 3 tháng', 540000, 90, 'Ưu đãi 3 tháng cho sinh viên.', ['Khu tạ', 'Tủ đồ', 'Cardio'], true],
      ['pkg_td_std', 'Gói thường 1 tháng', 350000, 30, 'Dành cho mọi đối tượng.', ['Khu tạ', 'Tủ đồ', 'Cardio'], true],
    ],
  },
];

// ======================= LỚP THAO TÁC =======================
let admin, db, auth, TS, FV;

function ts(ms) {
  return TS.fromMillis(ms);
}

const plan = []; // { ref, data, mode: 'upsert' | 'ensure', label }
function upsert(ref, data) { plan.push({ ref, data, mode: 'upsert' }); }
function ensure(ref, data) { plan.push({ ref, data, mode: 'ensure' }); }

async function ensureAuthUser(email, displayName) {
  try {
    return (await auth.getUserByEmail(email)).uid;
  } catch (e) {
    if (e.code !== 'auth/user-not-found') throw e;
  }
  if (DRY) return 'DRY_' + email.split('@')[0];
  const u = await auth.createUser({ email, password: PASSWORD, displayName, emailVerified: false });
  return u.uid;
}

function buildPlan(uids, media) {
  // --- người dùng ---
  const allUsers = [
    ...OWNERS.map((o) => ({ ...o, role: 'gym_owner', status: 'active' })),
    { ...PENDING_OWNER, role: 'gym_owner', status: 'pending' },
    ...MEMBERS.map((m) => ({ ...m, role: 'member', status: 'active' })),
  ];
  for (const u of allUsers) {
    const uid = uids[u.key];
    const data = {
      uid, fullName: u.name, email: u.email, phone: u.phone, role: u.role, accountStatus: u.status,
      createdAt: ts(NOW), updatedAt: ts(NOW),
    };
    if (u.proposedGymName) data.proposedGymName = u.proposedGymName;
    if (u.note) data.applicationNote = u.note;
    ensure(db.doc(`users/${uid}`), data);
  }
  // mục tiêu cá nhân cho member1
  upsert(db.doc(`users/${uids.m1}`), { goal: { targetWeightKg: 65, weeklyWorkoutTarget: 4, note: 'Giữ nhịp tập đều đặn.' } });

  if (ADMIN_EMAIL && uids.__admin) {
    ensure(db.doc(`platformAdmins/${uids.__admin}`), { uid: uids.__admin, email: ADMIN_EMAIL, createdAt: ts(NOW) });
  }

  // --- thương hiệu, gói, hội viên ---
  for (const g of GYMS) {
    const ownerUid = uids[g.ownerKey];
    const owner = OWNERS.find((o) => o.key === g.ownerKey);
    const normalized = normalizeName(g.name);
    upsert(db.doc(`gyms/${g.id}`), {
      name: g.name, normalizedName: normalized, ownerUid, ownerName: owner.name,
      address: g.address, area: g.area, phone: g.phone, description: g.description, openingHours: g.hours,
      logoUrl: '', coverUrl: '', galleryUrls: [], imageCredit: '', status: 'active', isSampleData: true,
      createdAt: ts(NOW - 90 * DAY), updatedAt: ts(NOW),
    });
    upsert(db.doc(`gymNameReservations/${normalized}`), { gymId: g.id, ownerUid, name: g.name, createdAt: ts(NOW - 90 * DAY) });
    for (const [pid, name, price, days, desc, benefits, active] of g.packages) {
      upsert(db.doc(`gyms/${g.id}/packages/${pid}`), {
        gymId: g.id, name, price, durationDays: days, description: desc, benefits, isActive: active,
        isSampleData: true, createdAt: ts(NOW - 80 * DAY), updatedAt: ts(NOW),
      });
    }
  }

  const gymById = Object.fromEntries(GYMS.map((g) => [g.id, g]));
  const pkgOf = (gid, pid) => gymById[gid].packages.find((p) => p[0] === pid);
  const mem = Object.fromEntries(MEMBERS.map((m) => [m.key, m]));

  // Tạo request(approved) + membership + member summary
  function approved(reqId, gid, mk, pid, startMs) {
    const g = gymById[gid];
    const p = pkgOf(gid, pid);
    const m = mem[mk];
    const ownerUid = uids[g.ownerKey];
    const endMs = startMs + p[3] * DAY;
    ensure(db.doc(`gyms/${gid}/membershipRequests/${reqId}`), {
      gymId: gid, gymName: g.name, memberUid: uids[mk], memberName: m.name, memberEmail: m.email, memberPhone: m.phone,
      packageId: pid, packageName: p[1], packagePrice: p[2], packageDurationDays: p[3], note: '',
      status: 'approved', reviewedBy: ownerUid, reviewedAt: ts(startMs), membershipId: reqId,
      createdAt: ts(startMs - 3600000), updatedAt: ts(startMs),
    });
    ensure(db.doc(`gyms/${gid}/memberships/${reqId}`), {
      gymId: gid, gymName: g.name, memberUid: uids[mk], memberName: m.name, packageId: pid, packageName: p[1],
      packagePrice: p[2], durationDays: p[3], requestId: reqId, status: 'active',
      startAt: ts(startMs), endAt: ts(endMs), approvedBy: ownerUid, createdAt: ts(startMs), updatedAt: ts(startMs),
    });
    ensure(db.doc(`gyms/${gid}/members/${uids[mk]}`), {
      memberUid: uids[mk], gymId: gid, fullName: m.name, email: m.email, phone: m.phone,
      latestMembershipId: reqId, latestPackageName: p[1], latestEndAt: ts(endMs), joinedAt: ts(startMs),
      updatedAt: ts(startMs), isSampleData: true,
    });
    return { endMs, ownerUid };
  }

  // member1: Quốc Anh (còn hạn) + FitLife (đã hết hạn)
  const a1 = approved('req_m1_quocanh', 'gym_quocanh', 'm1', 'pkg_qa_3m', NOW - 20 * DAY);
  approved('req_m1_fitlife', 'gym_fitlife', 'm1', 'pkg_fl_1m', NOW - 100 * DAY);
  // member4: TDMU (sắp hết hạn trong ~5 ngày)
  approved('req_m4_tdmu', 'gym_tdmu', 'm4', 'pkg_td_sv1m', NOW - 25 * DAY);

  // member2: yêu cầu đang chờ duyệt ở FitLife + marker chống trùng
  ensure(db.doc('gyms/gym_fitlife/membershipRequests/req_m2_fitlife_pending'), {
    gymId: 'gym_fitlife', gymName: 'FitLife Gym', memberUid: uids.m2, memberName: mem.m2.name, memberEmail: mem.m2.email,
    memberPhone: mem.m2.phone, packageId: 'pkg_fl_3m', packageName: 'Gói quý', packagePrice: 1200000, packageDurationDays: 90,
    note: 'Mình muốn bắt đầu từ đầu tuần sau.', status: 'pending', createdAt: ts(NOW - 2 * 3600000), updatedAt: ts(NOW - 2 * 3600000),
  });
  ensure(db.doc(`gyms/gym_fitlife/pendingRequests/${uids.m2}`), { requestId: 'req_m2_fitlife_pending', memberUid: uids.m2, createdAt: ts(NOW - 2 * 3600000) });

  // member3: yêu cầu bị từ chối ở TDMU
  ensure(db.doc('gyms/gym_tdmu/membershipRequests/req_m3_tdmu_rejected'), {
    gymId: 'gym_tdmu', gymName: 'TDMU Fitness', memberUid: uids.m3, memberName: mem.m3.name, memberEmail: mem.m3.email,
    memberPhone: mem.m3.phone, packageId: 'pkg_td_sv1m', packageName: 'Gói sinh viên 1 tháng', packagePrice: 200000,
    packageDurationDays: 30, note: '', status: 'rejected', reviewedBy: uids.tdmu, reviewedAt: ts(NOW - DAY),
    rejectReason: 'Chưa xác minh được thẻ sinh viên (dữ liệu mẫu).', createdAt: ts(NOW - 2 * DAY), updatedAt: ts(NOW - DAY),
  });

  // --- lịch tập (member1 @ Quốc Anh) ---
  const sched = [
    ['sch_m1_a', 'Tập ngực – tay sau', 'Khu tạ tự do', NOW + DAY, 'scheduled'],
    ['sch_m1_b', 'Tập chân – mông', 'Khu tạ tự do', NOW + 3 * DAY, 'scheduled'],
    ['sch_m1_c', 'Cardio nhẹ + core', 'Khu cardio', NOW - 2 * DAY, 'completed'],
  ];
  for (const [sid, title, loc, startMs, status] of sched) {
    ensure(db.doc(`gyms/gym_quocanh/schedules/${sid}`), {
      gymId: 'gym_quocanh', gymName: 'Quốc Anh Gym', memberUid: uids.m1, memberName: mem.m1.name, title,
      description: 'Buổi tập mẫu.', location: loc, startAt: ts(startMs), endAt: ts(startMs + 3600000), status,
      createdBy: a1.ownerUid, isSampleData: true, createdAt: ts(NOW - 5 * DAY), updatedAt: ts(NOW - 5 * DAY),
    });
  }

  // --- điểm danh member1 ---
  for (const d of [0, 2, 3, 5, 8]) {
    const when = NOW - d * DAY;
    const key = vnKey(new Date(when));
    ensure(db.doc(`gyms/gym_quocanh/attendance/${uids.m1}_${key}`), {
      gymId: 'gym_quocanh', gymName: 'Quốc Anh Gym', memberUid: uids.m1, memberName: mem.m1.name, membershipId: 'req_m1_quocanh',
      packageName: 'Gói 3 tháng', dateKey: key, checkInAt: ts(when), recordedBy: a1.ownerUid, recordedByName: OWNERS[0].name,
    });
  }

  // --- chỉ số cơ thể & lịch sử tập (member1) ---
  const weights = [[60, 72.0, 21.0], [45, 71.2, 20.5], [30, 70.5, 20.1], [14, 69.4, 19.6], [3, 68.8, 19.2]];
  for (const [d, w, fat] of weights) {
    const bmi = Math.round((w / Math.pow(1.72, 2)) * 100) / 100;
    ensure(db.doc(`users/${uids.m1}/measurements/ms_${d}`), {
      weightKg: w, heightCm: 172, bodyFatPercent: fat, bmi, measuredAt: ts(NOW - d * DAY), note: '',
      isSampleData: true, createdAt: ts(NOW - d * DAY), updatedAt: ts(NOW - d * DAY),
    });
  }
  const logs = [
    [1, 'bench-press-barbell', 'Đẩy ngực nằm ngang với tạ đòn', 'Ngực', 4, 8, 45],
    [3, 'barbell-squat', 'Squat với tạ đòn', 'Chân', 4, 8, 50],
    [4, 'seated-cable-row', 'Kéo cáp ngồi', 'Lưng', 3, 12, 35],
    [6, 'plank', 'Plank', 'Bụng', 3, 1, 15],
    [9, 'treadmill-run', 'Chạy bộ trên máy', 'Cardio', 1, 1, 25],
    [12, 'dumbbell-shoulder-press', 'Đẩy vai với tạ đơn', 'Vai', 3, 10, 30],
  ];
  for (const [d, exId, exName, cat, sets, reps, min] of logs) {
    ensure(db.doc(`users/${uids.m1}/workoutLogs/wl_${d}_${exId}`), {
      exerciseId: exId, exerciseName: exName, category: cat, sets, reps, durationMinutes: min, note: '',
      completedAt: ts(NOW - d * DAY), source: 'exercise_library', isSampleData: true, createdAt: ts(NOW - d * DAY),
    });
  }

  // --- thông báo ---
  const note = (rec, gid, gname, title, body, type, rel, sender, daysAgo, read) => ({
    recipientUid: rec, gymId: gid, gymName: gname, title, body, type, relatedId: rel, isRead: read, senderUid: sender,
    createdAt: ts(NOW - daysAgo * DAY),
  });
  ensure(db.doc(`users/${uids.m1}/notifications/seed_n1`),
    note(uids.m1, 'gym_quocanh', 'Quốc Anh Gym', 'Yêu cầu đăng ký đã được duyệt', 'Gói 3 tháng của bạn đã có hiệu lực.', 'request_approved', 'req_m1_quocanh', uids.quocanh, 20, true));
  ensure(db.doc(`users/${uids.m1}/notifications/seed_n2`),
    note(uids.m1, 'gym_quocanh', 'Quốc Anh Gym', 'Lịch tập mới', 'Buổi "Tập ngực – tay sau" đã được lên lịch.', 'schedule_created', 'sch_m1_a', uids.quocanh, 1, false));
  ensure(db.doc(`users/${uids.m3}/notifications/seed_n3`),
    note(uids.m3, 'gym_tdmu', 'TDMU Fitness', 'Yêu cầu đăng ký bị từ chối', 'Chưa xác minh được thẻ sinh viên (dữ liệu mẫu).', 'request_rejected', 'req_m3_tdmu_rejected', uids.tdmu, 1, false));
  ensure(db.doc(`users/${uids.fitlife}/notifications/seed_n4`),
    note(uids.fitlife, 'gym_fitlife', 'FitLife Gym', 'Yêu cầu đăng ký gói mới', `${mem.m2.name} vừa gửi yêu cầu đăng ký Gói quý.`, 'membership_request', 'req_m2_fitlife_pending', uids.m2, 0, false));
  ensure(db.doc('gyms/gym_quocanh/notifications/seed_announce1'), {
    gymId: 'gym_quocanh', gymName: 'Quốc Anh Gym', title: 'Lịch nghỉ lễ', body: 'Phòng tập mở cửa bình thường trong dịp lễ sắp tới (thông báo mẫu).',
    audience: 'all_members', recipientCount: 1, createdBy: uids.quocanh, createdAt: ts(NOW - 6 * DAY),
  });

  // --- bài tập ---
  for (const ex of exercises) {
    const data = { ...ex.data, createdAt: ts(NOW), updatedAt: ts(NOW) };
    const m = media[ex.id];
    if (m) {
      for (const k of ['videoUrl', 'videoType', 'videoThumbnailUrl', 'imageUrl']) if (m[k]) data[k] = m[k];
      if (m.videoDurationSeconds) data.videoDurationSeconds = Number(m.videoDurationSeconds);
    }
    upsert(db.doc(`exercises/${ex.id}`), data);
  }
}

async function applyPlan() {
  let created = 0, updated = 0, skipped = 0;
  let batch = db.batch();
  let n = 0;
  const flush = async () => { if (n > 0 && !DRY) await batch.commit(); batch = db.batch(); n = 0; };
  for (const p of plan) {
    const snap = await p.ref.get();
    if (p.mode === 'ensure' && snap.exists) { skipped++; continue; }
    if (snap.exists) updated++; else created++;
    if (!DRY) {
      if (p.mode === 'upsert') batch.set(p.ref, p.data, { merge: true });
      else batch.set(p.ref, p.data);
      if (++n >= 400) await flush();
    }
  }
  await flush();
  return { created, updated, skipped };
}

async function deleteSamples(uids) {
  buildPlan(uids, {});
  let del = 0;
  let batch = db.batch(); let n = 0;
  for (const p of plan) {
    // Chỉ xóa tài liệu có cờ isSampleData = true hoặc do seed tạo với ID cố định thuộc nhóm hội viên/giao dịch mẫu.
    const snap = await p.ref.get();
    if (!snap.exists) continue;
    const d = snap.data();
    const seedOwned = d.isSampleData === true || /^(req_|sch_|ms_|wl_|seed_)/.test(p.ref.id) || p.ref.path.startsWith('users/')
      || p.ref.path.startsWith('gymNameReservations/') || p.ref.path.includes('/pendingRequests/')
      || p.ref.path.includes('/members/') || p.ref.path.includes('/attendance/') || p.ref.path.includes('/memberships/')
      || p.ref.path.includes('/membershipRequests/');
    if (!seedOwned) continue;
    if (DRY) { del++; continue; }
    batch.delete(p.ref); del++;
    if (++n >= 400) { await batch.commit(); batch = db.batch(); n = 0; }
  }
  if (n > 0 && !DRY) await batch.commit();
  console.log(`  Đã xóa ${del} tài liệu mẫu trong Firestore.`);
  // xóa subcollection còn sót của người dùng mẫu không có trong plan là không cần thiết (plan đã liệt kê hết ID cố định).
  for (const u of [...OWNERS, PENDING_OWNER, ...MEMBERS]) {
    try {
      const rec = await auth.getUserByEmail(u.email);
      if (!DRY) await auth.deleteUser(rec.uid);
      console.log(`  Đã xóa tài khoản Auth mẫu ${u.email}`);
    } catch (e) {
      if (e.code !== 'auth/user-not-found') throw e;
    }
  }
}

async function main() {
  if (!DELETE && !DRY && PASSWORD.length < 8) {
    fail('Thiếu SEED_PASSWORD (tối thiểu 8 ký tự). Hãy đặt biến môi trường hoặc seed/.env – xem SEED_DATA_GUIDE_VI.md.');
  }
  if (!process.env.FIRESTORE_EMULATOR_HOST && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    fail('Chưa đặt GOOGLE_APPLICATION_CREDENTIALS (đường dẫn khóa service account, để NGOÀI repo). Xem FIREBASE_SETUP_VI.md.');
  }
  admin = require('firebase-admin');
  admin.initializeApp({ projectId: PROJECT_ID });
  db = admin.firestore();
  auth = admin.auth();
  TS = admin.firestore.Timestamp;
  FV = admin.firestore.FieldValue; // eslint-disable-line no-unused-vars

  console.log(`Dự án: ${PROJECT_ID}${DRY ? '  [DRY-RUN – không ghi gì]' : ''}${process.env.FIRESTORE_EMULATOR_HOST ? '  [EMULATOR]' : ''}`);
  const media = DELETE ? {} : loadMedia();

  // 1) Tài khoản Auth (tạo nếu chưa có)
  const uids = {};
  const all = [...OWNERS, PENDING_OWNER, ...MEMBERS];
  for (const u of all) {
    if (DELETE) {
      try { uids[u.key] = (await auth.getUserByEmail(u.email)).uid; } catch (e) { if (e.code !== 'auth/user-not-found') throw e; uids[u.key] = 'missing_' + u.key; }
    } else {
      uids[u.key] = await ensureAuthUser(u.email, u.name);
    }
  }
  if (ADMIN_EMAIL) {
    try { uids.__admin = (await auth.getUserByEmail(ADMIN_EMAIL)).uid; }
    catch (e) { console.warn(`  ! SEED_ADMIN_EMAIL=${ADMIN_EMAIL} chưa có trong Authentication → bỏ qua cấp quyền admin.`); }
  }

  if (DELETE) {
    console.log('Xóa dữ liệu mẫu…');
    await deleteSamples(uids);
    console.log('Hoàn tất.');
    return;
  }

  buildPlan(uids, media);
  const r = await applyPlan();
  console.log(`Hoàn tất: tạo mới ${r.created}, cập nhật ${r.updated}, giữ nguyên ${r.skipped} tài liệu.`);
  console.log('Tài khoản mẫu (mật khẩu = SEED_PASSWORD của bạn):');
  for (const u of all) console.log(`  - ${u.email}  (${u.key})`);
  if (ADMIN_EMAIL && uids.__admin) console.log(`  Đã cấp quyền quản trị nền tảng cho ${ADMIN_EMAIL}.`);
  const withVideo = Object.keys(media).filter((k) => !k.startsWith('_') && media[k].videoUrl).length;
  console.log(`Bài tập: ${exercises.length} (có video: ${withVideo}). Video/ảnh chỉ có khi bạn cung cấp seed/media.json với URL đã kiểm tra.`);
}

main().catch((e) => {
  console.error('\n✖ Seed thất bại:', e && e.message ? e.message : e);
  process.exit(1);
});
