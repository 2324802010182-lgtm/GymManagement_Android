package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.DateKeys;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.example.gymmanagement.util.Fmt;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Yêu cầu đăng ký gói → (Chủ Gym duyệt) → membership. Mọi thao tác nhiều bản ghi đều dùng WriteBatch
 * nên nguyên tử; Security Rules kiểm tra chéo bằng getAfter()/existsAfter().
 */
public class MembershipRepository {

    private DocumentReference gymRef(String gymId) {
        return Fb.db().collection("gyms").document(gymId);
    }

    // ------------------------------------------------------------------ Hội viên

    /** Hội viên gửi yêu cầu: tạo request(pending) + pending marker + thông báo cho Chủ Gym trong một batch. */
    public void submitRequest(Gym gym, GymPackage pkg, User member, String note, Callback<MembershipRequest> cb) {
        if (!Repo.guard(cb)) return;
        if (!pkg.isActive) {
            cb.onError(AppError.invalid("Gói này hiện không còn mở đăng ký."));
            return;
        }
        if (!Repo.limit(cb, note, 300, "Lời nhắn")) return;
        FirebaseFirestore db = Fb.db();
        DocumentReference gref = gymRef(gym.id);
        gref.collection("memberships").whereEqualTo("memberUid", member.uid)
                .whereEqualTo("packageId", pkg.id)
                .get()
                .addOnSuccessListener(msQs -> {
                    boolean hasActive = false;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : msQs.getDocuments()) {
                        Membership m = Membership.from(doc);
                        if (m.isValidNow()) {
                            hasActive = true;
                            break;
                        }
                    }
                    if (hasActive) {
                        cb.onError(new AppError(AppError.Kind.CONFLICT, "Bạn đang sử dụng gói này trong thời hạn hiệu lực."));
                        return;
                    }
                    DocumentReference marker = gref.collection("pendingRequests").document(member.uid);
                    marker.get().addOnSuccessListener(md -> {
                        if (md.exists()) {
                            cb.onError(new AppError(AppError.Kind.CONFLICT,
                                    "Bạn đã có một yêu cầu đang chờ duyệt tại Gym này. Hãy chờ Chủ Gym xử lý hoặc hủy yêu cầu cũ."));
                            return;
                        }
                        DocumentReference rref = gref.collection("membershipRequests").document();
                        Map<String, Object> r = new HashMap<>();
                        r.put("gymId", gym.id);
                        r.put("gymName", gym.name);
                        r.put("memberUid", member.uid);
                        r.put("memberName", member.fullName);
                        r.put("memberEmail", member.email);
                        r.put("memberPhone", member.phone);
                        r.put("packageId", pkg.id);
                        r.put("packageName", pkg.name);
                        r.put("packagePrice", pkg.price);
                        r.put("packageDurationDays", (long) pkg.durationDays);
                        r.put("note", note == null ? "" : note.trim());
                        r.put("status", MembershipRequest.PENDING);
                        r.put("createdAt", FieldValue.serverTimestamp());
                        r.put("updatedAt", FieldValue.serverTimestamp());

                        Map<String, Object> mk = new HashMap<>();
                        mk.put("requestId", rref.getId());
                        mk.put("memberUid", member.uid);
                        mk.put("createdAt", FieldValue.serverTimestamp());

                        DocumentReference nref = db.collection("users").document(gym.ownerUid)
                                .collection("notifications").document();
                        DocumentReference memberRef = gref.collection("members").document(member.uid);
                        Map<String, Object> mc = new HashMap<>();
                        mc.put("memberUid", member.uid);
                        mc.put("gymId", gym.id);
                        mc.put("fullName", member.fullName);
                        mc.put("email", member.email);
                        mc.put("phone", member.phone);
                        mc.put("latestPackageName", pkg.name);
                        mc.put("joinedAt", FieldValue.serverTimestamp());
                        mc.put("updatedAt", FieldValue.serverTimestamp());

                        WriteBatch b = db.batch();
                        b.set(rref, r);
                        b.set(marker, mk);
                        b.set(memberRef, mc, com.google.firebase.firestore.SetOptions.merge());
                        b.set(nref, Repo.notification(gym.ownerUid, gym.id, gym.name, "Yêu cầu đăng ký gói mới",
                                member.fullName + " muốn đăng ký gói \"" + pkg.name + "\" (" + Fmt.money(pkg.price) + ").",
                                "membership_request", rref.getId(), member.uid));
                        b.commit()
                                .addOnSuccessListener(v -> rref.get()
                                        .addOnSuccessListener(d -> cb.onSuccess(MembershipRequest.from(d)))
                                        .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e))))
                                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
                    }).addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
                }).addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void listMyRequests(Callback<List<MembershipRequest>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collectionGroup("membershipRequests").whereEqualTo("memberUid", Fb.uid()).get()
                .addOnSuccessListener(qs -> {
                    List<MembershipRequest> l = Repo.map(qs, MembershipRequest::from);
                    Collections.sort(l, (a, b) -> Repo.cmpDesc(a.createdAt, b.createdAt));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void listMyMemberships(Callback<List<Membership>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collectionGroup("memberships").whereEqualTo("memberUid", Fb.uid()).get()
                .addOnSuccessListener(qs -> {
                    List<Membership> l = Repo.map(qs, Membership::from);
                    Collections.sort(l, (a, b) -> Repo.cmpDesc(a.endAt, b.endAt));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Hội viên tự hủy yêu cầu đang chờ (gỡ marker trong cùng batch). */
    public void cancelRequest(MembershipRequest r, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        FirebaseFirestore db = Fb.db();
        DocumentReference rref = gymRef(r.gymId).collection("membershipRequests").document(r.id);
        Map<String, Object> m = new HashMap<>();
        m.put("status", MembershipRequest.CANCELLED);
        m.put("updatedAt", FieldValue.serverTimestamp());
        WriteBatch b = db.batch();
        b.update(rref, m);
        b.delete(gymRef(r.gymId).collection("pendingRequests").document(r.memberUid));
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    // ------------------------------------------------------------------ Chủ Gym

    public void listGymRequests(String gymId, Callback<List<MembershipRequest>> cb) {
        if (!Repo.guard(cb)) return;
        gymRef(gymId).collection("membershipRequests").orderBy("createdAt", Query.Direction.DESCENDING).limit(200).get()
                .addOnSuccessListener(qs -> cb.onSuccess(Repo.map(qs, MembershipRequest::from)))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /**
     * Duyệt yêu cầu: (1) request pending→approved, (2) tạo membership cùng ID, (3) tạo/cập nhật hồ sơ hội viên trong Gym,
     * (4) gỡ marker, (5) thông báo cho hội viên – tất cả trong MỘT batch. ID membership = ID yêu cầu nên
     * không thể tạo hai membership từ một yêu cầu, và duyệt lặp sẽ bị Rules từ chối.
     */
    public void approve(MembershipRequest r, User owner, Callback<Membership> cb) {
        if (!Repo.guard(cb)) return;
        if (!r.isPending()) {
            cb.onError(AppError.invalid("Yêu cầu này đã được xử lý."));
            return;
        }
        FirebaseFirestore db = Fb.db();
        DocumentReference gref = gymRef(r.gymId);
        DocumentReference memberRef = gref.collection("members").document(r.memberUid);
        memberRef.get().addOnSuccessListener(md -> {
            Date start = DateKeys.startOfDay(new Date());
            Timestamp startTs = new Timestamp(start);
            Timestamp endTs = new Timestamp(startTs.getSeconds() + r.packageDurationDays * 86_400L, startTs.getNanoseconds());

            DocumentReference rref = gref.collection("membershipRequests").document(r.id);
            DocumentReference mref = gref.collection("memberships").document(r.id);

            Map<String, Object> req = new HashMap<>();
            req.put("status", MembershipRequest.APPROVED);
            req.put("reviewedBy", owner.uid);
            req.put("reviewedAt", FieldValue.serverTimestamp());
            req.put("membershipId", r.id);
            req.put("updatedAt", FieldValue.serverTimestamp());

            Map<String, Object> ms = new HashMap<>();
            ms.put("gymId", r.gymId);
            ms.put("gymName", r.gymName);
            ms.put("memberUid", r.memberUid);
            ms.put("memberName", r.memberName);
            ms.put("packageId", r.packageId);
            ms.put("packageName", r.packageName);
            ms.put("packagePrice", r.packagePrice);
            ms.put("durationDays", (long) r.packageDurationDays);
            ms.put("requestId", r.id);
            ms.put("status", Membership.ACTIVE);
            ms.put("startAt", startTs);
            ms.put("endAt", endTs);
            ms.put("approvedBy", owner.uid);
            ms.put("createdAt", FieldValue.serverTimestamp());
            ms.put("updatedAt", FieldValue.serverTimestamp());

            WriteBatch b = db.batch();
            b.update(rref, req);
            b.set(mref, ms);
            if (md.exists()) {
                Map<String, Object> mu = new HashMap<>();
                mu.put("fullName", r.memberName);
                mu.put("email", r.memberEmail);
                mu.put("phone", r.memberPhone);
                mu.put("latestMembershipId", r.id);
                mu.put("latestPackageName", r.packageName);
                mu.put("latestEndAt", endTs);
                mu.put("updatedAt", FieldValue.serverTimestamp());
                b.update(memberRef, mu);
            } else {
                Map<String, Object> mc = new HashMap<>();
                mc.put("memberUid", r.memberUid);
                mc.put("gymId", r.gymId);
                mc.put("fullName", r.memberName);
                mc.put("email", r.memberEmail);
                mc.put("phone", r.memberPhone);
                mc.put("latestMembershipId", r.id);
                mc.put("latestPackageName", r.packageName);
                mc.put("latestEndAt", endTs);
                mc.put("joinedAt", FieldValue.serverTimestamp());
                mc.put("updatedAt", FieldValue.serverTimestamp());
                b.set(memberRef, mc);
            }
            b.delete(gref.collection("pendingRequests").document(r.memberUid));
            b.set(db.collection("users").document(r.memberUid).collection("notifications").document(),
                    Repo.notification(r.memberUid, r.gymId, r.gymName, "Yêu cầu đăng ký đã được duyệt",
                            "Gói \"" + r.packageName + "\" tại " + r.gymName + " có hiệu lực đến "
                                    + Fmt.date(endTs.toDate()) + ".",
                            "request_approved", r.id, owner.uid));
            b.commit()
                    .addOnSuccessListener(v -> mref.get()
                            .addOnSuccessListener(d -> cb.onSuccess(Membership.from(d)))
                            .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e))))
                    .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
        }).addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void reject(MembershipRequest r, User owner, String reason, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        if (!Repo.limit(cb, reason, 300, "Lý do")) return;
        if (!r.isPending()) {
            cb.onError(AppError.invalid("Yêu cầu này đã được xử lý."));
            return;
        }
        FirebaseFirestore db = Fb.db();
        DocumentReference gref = gymRef(r.gymId);
        Map<String, Object> req = new HashMap<>();
        req.put("status", MembershipRequest.REJECTED);
        req.put("reviewedBy", owner.uid);
        req.put("reviewedAt", FieldValue.serverTimestamp());
        req.put("rejectReason", reason == null ? "" : reason.trim());
        req.put("updatedAt", FieldValue.serverTimestamp());
        WriteBatch b = db.batch();
        b.update(gref.collection("membershipRequests").document(r.id), req);
        b.delete(gref.collection("pendingRequests").document(r.memberUid));
        String why = reason == null || reason.trim().isEmpty() ? "" : " Lý do: " + reason.trim();
        b.set(db.collection("users").document(r.memberUid).collection("notifications").document(),
                Repo.notification(r.memberUid, r.gymId, r.gymName, "Yêu cầu đăng ký bị từ chối",
                        "Yêu cầu gói \"" + r.packageName + "\" tại " + r.gymName + " chưa được chấp nhận." + why,
                        "request_rejected", r.id, owner.uid));
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void listMembers(String gymId, Callback<List<MemberSummary>> cb) {
        if (!Repo.guard(cb)) return;
        gymRef(gymId).collection("members").limit(500).get()
                .addOnSuccessListener(qs -> {
                    List<MemberSummary> l = Repo.map(qs, MemberSummary::from);
                    Collections.sort(l, (a, b) -> a.fullName.compareToIgnoreCase(b.fullName));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Memberships của một hội viên trong một Gym (Chủ Gym). */
    public void listMemberMemberships(String gymId, String memberUid, Callback<List<Membership>> cb) {
        if (!Repo.guard(cb)) return;
        gymRef(gymId).collection("memberships").whereEqualTo("memberUid", memberUid).get()
                .addOnSuccessListener(qs -> {
                    List<Membership> l = Repo.map(qs, Membership::from);
                    Collections.sort(l, (a, b) -> Repo.cmpDesc(a.endAt, b.endAt));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Hủy membership và báo cho hội viên (một batch). */
    public void cancelMembership(Membership m, User owner, String reason, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        if (!Repo.limit(cb, reason, 300, "Lý do")) return;
        FirebaseFirestore db = Fb.db();
        Map<String, Object> mm = new HashMap<>();
        mm.put("status", Membership.CANCELLED);
        mm.put("cancelReason", reason == null ? "" : reason.trim());
        mm.put("updatedAt", FieldValue.serverTimestamp());
        WriteBatch b = db.batch();
        b.update(gymRef(m.gymId).collection("memberships").document(m.id), mm);
        b.set(db.collection("users").document(m.memberUid).collection("notifications").document(),
                Repo.notification(m.memberUid, m.gymId, m.gymName, "Gói tập đã bị hủy",
                        "Gói \"" + m.packageName + "\" tại " + m.gymName + " đã bị Chủ Gym hủy."
                                + (reason == null || reason.trim().isEmpty() ? "" : " Lý do: " + reason.trim()),
                        "membership_cancelled", m.id, owner.uid));
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
