package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.util.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** F03 – quản lý gói tập (Chủ Gym). */
public class PackagesViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<GymPackage>>> list = new MutableLiveData<>();
    private List<GymPackage> all = new ArrayList<>();
    private String query = "";

    public LiveData<Resource<List<GymPackage>>> list() {
        return list;
    }

    public void load() {
        String gid = Session.gymId();
        if (gid == null) {
            list.setValue(Resource.<List<GymPackage>>empty());
            return;
        }
        list.setValue(Resource.<List<GymPackage>>loading());
        Repos.packages().listAll(gid, new com.example.gymmanagement.util.Callback<List<GymPackage>>() {
            @Override
            public void onSuccess(List<GymPackage> data) {
                all = data;
                publish();
            }

            @Override
            public void onError(com.example.gymmanagement.util.AppError e) {
                list.setValue(Resource.<List<GymPackage>>error(e));
            }
        });
    }

    public void search(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        publish();
    }

    private void publish() {
        List<GymPackage> out = new ArrayList<>();
        for (GymPackage p : all) {
            if (query.isEmpty() || p.name.toLowerCase(Locale.ROOT).contains(query)) out.add(p);
        }
        list.setValue(Resource.ofList(out));
    }

    public void save(GymPackage p, boolean isNew) {
        String gid = Session.gymId();
        runAction(isNew ? "Đã thêm gói tập." : "Đã cập nhật gói tập.", c -> {
            if (isNew) Repos.packages().create(gid, p, c);
            else Repos.packages().update(gid, p, c);
        });
    }

    public void toggle(GymPackage p) {
        String gid = Session.gymId();
        runAction(p.isActive ? "Đã tắt gói tập." : "Đã bật gói tập.",
                c -> Repos.packages().setActive(gid, p, !p.isActive, c));
    }

    public void delete(GymPackage p) {
        String gid = Session.gymId();
        runAction("Đã xóa gói tập.", c -> Repos.packages().delete(gid, p, c));
    }
}
