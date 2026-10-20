package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.Attendance;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.DateKeys;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Điểm danh: chỉ Chủ Gym ghi nhận; mỗi hội viên một lượt/ngày (ID = memberUid_yyyyMMdd). */
public class AttendanceRepository {

    private com.google.firebase.firestore.CollectionReference col(String gymId) {
        return Fb.db().collection("gyms").document(gymId).collection("attendance");
    }

    public void checkIn(Gym gym, User owner, MemberSummary member, Callback<Attendance> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("gyms").document(gym.id).collection("memberships")
                .whereEqualTo("memberUid", member.memberUid).get()
                .addOnSuccessListener(qs -> {
                    Membership best = null;
                    for (Membership m : Repo.map(qs, Membership::from)) {
                        if (m.isValidNow() && (best == null || Repo.cmpDesc(m.endAt, best.endAt) < 0)) best = m;
                    }
                    if (best == null) {
                        cb.onError(AppError.invalid("Hội viên không có membership còn hiệu lực nên không thể điểm danh."));
                        return;
                    }
                    final Membership ms = best;
                    String key = DateKeys.today();
                    DocumentReference ref = col(gym.id).document(member.memberUid + "_" + key);
                    ref.get().addOnSuccessListener(d -> {
                        if (d.exists()) {
                            cb.onError(new AppError(AppError.Kind.CONFLICT, "Hội viên này đã được điểm danh hôm nay."));
                            return;
                        }
                        Map<String, Object> m = new HashMap<>();
                        m.put("gymId", gym.id);
                        m.put("gymName", gym.name);
                        m.put("memberUid", member.memberUid);
                        m.put("memberName", member.fullName);
                        m.put("membershipId", ms.id);
                        m.put("packageName", ms.packageName);
                        m.put("dateKey", key);
                        m.put("checkInAt", FieldValue.serverTimestamp());
                        m.put("recordedBy", owner.uid);
                        m.put("recordedByName", owner.fullName);
                        ref.set(m).addOnSuccessListener(v -> ref.get()
                                        .addOnSuccessListener(x -> cb.onSuccess(Attendance.from(x)))
                                        .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e))))
                                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
                    }).addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void listToday(String gymId, Callback<List<Attendance>> cb) {
        if (!Repo.guard(cb)) return;
        col(gymId).whereEqualTo("dateKey", DateKeys.today()).get()
                .addOnSuccessListener(qs -> {
                    List<Attendance> l = Repo.map(qs, Attendance::from);
                    Collections.sort(l, (a, b) -> Repo.cmpDesc(a.checkInAt, b.checkInAt));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void listRecent(String gymId, Callback<List<Attendance>> cb) {
        if (!Repo.guard(cb)) return;
        col(gymId).orderBy("checkInAt", Query.Direction.DESCENDING).limit(100).get()
                .addOnSuccessListener(qs -> cb.onSuccess(Repo.map(qs, Attendance::from)))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Lịch sử điểm danh của một hội viên trong Gym (Chủ Gym). */
    public void listForMember(String gymId, String memberUid, Callback<List<Attendance>> cb) {
        if (!Repo.guard(cb)) return;
        col(gymId).whereEqualTo("memberUid", memberUid).get()
                .addOnSuccessListener(qs -> {
                    List<Attendance> l = Repo.map(qs, Attendance::from);
                    Collections.sort(l, (a, b) -> Repo.cmpDesc(a.checkInAt, b.checkInAt));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Hội viên: lịch sử điểm danh của chính mình (mọi Gym). */
    public void listMine(Callback<List<Attendance>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collectionGroup("attendance").whereEqualTo("memberUid", Fb.uid()).get()
                .addOnSuccessListener(qs -> {
                    List<Attendance> l = Repo.map(qs, Attendance::from);
                    Collections.sort(l, (a, b) -> Repo.cmpDesc(a.checkInAt, b.checkInAt));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void delete(Attendance a, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        col(a.gymId).document(a.id).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
