package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.model.Schedule;
import com.example.gymmanagement.util.Resource;

import java.util.Date;
import java.util.List;

/** F04 – Chủ Gym tạo/sửa/hủy lịch và gán hội viên. */
public class OwnerScheduleViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<Schedule>>> list = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<MemberSummary>>> members = new MutableLiveData<>();

    public LiveData<Resource<List<Schedule>>> list() {
        return list;
    }

    public LiveData<Resource<List<MemberSummary>>> members() {
        return members;
    }

    public void load() {
        String gid = Session.gymId();
        if (gid == null) {
            list.setValue(Resource.<List<Schedule>>empty());
            return;
        }
        list.setValue(Resource.<List<Schedule>>loading());
        Repos.schedules().listForGym(gid, intoList(list));
        Repos.memberships().listMembers(gid, intoList(members));
    }

    public void create(MemberSummary m, String title, String desc, String loc, Date start, Date end) {
        runAction("Đã tạo lịch tập và gửi thông báo cho hội viên.",
                c -> Repos.schedules().create(Session.ownerGym, Session.user, m, title, desc, loc, start, end, c));
    }

    public void update(Schedule s, String title, String desc, String loc, Date start, Date end) {
        runAction("Đã cập nhật lịch tập.", c -> Repos.schedules().update(s, Session.user, title, desc, loc, start, end, c));
    }

    public void cancel(Schedule s) {
        runAction("Đã hủy lịch tập.", c -> Repos.schedules().cancel(s, Session.user, c));
    }

    public void complete(Schedule s) {
        runAction("Đã đánh dấu hoàn thành.", c -> Repos.schedules().complete(s, c));
    }
}
