package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.Attendance;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** F09 – quản lý hội viên của Gym. */
public class MembersViewModel extends BaseViewModel {
    public static final int FILTER_ALL = 0;
    public static final int FILTER_ACTIVE = 1;
    public static final int FILTER_EXPIRED = 2;

    private final MutableLiveData<Resource<List<MemberSummary>>> list = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Membership>>> memberships = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Attendance>>> attendance = new MutableLiveData<>();
    private List<MemberSummary> all = new ArrayList<>();
    private String query = "";
    private int filter = FILTER_ALL;

    public LiveData<Resource<List<MemberSummary>>> list() {
        return list;
    }

    public LiveData<Resource<List<Membership>>> memberships() {
        return memberships;
    }

    public LiveData<Resource<List<Attendance>>> attendance() {
        return attendance;
    }

    public List<MemberSummary> all() {
        return all;
    }

    public void load() {
        String gid = Session.gymId();
        if (gid == null) {
            list.setValue(Resource.<List<MemberSummary>>empty());
            return;
        }
        list.setValue(Resource.<List<MemberSummary>>loading());
        Repos.memberships().listMembers(gid, new Callback<List<MemberSummary>>() {
            @Override
            public void onSuccess(List<MemberSummary> data) {
                all = data;
                publish();
            }

            @Override
            public void onError(AppError e) {
                list.setValue(Resource.<List<MemberSummary>>error(e));
            }
        });
    }

    public void search(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        publish();
    }

    public void setFilter(int f) {
        filter = f;
        publish();
    }

    private void publish() {
        List<MemberSummary> out = new ArrayList<>();
        for (MemberSummary m : all) {
            if (filter == FILTER_ACTIVE && !m.isActiveNow()) continue;
            if (filter == FILTER_EXPIRED && m.isActiveNow()) continue;
            String hay = (m.fullName + " " + m.email + " " + m.phone).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !hay.contains(query)) continue;
            out.add(m);
        }
        list.setValue(Resource.ofList(out));
    }

    public void loadDetail(String memberUid) {
        String gid = Session.gymId();
        memberships.setValue(Resource.<List<Membership>>loading());
        attendance.setValue(Resource.<List<Attendance>>loading());
        Repos.memberships().listMemberMemberships(gid, memberUid, intoList(memberships));
        Repos.attendance().listForMember(gid, memberUid, intoList(attendance));
    }

    public void cancelMembership(Membership m, String reason) {
        runAction("Đã hủy gói tập của hội viên.", c -> Repos.memberships().cancelMembership(m, Session.user, reason, c));
    }
}
