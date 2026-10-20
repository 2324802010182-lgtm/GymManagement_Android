package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** F02 – trang chủ khám phá Gym: tải từ Firestore, lọc cục bộ theo tên/địa chỉ. */
public class HomeViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<Gym>>> gyms = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Membership>>> memberships = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<MembershipRequest>>> requests = new MutableLiveData<>();
    private final MutableLiveData<Integer> unread = new MutableLiveData<>(0);
    private List<Gym> all = new ArrayList<>();
    private String query = "";

    public LiveData<Resource<List<Gym>>> gyms() {
        return gyms;
    }

    public LiveData<Resource<List<Membership>>> memberships() {
        return memberships;
    }

    public LiveData<Resource<List<MembershipRequest>>> requests() {
        return requests;
    }

    public LiveData<Integer> unread() {
        return unread;
    }

    public String query() {
        return query;
    }

    public boolean hasQuery() {
        return !query.isEmpty();
    }

    public void load() {
        gyms.setValue(Resource.<List<Gym>>loading());
        Repos.gyms().listActiveGyms(new Callback<List<Gym>>() {
            @Override
            public void onSuccess(List<Gym> data) {
                all = data;
                publish();
                for (Gym g : data) {
                    Repos.gyms().countActivePackages(g.id, new Callback<Integer>() {
                        @Override
                        public void onSuccess(Integer n) {
                            g.activePackageCount = n;
                            publish();
                        }

                        @Override
                        public void onError(AppError error) {
                            // Không có số gói thì chỉ ẩn nhãn, không báo lỗi cả màn hình
                        }
                    });
                }
            }

            @Override
            public void onError(AppError error) {
                gyms.setValue(Resource.<List<Gym>>error(error));
            }
        });
    }

    public void search(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        publish();
    }

    private void publish() {
        List<Gym> out = new ArrayList<>();
        for (Gym g : all) {
            if (query.isEmpty() || g.searchText().contains(query)) out.add(g);
        }
        gyms.setValue(Resource.ofList(out));
    }

    public void loadMine() {
        Repos.memberships().listMyMemberships(intoList(memberships));
        Repos.memberships().listMyRequests(intoList(requests));
    }

    public void refreshUnread() {
        Repos.notifications().unreadCount(new Callback<Integer>() {
            @Override
            public void onSuccess(Integer n) {
                unread.setValue(n);
            }

            @Override
            public void onError(AppError error) {
                // giữ nguyên số cũ
            }
        });
    }
}
