package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.Attendance;
import com.example.gymmanagement.model.Measurement;
import com.example.gymmanagement.model.WorkoutLog;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.DateKeys;
import com.example.gymmanagement.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** F07 – tiến độ tập luyện: toàn bộ số liệu lấy từ workoutLogs, measurements và attendance. */
public class ProgressViewModel extends BaseViewModel {

    public static class Data {
        public List<WorkoutLog> logs = new ArrayList<>();
        public List<Measurement> measurements = new ArrayList<>();
        public List<Attendance> attendance = new ArrayList<>();
    }

    public static class Summary {
        public int sessions;
        public int minutes;
        public int attendanceCount;
        public Double firstWeight;
        public Double lastWeight;
        public Double firstBmi;
        public Double lastBmi;
        public List<Measurement> weightSeries = new ArrayList<>(); // tăng dần theo ngày
        public List<WorkoutLog> logsInRange = new ArrayList<>();
        public boolean hasAnyData;
        public int thisWeekSessions;
        public Double latestWeight;
    }

    public static final int RANGE_7 = 7;
    public static final int RANGE_30 = 30;
    public static final int RANGE_90 = 90;
    public static final int RANGE_ALL = 0;

    private final MutableLiveData<Resource<Data>> data = new MutableLiveData<>();
    private final MutableLiveData<Resource<Summary>> summary = new MutableLiveData<>();
    private int range = RANGE_30;
    private Data loaded;
    private int pending;
    private AppError failure;

    public LiveData<Resource<Summary>> summary() {
        return summary;
    }

    public int range() {
        return range;
    }

    public void setRange(int r) {
        range = r;
        publish();
    }

    public void load() {
        summary.setValue(Resource.<Summary>loading());
        loaded = new Data();
        pending = 3;
        failure = null;
        Repos.workouts().list(new Callback<List<WorkoutLog>>() {
            @Override
            public void onSuccess(List<WorkoutLog> d) {
                loaded.logs = d;
                done();
            }

            @Override
            public void onError(AppError e) {
                failure = e;
                done();
            }
        });
        Repos.measurements().list(new Callback<List<Measurement>>() {
            @Override
            public void onSuccess(List<Measurement> d) {
                loaded.measurements = d;
                done();
            }

            @Override
            public void onError(AppError e) {
                failure = e;
                done();
            }
        });
        Repos.attendance().listMine(new Callback<List<Attendance>>() {
            @Override
            public void onSuccess(List<Attendance> d) {
                loaded.attendance = d;
                done();
            }

            @Override
            public void onError(AppError e) {
                failure = e;
                done();
            }
        });
    }

    private void done() {
        if (--pending > 0) return;
        if (failure != null) {
            summary.setValue(Resource.<Summary>error(failure));
            return;
        }
        publish();
    }

    private void publish() {
        if (loaded == null) return;
        Date from = range == RANGE_ALL ? null : DateKeys.addDays(new Date(), -range);
        Summary s = new Summary();
        Date weekStart = startOfWeek();
        for (WorkoutLog w : loaded.logs) {
            if (w.completedAt != null && !w.completedAt.before(weekStart)) s.thisWeekSessions++;
        }
        if (!loaded.measurements.isEmpty()) s.latestWeight = loaded.measurements.get(0).weightKg;
        s.hasAnyData = !loaded.logs.isEmpty() || !loaded.measurements.isEmpty() || !loaded.attendance.isEmpty();
        for (WorkoutLog w : loaded.logs) {
            if (from != null && (w.completedAt == null || w.completedAt.before(from))) continue;
            s.sessions++;
            s.minutes += w.durationMinutes;
            s.logsInRange.add(w);
        }
        for (Attendance a : loaded.attendance) {
            if (from != null && (a.checkInAt == null || a.checkInAt.before(from))) continue;
            s.attendanceCount++;
        }
        List<Measurement> inRange = new ArrayList<>();
        for (int i = loaded.measurements.size() - 1; i >= 0; i--) { // danh sách gốc giảm dần → đảo thành tăng dần
            Measurement m = loaded.measurements.get(i);
            if (from != null && (m.measuredAt == null || m.measuredAt.before(from))) continue;
            inRange.add(m);
        }
        s.weightSeries = inRange;
        if (!inRange.isEmpty()) {
            s.firstWeight = inRange.get(0).weightKg;
            s.lastWeight = inRange.get(inRange.size() - 1).weightKg;
            s.firstBmi = inRange.get(0).bmi;
            s.lastBmi = inRange.get(inRange.size() - 1).bmi;
        }
        summary.setValue(s.hasAnyData ? Resource.success(s) : Resource.<Summary>empty());
    }

    private Date startOfWeek() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        int dow = c.get(java.util.Calendar.DAY_OF_WEEK);
        int back = dow == java.util.Calendar.SUNDAY ? 6 : dow - java.util.Calendar.MONDAY;
        c.add(java.util.Calendar.DAY_OF_YEAR, -back);
        return c.getTime();
    }
}
