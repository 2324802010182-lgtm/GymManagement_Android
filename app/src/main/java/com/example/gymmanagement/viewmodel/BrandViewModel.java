package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Event;
import com.example.gymmanagement.util.Resource;

/** F09 – quản lý thương hiệu (tạo mới hoặc cập nhật). */
public class BrandViewModel extends BaseViewModel {
    private final MutableLiveData<Event<Resource<Gym>>> saved = new MutableLiveData<>();

    public LiveData<Event<Resource<Gym>>> saved() {
        return saved;
    }

    public void save(Gym draft) {
        saved.setValue(new Event<>(Resource.<Gym>loading()));
        Callback<Gym> cb = new Callback<Gym>() {
            @Override
            public void onSuccess(Gym g) {
                Session.ownerGym = g;
                saved.setValue(new Event<>(Resource.success(g)));
            }

            @Override
            public void onError(AppError e) {
                saved.setValue(new Event<>(Resource.<Gym>error(e)));
            }
        };
        if (Session.ownerGym == null) Repos.gyms().createGym(draft, Session.user, cb);
        else Repos.gyms().updateGym(Session.ownerGym, draft, cb);
    }
}
