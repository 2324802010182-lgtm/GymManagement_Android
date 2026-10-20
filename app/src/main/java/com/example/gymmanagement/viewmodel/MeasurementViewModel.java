package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.Measurement;
import com.example.gymmanagement.util.Resource;

import java.util.Date;
import java.util.List;

/** F06 – chỉ số cơ thể. */
public class MeasurementViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<Measurement>>> list = new MutableLiveData<>();

    public LiveData<Resource<List<Measurement>>> list() {
        return list;
    }

    public void load() {
        list.setValue(Resource.<List<Measurement>>loading());
        Repos.measurements().list(intoList(list));
    }

    public void save(String id, double w, double h, Double fat, Date at, String note) {
        runAction(id == null || id.isEmpty() ? "Đã lưu chỉ số cơ thể." : "Đã cập nhật chỉ số.",
                c -> Repos.measurements().save(id, w, h, fat, at, note, c));
    }

    public void delete(Measurement m) {
        runAction("Đã xóa bản ghi.", c -> Repos.measurements().delete(m.id, c));
    }
}
