package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

/** Đăng nhập, đăng ký, hồ sơ cá nhân (F01). */
public class AuthRepository {

    public boolean isSignedIn() {
        return Fb.auth().getCurrentUser() != null;
    }

    /** Đăng nhập Email/Password rồi nạp hồ sơ Firestore. */
    public void login(String email, String password, Callback<User> cb) {
        if (!Repo.guard(cb)) return;
        Fb.auth().signInWithEmailAndPassword(email.trim().toLowerCase(java.util.Locale.ROOT), password)
                .addOnSuccessListener(r -> loadSession(cb))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Nạp hồ sơ + vai trò thực tế của người đang đăng nhập. */
    public void loadSession(Callback<User> cb) {
        if (!Repo.guard(cb)) return;
        String uid = Fb.uid();
        if (uid == null) {
            cb.onError(new AppError(AppError.Kind.AUTH, "Phiên đăng nhập đã hết hạn."));
            return;
        }
        Fb.db().collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        Fb.auth().signOut();
                        Session.clear();
                        cb.onError(new AppError(AppError.Kind.NOT_FOUND,
                                "Không tìm thấy hồ sơ tài khoản trên hệ thống. Vui lòng liên hệ quản trị viên."));
                        return;
                    }
                    User u = User.from(snap);
                    Session.user = u;
                    Session.platformAdmin = false;
                    Session.ownerGym = null;
                    checkAdminThenOwner(u, cb);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    private void checkAdminThenOwner(User u, Callback<User> cb) {
        Fb.db().collection("platformAdmins").document(u.uid).get()
                .addOnSuccessListener(a -> {
                    Session.platformAdmin = a.exists();
                    loadOwnerGym(u, cb);
                })
                .addOnFailureListener(e -> {
                    Session.platformAdmin = false;
                    loadOwnerGym(u, cb);
                });
    }

    private void loadOwnerGym(User u, Callback<User> cb) {
        if (!u.isOwner() || !u.isActive()) {
            cb.onSuccess(u);
            return;
        }
        Fb.db().collection("gyms").whereEqualTo("ownerUid", u.uid).limit(1).get()
                .addOnSuccessListener(qs -> {
                    Session.ownerGym = qs.isEmpty() ? null : com.example.gymmanagement.model.Gym.from(qs.getDocuments().get(0));
                    cb.onSuccess(u);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void registerMember(String fullName, String email, String phone, String password, Callback<User> cb) {
        register(fullName, email, phone, password, User.ROLE_MEMBER, User.STATUS_ACTIVE, null, null, cb);
    }

    public void registerOwner(String fullName, String email, String phone, String password,
                              String note, String proposedGymName, Callback<User> cb) {
        register(fullName, email, phone, password, User.ROLE_OWNER, User.STATUS_PENDING, note, proposedGymName, cb);
    }

    private void register(String fullName, String email, String phone, String password, String role,
                          String status, String note, String proposedGym, Callback<User> cb) {
        if (!Repo.guard(cb)) return;
        Fb.auth().createUserWithEmailAndPassword(email.trim().toLowerCase(java.util.Locale.ROOT), password)
                .addOnSuccessListener(r -> {
                    FirebaseUser fu = r.getUser();
                    if (fu == null) {
                        cb.onError(AppError.unknown("Không tạo được tài khoản."));
                        return;
                    }
                    Map<String, Object> m = new HashMap<>();
                    m.put("uid", fu.getUid());
                    m.put("fullName", fullName.trim());
                    m.put("email", fu.getEmail());
                    m.put("phone", phone.trim());
                    m.put("role", role);
                    m.put("accountStatus", status);
                    if (note != null && !note.trim().isEmpty()) m.put("applicationNote", note.trim());
                    if (proposedGym != null && !proposedGym.trim().isEmpty()) m.put("proposedGymName", proposedGym.trim());
                    m.put("createdAt", FieldValue.serverTimestamp());
                    m.put("updatedAt", FieldValue.serverTimestamp());
                    Fb.db().collection("users").document(fu.getUid()).set(m)
                            .addOnSuccessListener(v -> loadSession(cb))
                            .addOnFailureListener(e -> {
                                // Hồ sơ không ghi được: xóa tài khoản Auth vừa tạo để tránh tài khoản "mồ côi".
                                fu.delete().addOnCompleteListener(t -> Fb.auth().signOut());
                                cb.onError(ErrorMapper.from(e));
                            });
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void sendPasswordReset(String email, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        Fb.auth().sendPasswordResetEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Chỉ sửa họ tên, số điện thoại và mục tiêu cá nhân (Rules không cho đổi role/email/trạng thái). */
    public void updateProfile(String fullName, String phone, Callback<User> cb) {
        if (!Repo.guard(cb)) return;
        String uid = Fb.uid();
        Map<String, Object> m = new HashMap<>();
        m.put("fullName", fullName.trim());
        m.put("phone", phone.trim());
        m.put("updatedAt", FieldValue.serverTimestamp());
        Fb.db().collection("users").document(uid).update(m)
                .addOnSuccessListener(v -> loadSession(cb))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void updateGoal(Double targetWeightKg, Integer weeklyWorkouts, String note, Callback<User> cb) {
        if (!Repo.guard(cb)) return;
        if (!Repo.limit(cb, note, 200, "Ghi chú mục tiêu")) return;
        Map<String, Object> goal = new HashMap<>();
        goal.put("targetWeightKg", targetWeightKg);
        goal.put("weeklyWorkoutTarget", weeklyWorkouts == null ? null : Long.valueOf(weeklyWorkouts));
        goal.put("note", note == null ? "" : note.trim());
        Map<String, Object> m = new HashMap<>();
        m.put("goal", goal);
        m.put("updatedAt", FieldValue.serverTimestamp());
        Fb.db().collection("users").document(Fb.uid()).update(m)
                .addOnSuccessListener(v -> loadSession(cb))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void signOut() {
        try {
            Fb.auth().signOut();
        } catch (Exception ignored) {
        }
        Session.clear();
    }
}
