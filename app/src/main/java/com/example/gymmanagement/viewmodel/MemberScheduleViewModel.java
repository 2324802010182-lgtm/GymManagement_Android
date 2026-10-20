package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.Schedule;
import com.example.gymmanagement.util.Resource;

import java.util.List;

/** F04 – lịch tập của hội viên (chỉ các buổi được gán cho mình). */
public class MemberScheduleViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<Schedule>>> list = new MutableLiveData<>();

    public LiveData<Resource<List<Schedule>>> list() {
        return list;
    }

    public void load() {
        list.setValue(Resource.<List<Schedule>>loading());
        Repos.schedules().listMine(intoList(list));
    }
}
