package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.DashboardStats;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.model.Schedule;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.DateKeys;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Số liệu thật cho dashboard Chủ Gym: mọi con số đều đến từ truy vấn Firestore. */
public class DashboardRepository {

    public void load(String gymId, Callback<DashboardStats> cb) {
        if (!Repo.guard(cb)) return;
        DocumentReference g = Fb.db().collection("gyms").document(gymId);
        Task<AggregateQuerySnapshot> tMembers = g.collection("members").count().get(AggregateSource.SERVER);
        Task<QuerySnapshot> tActive = g.collection("memberships").whereEqualTo("status", Membership.ACTIVE).get();
        Task<AggregateQuerySnapshot> tPackages = g.collection("packages").whereEqualTo("isActive", true)
                .count().get(AggregateSource.SERVER);
        Task<AggregateQuerySnapshot> tPending = g.collection("membershipRequests")
                .whereEqualTo("status", MembershipRequest.PENDING).count().get(AggregateSource.SERVER);
        Task<AggregateQuerySnapshot> tToday = g.collection("attendance").whereEqualTo("dateKey", DateKeys.today())
                .count().get(AggregateSource.SERVER);
        Task<QuerySnapshot> tSchedules = g.collection("schedules").whereEqualTo("status", Schedule.SCHEDULED).get();
        Task<QuerySnapshot> tRecent = g.collection("membershipRequests")
                .orderBy("updatedAt", Query.Direction.DESCENDING).limit(5).get();

        Tasks.whenAll(tMembers, tActive, tPackages, tPending, tToday, tSchedules, tRecent)
                .addOnSuccessListener(v -> {
                    DashboardStats s = new DashboardStats();
                    s.totalMembers = (int) tMembers.getResult().getCount();
                    Set<String> active = new HashSet<>();
                    for (Membership m : Repo.map(tActive.getResult(), Membership::from)) {
                        if (m.isValidNow()) active.add(m.memberUid);
                    }
                    s.activeMembers = active.size();
                    s.activePackages = (int) tPackages.getResult().getCount();
                    s.pendingRequests = (int) tPending.getResult().getCount();
                    s.attendanceToday = (int) tToday.getResult().getCount();
                    List<Schedule> up = new ArrayList<>();
                    Date now = new Date();
                    for (Schedule x : Repo.map(tSchedules.getResult(), Schedule::from)) {
                        if (x.endAt != null && x.endAt.after(now)) up.add(x);
                    }
                    Collections.sort(up, (a, b) -> Repo.cmpAsc(a.startAt, b.startAt));
                    s.upcomingSchedules = up.size() > 5 ? new ArrayList<>(up.subList(0, 5)) : up;
                    s.recentRequests = Repo.map(tRecent.getResult(), MembershipRequest::from);
                    cb.onSuccess(s);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
