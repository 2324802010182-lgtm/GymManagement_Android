package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.model.Schedule;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
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

/** F04 – lịch tập. */
public class ScheduleRepository {

    /** Hội viên: các buổi tập được gán cho mình ở mọi Gym. */
    public void listMine(Callback<List<Schedule>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collectionGroup("schedules").whereEqualTo("memberUid", Fb.uid()).get()
                .addOnSuccessListener(qs -> {
                    List<Schedule> l = Repo.map(qs, Schedule::from);
                    Collections.sort(l, (a, b) -> Repo.cmpAsc(a.startAt, b.startAt));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void listForGym(String gymId, Callback<List<Schedule>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("gyms").document(gymId).collection("schedules")
                .orderBy("startAt", Query.Direction.DESCENDING).limit(300).get()
                .addOnSuccessListener(qs -> cb.onSuccess(Repo.map(qs, Schedule::from)))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void create(Gym gym, User owner, MemberSummary member, String title, String description, String location,
                       Date start, Date end, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        if (!checkFields(cb, title, description, location)) return;
        if (!end.after(start)) {
            cb.onError(AppError.invalid("Thời gian kết thúc phải sau thời gian bắt đầu."));
            return;
        }
        FirebaseFirestore db = Fb.db();
        DocumentReference sref = db.collection("gyms").document(gym.id).collection("schedules").document();
        Map<String, Object> m = new HashMap<>();
        m.put("gymId", gym.id);
        m.put("gymName", gym.name);
        m.put("memberUid", member.memberUid);
        m.put("memberName", member.fullName);
        m.put("title", title.trim());
        m.put("description", description.trim());
        m.put("location", location.trim());
        m.put("startAt", new Timestamp(start));
        m.put("endAt", new Timestamp(end));
        m.put("status", Schedule.SCHEDULED);
        m.put("createdBy", owner.uid);
        m.put("isSampleData", false);
        m.put("createdAt", FieldValue.serverTimestamp());
        m.put("updatedAt", FieldValue.serverTimestamp());
        WriteBatch b = db.batch();
        b.set(sref, m);
        b.set(db.collection("users").document(member.memberUid).collection("notifications").document(),
                Repo.notification(member.memberUid, gym.id, gym.name, "Lịch tập mới",
                        "Buổi \"" + title.trim() + "\" lúc " + Fmt.dateTime(start) + " tại " + gym.name + ".",
                        "schedule_created", sref.getId(), owner.uid));
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    private static boolean checkFields(Callback<?> cb, String title, String description, String location) {
        String t = title == null ? "" : title.trim();
        if (t.length() < 2 || t.length() > 80) {
            cb.onError(AppError.invalid("Nội dung buổi tập phải từ 2 đến 80 ký tự."));
            return false;
        }
        return Repo.limit(cb, description, 500, "Mô tả") && Repo.limit(cb, location, 80, "Địa điểm");
    }

    public void update(Schedule s, User owner, String title, String description, String location,
                       Date start, Date end, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        if (!checkFields(cb, title, description, location)) return;
        if (!end.after(start)) {
            cb.onError(AppError.invalid("Thời gian kết thúc phải sau thời gian bắt đầu."));
            return;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("title", title.trim());
        m.put("description", description.trim());
        m.put("location", location.trim());
        m.put("startAt", new Timestamp(start));
        m.put("endAt", new Timestamp(end));
        m.put("updatedAt", FieldValue.serverTimestamp());
        write(s, owner, m, "schedule_updated", "Lịch tập thay đổi",
                "Buổi \"" + title.trim() + "\" được cập nhật: " + Fmt.dateTime(start) + ".", cb);
    }

    public void cancel(Schedule s, User owner, Callback<Void> cb) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", Schedule.CANCELLED);
        m.put("updatedAt", FieldValue.serverTimestamp());
        write(s, owner, m, "schedule_cancelled", "Lịch tập bị hủy",
                "Buổi \"" + s.title + "\" lúc " + Fmt.dateTime(s.startAt) + " đã bị hủy.", cb);
    }

    public void complete(Schedule s, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        Map<String, Object> m = new HashMap<>();
        m.put("status", Schedule.COMPLETED);
        m.put("updatedAt", FieldValue.serverTimestamp());
        Fb.db().collection("gyms").document(s.gymId).collection("schedules").document(s.id).update(m)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    private void write(Schedule s, User owner, Map<String, Object> fields, String type, String title, String body,
                       Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        FirebaseFirestore db = Fb.db();
        WriteBatch b = db.batch();
        b.update(db.collection("gyms").document(s.gymId).collection("schedules").document(s.id), fields);
        b.set(db.collection("users").document(s.memberUid).collection("notifications").document(),
                Repo.notification(s.memberUid, s.gymId, s.gymName, title, body, type, s.id, owner.uid));
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
