package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.Resource;

import java.util.List;

/** Quản trị nền tảng: duyệt Chủ Gym, tạm khóa/mở lại thương hiệu. */
public class AdminViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<User>>> owners = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Gym>>> gyms = new MutableLiveData<>();

    public LiveData<Resource<List<User>>> owners() {
        return owners;
    }

    public LiveData<Resource<List<Gym>>> gyms() {
        return gyms;
    }

    public void load() {
        owners.setValue(Resource.<List<User>>loading());
        gyms.setValue(Resource.<List<Gym>>loading());
        Repos.admin().listPendingOwners(intoList(owners));
        Repos.gyms().listAllGyms(intoList(gyms));
    }

    public void review(User u, boolean approve, String note) {
        runAction(approve ? "Đã duyệt Chủ Gym." : "Đã từ chối Chủ Gym.", c -> Repos.admin().reviewOwner(u, approve, note, c));
    }

    public void toggleGym(Gym g) {
        String next = g.status.equals(Gym.STATUS_SUSPENDED) ? Gym.STATUS_ACTIVE : Gym.STATUS_SUSPENDED;
        runAction(next.equals(Gym.STATUS_SUSPENDED) ? "Đã tạm khóa thương hiệu." : "Đã mở lại thương hiệu.",
                c -> Repos.gyms().setStatusAsAdmin(g, next, c));
    }
}
