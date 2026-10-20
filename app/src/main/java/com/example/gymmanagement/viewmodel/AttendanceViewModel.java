package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.Attendance;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;

import java.util.List;

/** Điểm danh (Chủ Gym). */
public class AttendanceViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<Attendance>>> today = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Attendance>>> recent = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<MemberSummary>>> members = new MutableLiveData<>();

    public LiveData<Resource<List<Attendance>>> today() {
        return today;
    }

    public LiveData<Resource<List<Attendance>>> recent() {
        return recent;
    }

    public LiveData<Resource<List<MemberSummary>>> members() {
        return members;
    }

    public void load() {
        String gid = Session.gymId();
        if (gid == null) {
            today.setValue(Resource.<List<Attendance>>empty());
            return;
        }
        today.setValue(Resource.<List<Attendance>>loading());
        recent.setValue(Resource.<List<Attendance>>loading());
        Repos.attendance().listToday(gid, intoList(today));
        Repos.attendance().listRecent(gid, intoList(recent));
        Repos.memberships().listMembers(gid, intoList(members));
    }

    public void checkIn(MemberSummary m) {
        runActionWith("Đã điểm danh cho " + m.fullName + ".",
                (Callback<Attendance> c) -> Repos.attendance().checkIn(Session.ownerGym, Session.user, m, c));
    }

    public void delete(Attendance a) {
        runAction("Đã xóa lượt điểm danh.", c -> Repos.attendance().delete(a, c));
    }
}
