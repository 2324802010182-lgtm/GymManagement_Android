package com.example.gymmanagement.data;

import com.example.gymmanagement.data.repository.AdminRepository;
import com.example.gymmanagement.data.repository.AttendanceRepository;
import com.example.gymmanagement.data.repository.AuthRepository;
import com.example.gymmanagement.data.repository.DashboardRepository;
import com.example.gymmanagement.data.repository.ExerciseRepository;
import com.example.gymmanagement.data.repository.GymRepository;
import com.example.gymmanagement.data.repository.MeasurementRepository;
import com.example.gymmanagement.data.repository.MembershipRepository;
import com.example.gymmanagement.data.repository.NotificationRepository;
import com.example.gymmanagement.data.repository.PackageRepository;
import com.example.gymmanagement.data.repository.ScheduleRepository;
import com.example.gymmanagement.data.repository.WorkoutRepository;

/** Điểm truy cập tập trung tới các repository (đơn giản thay cho DI). */
public final class Repos {
    private static final AuthRepository AUTH = new AuthRepository();
    private static final GymRepository GYMS = new GymRepository();
    private static final PackageRepository PACKAGES = new PackageRepository();
    private static final MembershipRepository MEMBERSHIPS = new MembershipRepository();
    private static final ScheduleRepository SCHEDULES = new ScheduleRepository();
    private static final AttendanceRepository ATTENDANCE = new AttendanceRepository();
    private static final NotificationRepository NOTIFICATIONS = new NotificationRepository();
    private static final ExerciseRepository EXERCISES = new ExerciseRepository();
    private static final MeasurementRepository MEASUREMENTS = new MeasurementRepository();
    private static final WorkoutRepository WORKOUTS = new WorkoutRepository();
    private static final DashboardRepository DASHBOARD = new DashboardRepository();
    private static final AdminRepository ADMIN = new AdminRepository();

    private Repos() {
    }

    public static AuthRepository auth() { return AUTH; }
    public static GymRepository gyms() { return GYMS; }
    public static PackageRepository packages() { return PACKAGES; }
    public static MembershipRepository memberships() { return MEMBERSHIPS; }
    public static ScheduleRepository schedules() { return SCHEDULES; }
    public static AttendanceRepository attendance() { return ATTENDANCE; }
    public static NotificationRepository notifications() { return NOTIFICATIONS; }
    public static ExerciseRepository exercises() { return EXERCISES; }
    public static MeasurementRepository measurements() { return MEASUREMENTS; }
    public static WorkoutRepository workouts() { return WORKOUTS; }
    public static DashboardRepository dashboard() { return DASHBOARD; }
    public static AdminRepository admin() { return ADMIN; }
}
