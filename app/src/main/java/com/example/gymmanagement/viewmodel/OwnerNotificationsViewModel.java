package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.AppNotification;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;

import java.util.List;

/** Chủ Gym gửi thông báo chung và xem lịch sử thông báo đã gửi. */
public class OwnerNotificationsViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<AppNotification>>> history = new MutableLiveData<>();

    public LiveData<Resource<List<AppNotification>>> history() {
        return history;
    }

    public void load() {
        String gid = Session.gymId();
        if (gid == null) {
            history.setValue(Resource.<List<AppNotification>>empty());
            return;
        }
        history.setValue(Resource.<List<AppNotification>>loading());
        Repos.notifications().listGymAnnouncements(gid, intoList(history));
    }

    public void send(String title, String body, boolean activeOnly) {
        runAction("Đã gửi thông báo.", c -> Repos.memberships().listMembers(Session.gymId(), new Callback<List<MemberSummary>>() {
            @Override
            public void onSuccess(List<MemberSummary> members) {
                Repos.notifications().sendAnnouncement(Session.ownerGym, Session.user, title, body, activeOnly,
                        members, new Callback<Integer>() {
                            @Override
                            public void onSuccess(Integer n) {
                                c.onSuccess(null);
                            }

                            @Override
                            public void onError(AppError e) {
                                c.onError(e);
                            }
                        });
            }

            @Override
            public void onError(AppError e) {
                c.onError(e);
            }
        }));
    }
}
