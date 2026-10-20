package com.example.gymmanagement.du_lieu;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kho dữ liệu Firebase của GymManagement.
 * Phiên bản này KHÔNG có chế độ demo/local fallback: mọi dữ liệu ứng dụng
 * đều phải đi qua Firebase Authentication và Cloud Firestore.
 */
public class KhoDuLieu {

    public interface KetQua<T> {
        void thanhCong(T duLieu);
        void thatBai(String thongBao);
    }

    private static KhoDuLieu instance;
    private final Context context;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private boolean firebaseSanSang;

    public static synchronized KhoDuLieu lay(Context context) {
        if (instance == null) {
            instance = new KhoDuLieu(context.getApplicationContext());
        }
        return instance;
    }

    private KhoDuLieu(Context context) {
        this.context = context;
        khoiTaoFirebase();
    }

    private void khoiTaoFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            if (!FirebaseApp.getApps(context).isEmpty()) {
                auth = FirebaseAuth.getInstance();
                firestore = FirebaseFirestore.getInstance();
                firebaseSanSang = true;
            } else {
                firebaseSanSang = false;
                auth = null;
                firestore = null;
            }
        } catch (Exception ignored) {
            firebaseSanSang = false;
            auth = null;
            firestore = null;
        }
    }

    public boolean dangDungFirebase() {
        return firebaseSanSang;
    }

    public boolean daDangNhap() {
        return !uidHienTai().isEmpty();
    }

    public String uidHienTai() {
        return firebaseSanSang && auth != null && auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "";
    }

    public void dangXuat() {
        if (firebaseSanSang && auth != null) auth.signOut();
    }

    public void dangNhap(String email, String matKhau, KetQua<Map<String, Object>> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        auth.signInWithEmailAndPassword(email.trim(), matKhau)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() == null ? "" : authResult.getUser().getUid();
                    if (uid.isEmpty()) {
                        auth.signOut();
                        ketQua.thatBai("Không lấy được UID tài khoản từ Firebase Authentication.");
                        return;
                    }
                    layMot("nguoi_dung", uid, new KetQua<Map<String, Object>>() {
                        @Override
                        public void thanhCong(Map<String, Object> profile) {
                            if (profile == null) {
                                auth.signOut();
                                ketQua.thatBai("Tài khoản chưa có hồ sơ trong Firestore. Hãy kiểm tra document nguoi_dung/" + uid + ".");
                                return;
                            }
                            if (Boolean.FALSE.equals(profile.get("trang_thai"))) {
                                auth.signOut();
                                ketQua.thatBai("Tài khoản đang bị khóa. Vui lòng liên hệ quản trị Gym.");
                                return;
                            }
                            ketQua.thanhCong(profile);
                        }

                        @Override
                        public void thatBai(String thongBao) {
                            auth.signOut();
                            ketQua.thatBai(thongBao);
                        }
                    });
                })
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void dangKy(String email, String matKhau, Map<String, Object> profile,
                       KetQua<Map<String, Object>> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        auth.createUserWithEmailAndPassword(email.trim(), matKhau)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() == null ? "" : authResult.getUser().getUid();
                    if (uid.isEmpty()) {
                        auth.signOut();
                        ketQua.thatBai("Không tạo được UID tài khoản.");
                        return;
                    }
                    Map<String, Object> banSao = new HashMap<>(profile);
                    banSao.put("uid", uid);
                    luu("nguoi_dung", uid, banSao, new KetQua<Boolean>() {
                        @Override
                        public void thanhCong(Boolean ok) {
                            banSao.put("id", uid);
                            ketQua.thanhCong(banSao);
                        }

                        @Override
                        public void thatBai(String thongBao) {
                            // Tránh để lại tài khoản Auth mồ côi nếu không tạo được hồ sơ Firestore.
                            if (auth.getCurrentUser() != null) {
                                auth.getCurrentUser().delete().addOnCompleteListener(task -> {
                                    auth.signOut();
                                    ketQua.thatBai("Không lưu được hồ sơ Firestore; tài khoản vừa tạo đã được dọn dẹp. Chi tiết: " + thongBao);
                                });
                            } else {
                                auth.signOut();
                                ketQua.thatBai("Không lưu được hồ sơ Firestore: " + thongBao);
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void guiEmailDatLaiMatKhau(String email, KetQua<Boolean> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> ketQua.thanhCong(true))
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void layMot(String collection, String id, KetQua<Map<String, Object>> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        firestore.collection(collection).document(id).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        ketQua.thanhCong(null);
                        return;
                    }
                    Map<String, Object> data = snapshot.getData() == null
                            ? new HashMap<>() : new HashMap<>(snapshot.getData());
                    data.put("id", snapshot.getId());
                    ketQua.thanhCong(data);
                })
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void layTatCa(String collection, KetQua<List<Map<String, Object>>> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        firestore.collection(collection).get()
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        Map<String, Object> data = document.getData() == null
                                ? new HashMap<>() : new HashMap<>(document.getData());
                        data.put("id", document.getId());
                        result.add(data);
                    }
                    ketQua.thanhCong(result);
                })
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void layTheoTruong(String collection, String field, Object value,
                              KetQua<List<Map<String, Object>>> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        firestore.collection(collection).whereEqualTo(field, value).get()
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        Map<String, Object> data = document.getData() == null
                                ? new HashMap<>() : new HashMap<>(document.getData());
                        data.put("id", document.getId());
                        result.add(data);
                    }
                    ketQua.thanhCong(result);
                })
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void layThongBaoHienThi(String gymId, String uid, boolean laQuanTri,
                                   KetQua<List<Map<String, Object>>> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        com.google.firebase.firestore.Query query = firestore.collection("thong_bao")
                .whereEqualTo("gym_id", gymId);
        if (!laQuanTri) query = query.whereIn("uid_nhan", Arrays.asList("", uid));
        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshot) {
                        Map<String, Object> data = document.getData() == null
                                ? new HashMap<>() : new HashMap<>(document.getData());
                        data.put("id", document.getId());
                        result.add(data);
                    }
                    ketQua.thanhCong(result);
                })
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void them(String collection, Map<String, Object> data, KetQua<String> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        com.google.firebase.firestore.DocumentReference ref = firestore.collection(collection).document();
        Map<String, Object> banSao = new HashMap<>(data);
        banSao.remove("id");
        ref.set(banSao)
                .addOnSuccessListener(unused -> ketQua.thanhCong(ref.getId()))
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void luu(String collection, String id, Map<String, Object> data, KetQua<Boolean> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        Map<String, Object> banSao = new HashMap<>(data);
        banSao.remove("id");
        firestore.collection(collection).document(id).set(banSao)
                .addOnSuccessListener(unused -> ketQua.thanhCong(true))
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void capNhat(String collection, String id, Map<String, Object> patch, KetQua<Boolean> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        Map<String, Object> banSao = new HashMap<>(patch);
        banSao.remove("id");
        // update() không vô tình tạo document mới khi ID không tồn tại.
        firestore.collection(collection).document(id).update(banSao)
                .addOnSuccessListener(unused -> ketQua.thanhCong(true))
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public void xoa(String collection, String id, KetQua<Boolean> ketQua) {
        if (!kiemTraFirebase(ketQua)) return;
        firestore.collection(collection).document(id).delete()
                .addOnSuccessListener(unused -> ketQua.thanhCong(true))
                .addOnFailureListener(e -> ketQua.thatBai(thongBaoLoi(e)));
    }

    public static String chuoi(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static long soLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(chuoi(value)); } catch (Exception ignored) { return 0L; }
    }

    public static boolean booleanValue(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        return "true".equalsIgnoreCase(chuoi(value)) || "1".equals(chuoi(value));
    }

    public static String chuanHoaTen(String ten) {
        String value = Normalizer.normalize(ten == null ? "" : ten.trim(), Normalizer.Form.NFKC);
        value = value.replaceAll("\\s+", " ").toLowerCase(Locale.forLanguageTag("vi-VN"));
        return value;
    }

    private <T> boolean kiemTraFirebase(KetQua<T> ketQua) {
        if (firebaseSanSang && auth != null && firestore != null) return true;
        ketQua.thatBai("Ứng dụng chưa kết nối Firebase. Hãy thêm app/google-services.json, bật Email/Password Authentication, tạo Firestore rồi Sync Gradle và chạy lại ứng dụng.");
        return false;
    }

    private String thongBaoLoi(Exception e) {
        String msg = e == null ? "" : e.getMessage();
        if (msg == null || msg.trim().isEmpty()) return "Đã xảy ra lỗi không xác định.";
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("permission_denied") || lower.contains("missing or insufficient permissions")) {
            return "Firebase từ chối truy cập (PERMISSION_DENIED). Hãy kiểm tra firestore.rules, vai trò và gym_id của tài khoản.";
        }
        if (lower.contains("network") || lower.contains("unavailable") || lower.contains("unable to resolve host")) {
            return "Không kết nối được Firebase. Hãy kiểm tra Internet và cấu hình Firebase Project.";
        }
        if (lower.contains("email address is already in use")) return "Email này đã được đăng ký.";
        if (lower.contains("password should be at least")) return "Mật khẩu cần ít nhất 6 ký tự.";
        return msg;
    }
}
