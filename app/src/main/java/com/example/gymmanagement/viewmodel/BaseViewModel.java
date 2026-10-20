package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Event;
import com.example.gymmanagement.util.Resource;

import java.util.List;
import java.util.function.Consumer;

/** Lớp nền: kết quả thao tác ghi (action) và tiện ích nối Callback vào LiveData. */
public abstract class BaseViewModel extends ViewModel {
    /** Kết quả thao tác ghi gần nhất; data = thông báo thành công. */
    public final MutableLiveData<Event<Resource<String>>> action = new MutableLiveData<>();

    protected <T> Callback<T> into(MutableLiveData<Resource<T>> live) {
        return new Callback<T>() {
            @Override
            public void onSuccess(T data) {
                live.setValue(Resource.success(data));
            }

            @Override
            public void onError(AppError error) {
                live.setValue(Resource.error(error));
            }
        };
    }

    protected <E> Callback<List<E>> intoList(MutableLiveData<Resource<List<E>>> live) {
        return new Callback<List<E>>() {
            @Override
            public void onSuccess(List<E> data) {
                live.setValue(Resource.ofList(data));
            }

            @Override
            public void onError(AppError error) {
                live.setValue(Resource.error(error));
            }
        };
    }

    /** Chạy một thao tác ghi; chỉ báo thành công sau khi Firebase xác nhận. */
    protected void runAction(String okMessage, Consumer<Callback<Void>> starter) {
        action.setValue(new Event<>(Resource.<String>loading()));
        starter.accept(new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                action.setValue(new Event<>(Resource.success(okMessage)));
            }

            @Override
            public void onError(AppError error) {
                action.setValue(new Event<>(Resource.<String>error(error)));
            }
        });
    }

    protected <T> void runActionWith(String okMessage, Consumer<Callback<T>> starter) {
        action.setValue(new Event<>(Resource.<String>loading()));
        starter.accept(new Callback<T>() {
            @Override
            public void onSuccess(T data) {
                action.setValue(new Event<>(Resource.success(okMessage)));
            }

            @Override
            public void onError(AppError error) {
                action.setValue(new Event<>(Resource.<String>error(error)));
            }
        });
    }
}
