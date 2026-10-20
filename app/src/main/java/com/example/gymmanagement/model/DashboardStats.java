package com.example.gymmanagement.model;

import java.util.ArrayList;
import java.util.List;

/** Số liệu thật của dashboard Chủ Gym (đọc từ Firestore). */
public class DashboardStats {
    public int totalMembers;
    public int activeMembers;
    public int activePackages;
    public int pendingRequests;
    public int attendanceToday;
    public List<Schedule> upcomingSchedules = new ArrayList<>();
    public List<MembershipRequest> recentRequests = new ArrayList<>();
}
