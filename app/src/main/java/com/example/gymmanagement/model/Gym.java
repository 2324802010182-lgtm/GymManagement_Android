package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** gyms/{gymId} – thương hiệu Gym. */
public class Gym implements java.io.Serializable {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_HIDDEN = "hidden";
    public static final String STATUS_SUSPENDED = "suspended";

    public String id = "";
    public String name = "";
    public String normalizedName = "";
    public String ownerUid = "";
    public String ownerName = "";
    public String address = "";
    public String area = "";
    public String phone = "";
    public String description = "";
    public String openingHours = "";
    public String logoUrl = "";
    public String coverUrl = "";
    public List<String> galleryUrls = new ArrayList<>();
    public String imageCredit = "";
    public String status = STATUS_ACTIVE;
    public boolean isSampleData;
    public Date createdAt;
    public Date updatedAt;

    /** Số gói đang mở bán (nạp sau, null nếu chưa biết). */
    public transient Integer activePackageCount;

    public static Gym from(DocumentSnapshot s) {
        Gym g = new Gym();
        g.id = s.getId();
        g.name = Docs.str(s, "name");
        g.normalizedName = Docs.str(s, "normalizedName");
        g.ownerUid = Docs.str(s, "ownerUid");
        g.ownerName = Docs.str(s, "ownerName");
        g.address = Docs.str(s, "address");
        g.area = Docs.str(s, "area");
        g.phone = Docs.str(s, "phone");
        g.description = Docs.str(s, "description");
        g.openingHours = Docs.str(s, "openingHours");
        g.logoUrl = Docs.str(s, "logoUrl");
        g.coverUrl = Docs.str(s, "coverUrl");
        g.galleryUrls = Docs.strList(s, "galleryUrls");
        g.imageCredit = Docs.str(s, "imageCredit");
        g.status = Docs.str(s, "status");
        g.isSampleData = Docs.bool(s, "isSampleData");
        g.createdAt = Docs.date(s, "createdAt");
        g.updatedAt = Docs.date(s, "updatedAt");
        return g;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public String statusLabel() {
        switch (status) {
            case STATUS_ACTIVE:
                return "Đang hoạt động";
            case STATUS_HIDDEN:
                return "Đang ẩn";
            case STATUS_SUSPENDED:
                return "Bị tạm khóa";
            default:
                return status;
        }
    }

    /** Chuỗi dùng để tìm kiếm theo tên / địa chỉ / khu vực. */
    public String searchText() {
        return (name + " " + address + " " + area).toLowerCase(java.util.Locale.ROOT);
    }
}
