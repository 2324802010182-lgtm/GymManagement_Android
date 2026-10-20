package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.AppNotification;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;

import java.util.ArrayList;
import java.util.List;

/** F08 – thông báo cá nhân (hội viên và Chủ Gym). */
public class NotificationsViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<AppNotification>>> list = new MutableLiveData<>();
    private List<AppNotification> all = new ArrayList<>();
    private boolean unreadOnly;

    public LiveData<Resource<List<AppNotification>>> list() {
        return list;
    }

    public boolean unreadOnly() {
        return unreadOnly;
    }

    public void setUnreadOnly(boolean v) {
        unreadOnly = v;
        publish();
    }

    public void load() {
        list.setValue(Resource.<List<AppNotification>>loading());
        Repos.notifications().listMine(new Callback<List<AppNotification>>() {
            @Override
            public void onSuccess(List<AppNotification> data) {
                all = data;
                publish();
            }

            @Override
            public void onError(AppError error) {
                list.setValue(Resource.<List<AppNotification>>error(error));
            }
        });
    }

    private void publish() {
        List<AppNotification> out = new ArrayList<>();
        for (AppNotification n : all) if (!unreadOnly || !n.isRead) out.add(n);
        list.setValue(Resource.ofList(out));
    }

    public void markRead(AppNotification n) {
        if (n.isRead) return;
        Repos.notifications().markRead(n, new Callback<Void>() {
            @Override
            public void onSuccess(Void d) {
                n.isRead = true;
                publish();
            }

            @Override
            public void onError(AppError e) {
                action.setValue(new com.example.gymmanagement.util.Event<>(Resource.<String>error(e)));
            }
        });
    }

    public void markAllRead() {
        runAction("Đã đánh dấu tất cả là đã đọc.", c -> Repos.notifications().markAllRead(all, c));
        action.observeForever(new androidx.lifecycle.Observer<com.example.gymmanagement.util.Event<Resource<String>>>() {
            @Override
            public void onChanged(com.example.gymmanagement.util.Event<Resource<String>> e) {
                if (e == null) return;
                Resource<String> r = e.peek();
                if (r.status == Resource.Status.LOADING) return;
                action.removeObserver(this);
                if (r.status == Resource.Status.SUCCESS) {
                    for (AppNotification n : all) n.isRead = true;
                    publish();
                }
            }
        });
    }
}
