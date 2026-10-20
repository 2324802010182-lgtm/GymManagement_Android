package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.DashboardStats;
import com.example.gymmanagement.util.Resource;

public class OwnerDashboardViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<DashboardStats>> stats = new MutableLiveData<>();
    private final MutableLiveData<Integer> unread = new MutableLiveData<>(0);

    public LiveData<Resource<DashboardStats>> stats() {
        return stats;
    }

    public LiveData<Integer> unread() {
        return unread;
    }

    public void load() {
        if (Session.gymId() == null) {
            stats.setValue(Resource.<DashboardStats>empty());
            return;
        }
        stats.setValue(Resource.<DashboardStats>loading());
        Repos.dashboard().load(Session.gymId(), into(stats));
        Repos.notifications().unreadCount(new com.example.gymmanagement.util.Callback<Integer>() {
            @Override
            public void onSuccess(Integer n) {
                unread.setValue(n);
            }

            @Override
            public void onError(com.example.gymmanagement.util.AppError e) {
            }
        });
    }
}
