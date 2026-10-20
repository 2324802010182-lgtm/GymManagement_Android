package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Event;
import com.example.gymmanagement.util.Resource;

public class AuthViewModel extends BaseViewModel {
    private final MutableLiveData<Event<Resource<User>>> result = new MutableLiveData<>();
    private final MutableLiveData<Event<Resource<String>>> reset = new MutableLiveData<>();

    public LiveData<Event<Resource<User>>> result() {
        return result;
    }

    public LiveData<Event<Resource<String>>> reset() {
        return reset;
    }

    private Callback<User> cb() {
        result.setValue(new Event<>(Resource.<User>loading()));
        return new Callback<User>() {
            @Override
            public void onSuccess(User data) {
                result.setValue(new Event<>(Resource.success(data)));
            }

            @Override
            public void onError(AppError error) {
                result.setValue(new Event<>(Resource.<User>error(error)));
            }
        };
    }

    public void login(String email, String password) {
        Repos.auth().login(email, password, cb());
    }

    public void restoreSession() {
        Repos.auth().loadSession(cb());
    }

    public void registerMember(String name, String email, String phone, String password) {
        Repos.auth().registerMember(name, email, phone, password, cb());
    }

    public void registerOwner(String name, String email, String phone, String password, String note, String gymName) {
        Repos.auth().registerOwner(name, email, phone, password, note, gymName, cb());
    }

    public void sendReset(String email) {
        reset.setValue(new Event<>(Resource.<String>loading()));
        Repos.auth().sendPasswordReset(email, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                reset.setValue(new Event<>(Resource.success(email)));
            }

            @Override
            public void onError(AppError error) {
                reset.setValue(new Event<>(Resource.<String>error(error)));
            }
        });
    }

    public void updateProfile(String name, String phone) {
        runActionWith("Đã cập nhật hồ sơ.", (Callback<User> c) -> Repos.auth().updateProfile(name, phone, c));
    }

    public void updateGoal(Double weight, Integer weekly, String note) {
        runActionWith("Đã lưu mục tiêu.", (Callback<User> c) -> Repos.auth().updateGoal(weight, weekly, note, c));
    }
}
