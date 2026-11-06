package com.example.gymmanagement.hoat_dong;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ActivityLoginBinding;
import com.example.gymmanagement.databinding.ActivityRegisterMemberBinding;
import com.example.gymmanagement.databinding.ActivityRegisterOwnerBinding;
import com.example.gymmanagement.databinding.FragmentProfileBinding;
import com.example.gymmanagement.databinding.ScreenBrandSetupBinding;
import com.example.gymmanagement.data.ExerciseCatalog;
import com.example.gymmanagement.du_lieu.KhoDuLieu;
import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.ui.components.VideoPlayerView;
import com.example.gymmanagement.ui.components.MemberBarcodeView;
import com.example.gymmanagement.util.ImageLoader;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Ứng dụng GymManagement - UI Android native, Java, XML.
 * Các màn hình hiện được điều hướng qua Activity này; dữ liệu được lấy từ Firebase.
 */
public class MainActivity extends AppCompatActivity {

    private static final int DO = Color.rgb(229, 57, 53);
    private static final int DEN = Color.rgb(18, 18, 18);
    private static final int DEN_THE = Color.rgb(30, 30, 30);
    private static final int DEN_PHU = Color.rgb(42, 42, 42);
    private static final int TRANG = Color.WHITE;
    private static final int XAM = Color.rgb(189, 189, 189);
    private static final int XAM_DAM = Color.rgb(85, 85, 85);

    private KhoDuLieu kho;
    private FrameLayout khungChinh;
    private LinearLayout thanTrang;
    private String manHinhHienTai = "";
    private String uid = "";
    private Map<String, Object> nguoiDung = new HashMap<>();
    private String tenGymHienTai = "Gym của bạn";
    private final List<Exercise> exerciseCatalog = new ArrayList<>();
    private String exerciseCategory = "";
    private String exerciseDifficulty = "";
    private String exerciseQuery = "";
    private String exerciseEquipmentFilter = "";
    private String exerciseMode = "Phòng gym";
    private String exerciseSidebarCategory = "Ngực";
    private boolean exerciseOnlyVideo;
    private final Set<String> exerciseFavorites = new HashSet<>();
    private LinearLayout exerciseFilterHost;
    private LinearLayout exerciseListHost;
    private TextView exerciseCount;
    private VideoPlayerView activeExerciseVideo;
    private String boLocThongBao = "Tất cả";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        khungChinh = findViewById(R.id.khung_chinh);
        kho = KhoDuLieu.lay(this);
        if (!kho.dangDungFirebase()) {
            hienCauHinhFirebase();
            return;
        }
        kiemTraPhienDangNhap();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (activeExerciseVideo != null) activeExerciseVideo.start();
    }

    @Override
    protected void onStop() {
        if (activeExerciseVideo != null) activeExerciseVideo.stop();
        super.onStop();
    }

    /** Chặn toàn bộ luồng ứng dụng khi chưa liên kết Firebase thật. */
    private void hienCauHinhFirebase() {
        taoTrang("CẦN KẾT NỐI FIREBASE", "GymManagement chỉ sử dụng dữ liệu thật", null, false);
        themChu("Chế độ dữ liệu demo đã được tắt. Ứng dụng chưa tìm thấy cấu hình Firebase hợp lệ nên chưa cho phép đăng nhập, đăng ký hoặc lưu dữ liệu.", 14, TRANG, false);
        the("BƯỚC 1 · TẠO FIREBASE PROJECT", "Mở Firebase Console và tạo một project mới cho GymManagement.");
        the("BƯỚC 2 · ĐĂNG KÝ ỨNG DỤNG ANDROID", "Package name phải chính xác: com.example.gymmanagement");
        the("BƯỚC 3 · THÊM FILE CẤU HÌNH", "Đặt file google-services.json vào thư mục app/ của project, không đổi tên file.");
        the("BƯỚC 4 · BẬT DỊCH VỤ", "Bật Authentication → Email/Password, tạo Cloud Firestore và triển khai firestore.rules trong project.");
        the("BƯỚC 5 · ĐỒNG BỘ", "Chọn File → Sync Project with Gradle Files, sau đó chạy lại ứng dụng.");
        nut("MỞ FIREBASE CONSOLE", true, () -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://console.firebase.google.com/")));
            } catch (Exception e) {
                baoLoi("Không mở được trình duyệt. Hãy truy cập https://console.firebase.google.com/ bằng trình duyệt.");
            }
        });
    }

    private void kiemTraPhienDangNhap() {
        uid = kho.uidHienTai();
        if (TextUtils.isEmpty(uid)) {
            hienDangNhap();
            return;
        }
        kho.layMot("nguoi_dung", uid, new KhoDuLieu.KetQua<Map<String, Object>>() {
            @Override
            public void thanhCong(Map<String, Object> data) {
                if (data == null) {
                    kho.dangXuat();
                    hienDangNhap();
                    return;
                }
                if (Boolean.FALSE.equals(data.get("trang_thai"))) {
                    kho.dangXuat();
                    thongBao("Tài khoản đang bị khóa.");
                    hienDangNhap();
                    return;
                }
                nguoiDung = data;
                if (laQuanTri() && TextUtils.isEmpty(gymId())) hienTaoThuongHieu();
                else hienBangDieuKhien();
            }

            @Override
            public void thatBai(String thongBao) {
                kho.dangXuat();
                hienDangNhap();
                thongBao("Không tải được hồ sơ: " + thongBao);
            }
        });
    }

    // ---------------------------------------------------------------------
    // KHUNG GIAO DIỆN VÀ THÀNH PHẦN DÙNG CHUNG
    // ---------------------------------------------------------------------

    private void taoTrang(String tieuDe, String moTa, Runnable quayLai, boolean coThanhDieuHuong) {
        if (activeExerciseVideo != null) {
            activeExerciseVideo.stop();
            activeExerciseVideo = null;
        }
        String cleanTitle = tieuDe == null ? "" : tieuDe.replace("Xin chào, ", "");
        manHinhHienTai = cleanTitle;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(DEN);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        LinearLayout thanhTieuDe = new LinearLayout(this);
        thanhTieuDe.setOrientation(LinearLayout.HORIZONTAL);
        thanhTieuDe.setGravity(Gravity.CENTER_VERTICAL);
        thanhTieuDe.setPadding(dp(16), dp(12), dp(16), dp(12));

        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF100D0E, 0xFF351416, 0xFF100D0E}
        );
        thanhTieuDe.setBackground(headerBg);

        if (quayLai != null) {
            Button back = taoNut("‹", false, quayLai);
            LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(46), dp(46));
            backParams.setMargins(0, 0, dp(12), 0);
            thanhTieuDe.addView(back, backParams);
        } else {
            FrameLayout avatarFrame = new FrameLayout(this);
            ImageView avatarImg = new ImageView(this);
            avatarImg.setScaleType(ImageView.ScaleType.CENTER_CROP);

            String avatarUrl = nguoiDung != null ? KhoDuLieu.chuoi(nguoiDung.get("anh_dai_dien")) : "";
            if (!avatarUrl.isEmpty()) {
                ImageLoader.load(avatarImg, avatarUrl, R.drawable.bg_circle_surface);
                GradientDrawable circleBg = new GradientDrawable();
                circleBg.setShape(GradientDrawable.OVAL);
                avatarImg.setBackground(circleBg);
                avatarImg.setClipToOutline(true);
            } else {
                TextView tvLetter = new TextView(this);
                String nameLetter = cleanTitle.isEmpty() ? "G" : cleanTitle.substring(0, 1).toUpperCase(Locale.ROOT);
                tvLetter.setText(nameLetter);
                tvLetter.setTextColor(TRANG);
                tvLetter.setTextSize(16);
                tvLetter.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                tvLetter.setGravity(Gravity.CENTER);

                GradientDrawable circleBg = new GradientDrawable();
                circleBg.setShape(GradientDrawable.OVAL);
                circleBg.setColor(0xFFE53935);
                tvLetter.setBackground(circleBg);

                avatarFrame.addView(tvLetter, new FrameLayout.LayoutParams(dp(42), dp(42)));
            }
            avatarFrame.addView(avatarImg, new FrameLayout.LayoutParams(dp(42), dp(42)));
            avatarFrame.setOnClickListener(v -> hienHoSo());

            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(42), dp(42));
            avatarParams.setMargins(0, 0, dp(12), 0);
            thanhTieuDe.addView(avatarFrame, avatarParams);
        }

        LinearLayout cotTieuDe = new LinearLayout(this);
        cotTieuDe.setOrientation(LinearLayout.VERTICAL);
        cotTieuDe.setGravity(Gravity.CENTER_VERTICAL);
        cotTieuDe.setOnClickListener(v -> hienHoSo());

        TextView title = chu(cleanTitle, 19, TRANG, true);
        TextView subtitle = chu(moTa == null ? "" : moTa, 12, 0xFFCBD5E1, false);
        cotTieuDe.addView(title);
        if (!TextUtils.isEmpty(moTa)) {
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
            subParams.topMargin = dp(2);
            cotTieuDe.addView(subtitle, subParams);
        }
        thanhTieuDe.addView(cotTieuDe, new LinearLayout.LayoutParams(0, -2, 1));

        if (!TextUtils.isEmpty(uid)) {
            ImageView bell = new ImageView(this);
            bell.setImageResource(R.drawable.ic_notifications);
            bell.setColorFilter(0xFFFF5A60);
            bell.setPadding(dp(10), dp(10), dp(10), dp(10));
            bell.setOnClickListener(v -> hienThongBao());
            thanhTieuDe.addView(bell, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }

        root.addView(thanhTieuDe, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        root.addView(scroll, scrollParams);

        thanTrang = new LinearLayout(this);
        thanTrang.setOrientation(LinearLayout.VERTICAL);
        thanTrang.setPadding(dp(18), dp(18), dp(18), dp(24));
        scroll.addView(thanTrang, new ScrollView.LayoutParams(-1, -2));

        if (coThanhDieuHuong && !TextUtils.isEmpty(uid)) {
            root.addView(taoThanhDieuHuong(), new LinearLayout.LayoutParams(-1, -2));
        }

        khungChinh.removeAllViews();
        khungChinh.addView(root, new FrameLayout.LayoutParams(-1, -1));
    }

    private LinearLayout taoThanhDieuHuong() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        GradientDrawable navBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF141419, 0xFF20202A, 0xFF101014}
        );
        navBg.setCornerRadii(new float[]{dp(20), dp(20), dp(20), dp(20), 0, 0, 0, 0});
        navBg.setStroke(dp(1), 0xFF393942);
        nav.setBackground(navBg);
        nav.setPadding(dp(5), dp(5), dp(5), dp(5));
        
        String[] labels = laQuanTri()
                ? new String[]{"Trang chủ", "Hội viên", "Doanh thu", "Hồ sơ"}
                : new String[]{"Trang chủ", "Thư viện", "Lịch tập", "Tiến độ", "Hồ sơ"};
        int[] icons = laQuanTri()
                ? new int[]{R.drawable.ic_home, R.drawable.ic_group, R.drawable.ic_chart, R.drawable.ic_person}
                : new int[]{R.drawable.ic_home, R.drawable.ic_fitness, R.drawable.ic_calendar, R.drawable.ic_chart, R.drawable.ic_person};

        Runnable[] actions = laQuanTri()
                ? new Runnable[]{this::hienBangDieuKhien, this::hienHoiVien, this::hienDoanhThu, this::hienHoSo}
                : new Runnable[]{this::hienBangDieuKhien, this::hienThuVienBaiTap, this::hienLichTap, this::hienTienDo, this::hienHoSo};
        
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            boolean isHome = index == 0 && ("GYM MANAGEMENT".equals(manHinhHienTai) || tenGymHienTai.equals(manHinhHienTai));
            boolean isActiveTab = laQuanTri()
                    ? isHome
                        || (index == 1 && "QUẢN LÝ HỘI VIÊN".equals(manHinhHienTai))
                        || (index == 2 && "DOANH THU".equals(manHinhHienTai))
                        || (index == 3 && "HỒ SƠ CÁ NHÂN".equals(manHinhHienTai))
                    : isHome
                        || (index == 1 && (manHinhHienTai.contains("BÀI TẬP") || manHinhHienTai.contains("THƯ VIỆN")))
                        || (index == 2 && "LỊCH TẬP".equals(manHinhHienTai))
                        || (index == 3 && manHinhHienTai.contains("TIẾN ĐỘ"))
                        || (index == 4 && "HỒ SƠ CÁ NHÂN".equals(manHinhHienTai));
            int tint = isActiveTab ? 0xFFFF5A60 : 0xFF96949A;
            ImageView icon = new ImageView(this);
            icon.setImageResource(icons[i]);
            icon.setColorFilter(tint);
            item.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
            TextView label = chu(labels[i], 10, tint, isActiveTab);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
            labelParams.topMargin = dp(2);
            item.addView(label, labelParams);
            item.setBackground(nenVien(isActiveTab ? 0xFF2C171B : 0x00101010, isActiveTab ? 0xFF6E3439 : 0x00101010, 12));
            item.setContentDescription(labels[i]);
            item.setOnClickListener(v -> actions[index].run());
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(52), 1));
        }
        return nav;
    }

    private TextView chu(String value, int size, int color, boolean dam) {
        TextView tv = new TextView(this);
        tv.setText(value == null ? "" : value);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setIncludeFontPadding(true);
        if (dam) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private TextView themChu(String value, int size, int color, boolean dam) {
        TextView tv = chu(value, size, color, dam);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        thanTrang.addView(tv, p);
        return tv;
    }

    private EditText oNhap(String hint, int inputType, String giaTri) {
        EditText edit = new EditText(this);
        edit.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        edit.setTextSize(15);
        edit.setTextColor(TRANG);
        edit.setHintTextColor(XAM);
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setPadding(dp(14), dp(11), dp(14), dp(11));
        edit.setBackground(nenVien(DEN_THE, Color.rgb(67, 67, 67), 12));
        edit.setMinHeight(dp(49));
        if (!TextUtils.isEmpty(giaTri)) edit.setText(giaTri);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(13);
        thanTrang.addView(edit, p);
        return edit;
    }

    private EditText truong(String nhan, String goiY, int inputType, String giaTri) {
        themChu(nhan, 13, XAM, true);
        return oNhap(goiY, inputType, giaTri);
    }

    private Button taoNut(String value, boolean chinh, Runnable hanhDong) {
        Button btn = new Button(this);
        btn.setText(value);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(TRANG);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setPadding(dp(12), dp(8), dp(12), dp(8));
        btn.setMinHeight(dp(48));
        btn.setBackground(nenVien(chinh ? DO : DEN_PHU, chinh ? DO : Color.rgb(67, 67, 67), 12));
        if (hanhDong != null) btn.setOnClickListener(v -> hanhDong.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(10);
        btn.setLayoutParams(p);
        return btn;
    }

    private Button nut(String value, boolean chinh, Runnable hanhDong) {
        Button btn = taoNut(value, chinh, hanhDong);
        if (thanTrang != null) thanTrang.addView(btn);
        return btn;
    }

    private Button taoNutNho(String value, boolean chinh, Runnable hanhDong) {
        Button btn = taoNut(value, chinh, hanhDong);
        btn.setTextSize(12);
        btn.setMinHeight(dp(40));
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        return btn;
    }

    private Button nutNho(String value, boolean chinh, Runnable hanhDong) {
        Button btn = taoNutNho(value, chinh, hanhDong);
        if (thanTrang != null) thanTrang.addView(btn);
        return btn;
    }

    private LinearLayout taoThe(LinearLayout parent, String tieuDe, String noiDung) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(7));
        card.setBackground(nenVien(DEN_THE, Color.rgb(47, 47, 47), 15));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(12);
        parent.addView(card, cp);
        if (!TextUtils.isEmpty(tieuDe)) {
            TextView t = chu(tieuDe, 16, TRANG, true);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
            tp.bottomMargin = dp(7);
            card.addView(t, tp);
        }
        if (!TextUtils.isEmpty(noiDung)) {
            TextView d = chu(noiDung, 13, XAM, false);
            d.setLineSpacing(dp(2), 1.0f);
            LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, -2);
            dpv.bottomMargin = dp(8);
            card.addView(d, dpv);
        }
        return card;
    }

    private LinearLayout the(String tieuDe, String noiDung) {
        return taoThe(thanTrang, tieuDe, noiDung);
    }

    private LinearLayout theTrong(LinearLayout parent, String tieuDe, String noiDung) {
        return taoThe(parent, tieuDe, noiDung);
    }

    private void themNutVaoThe(LinearLayout card, String text, boolean chinh, Runnable action) {
        Button b = taoNutNho(text, chinh, action);
        card.addView(b);
    }

    private Spinner spinner(List<String> values) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        sp.setBackground(nenVien(DEN_THE, Color.rgb(67, 67, 67), 10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(50));
        p.bottomMargin = dp(14);
        thanTrang.addView(sp, p);
        return sp;
    }

    private void themDuongKe() {
        View line = new View(this);
        line.setBackgroundColor(Color.rgb(54, 54, 54));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
        p.topMargin = dp(5);
        p.bottomMargin = dp(14);
        thanTrang.addView(line, p);
    }

    private GradientDrawable nenVien(int fill, int stroke, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void thongBao(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void baoLoi(String msg) {
        thongBao(TextUtils.isEmpty(msg) ? "Có lỗi xảy ra. Vui lòng thử lại." : msg);
    }

    private String gymId() {
        return KhoDuLieu.chuoi(nguoiDung.get("gym_id"));
    }

    private boolean laQuanTri() {
        return "quan_tri_gym".equals(KhoDuLieu.chuoi(nguoiDung.get("vai_tro")));
    }

    private String tenNguoiDung() {
        String name = KhoDuLieu.chuoi(nguoiDung.get("ho_ten"));
        return name.isEmpty() ? "bạn" : name;
    }

    private String tien(long amount) {
        NumberFormat f = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return f.format(amount) + " VNĐ";
    }

    private String ngay(long epoch) {
        if (epoch <= 0) return "Chưa cập nhật";
        return new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(epoch));
    }

    private String ngayHomNay() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private long laySo(Map<String, Object> map, String key) {
        return KhoDuLieu.soLong(map.get(key));
    }

    private List<Map<String, Object>> locGym(List<Map<String, Object>> input) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> item : input) if (gymId().equals(KhoDuLieu.chuoi(item.get("gym_id")))) out.add(item);
        return out;
    }

    private void taiGymSauDangNhap(Runnable tiepTuc) {
        if (TextUtils.isEmpty(gymId())) {
            tenGymHienTai = "Chưa chọn thương hiệu Gym";
            tiepTuc.run();
            return;
        }
        kho.layMot("phong_gym", gymId(), new KhoDuLieu.KetQua<Map<String, Object>>() {
            @Override
            public void thanhCong(Map<String, Object> gym) {
                if (gym != null) {
                    tenGymHienTai = KhoDuLieu.chuoi(gym.get("ten_thuong_hieu"));
                    if (tenGymHienTai.isEmpty()) tenGymHienTai = "Gym của bạn";
                    nguoiDung.put("ten_thuong_hieu", tenGymHienTai);
                } else {
                    tenGymHienTai = KhoDuLieu.chuoi(nguoiDung.get("ten_thuong_hieu"));
                }
                tiepTuc.run();
            }

            @Override
            public void thatBai(String thongBao) {
                tenGymHienTai = KhoDuLieu.chuoi(nguoiDung.get("ten_thuong_hieu"));
                tiepTuc.run();
            }
        });
    }

    // ---------------------------------------------------------------------
    // F01 - ĐĂNG NHẬP / ĐĂNG KÝ / ĐẶT LẠI MẬT KHẨU
    // ---------------------------------------------------------------------

    private void hienManHinh(View content, String title) {
        manHinhHienTai = title;
        thanTrang = null;
        khungChinh.removeAllViews();
        khungChinh.addView(content, new FrameLayout.LayoutParams(-1, -1));
    }

    private void hienDangNhap() {
        ActivityLoginBinding login = ActivityLoginBinding.inflate(getLayoutInflater());
        hienManHinh(login.getRoot(), "GYM MANAGEMENT");
        login.loginButton.setOnClickListener(v -> {
            login.loginEmailLayout.setError(null);
            login.loginPasswordLayout.setError(null);
            login.loginError.setVisibility(View.GONE);
            String email = String.valueOf(login.loginEmail.getText()).trim();
            String password = String.valueOf(login.loginPassword.getText());
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                login.loginEmailLayout.setError(getString(R.string.error_email_invalid));
                login.loginEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                login.loginPasswordLayout.setError(getString(R.string.error_required));
                login.loginPassword.requestFocus();
                return;
            }
            login.loginProgress.setVisibility(View.VISIBLE);
            login.loginButton.setEnabled(false);
            kho.dangNhap(email, password, new KhoDuLieu.KetQua<Map<String, Object>>() {
                @Override public void thanhCong(Map<String, Object> data) {
                    if (Boolean.FALSE.equals(data.get("trang_thai"))) {
                        kho.dangXuat();
                        login.loginProgress.setVisibility(View.GONE);
                        login.loginButton.setEnabled(true);
                        login.loginError.setText("Tài khoản đã bị khóa.");
                        login.loginError.setVisibility(View.VISIBLE);
                        return;
                    }
                    uid = KhoDuLieu.chuoi(data.get("uid"));
                    if (uid.isEmpty()) uid = KhoDuLieu.chuoi(data.get("id"));
                    nguoiDung = data;
                    taiGymSauDangNhap(() -> {
                        if (laQuanTri() && TextUtils.isEmpty(gymId())) hienTaoThuongHieu();
                        else hienBangDieuKhien();
                    });
                }
                @Override public void thatBai(String error) {
                    login.loginProgress.setVisibility(View.GONE);
                    login.loginButton.setEnabled(true);
                    login.loginError.setText(error);
                    login.loginError.setVisibility(View.VISIBLE);
                }
            });
        });
        login.loginForgot.setOnClickListener(v -> hienDatLaiMatKhau());
        login.loginRegisterMember.setOnClickListener(v -> hienDangKy(false));
        login.loginRegisterOwner.setOnClickListener(v -> hienDangKy(true));
    }
    private void nutLoading(String message) {
        ProgressBar bar = new ProgressBar(this);
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(DO));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(36), dp(36));
        p.gravity = Gravity.CENTER_HORIZONTAL;
        p.bottomMargin = dp(8);
        thanTrang.addView(bar, p);
        themChu(message, 12, XAM, false);
    }

    private void hienDatLaiMatKhau() {
        taoTrang("ĐẶT LẠI MẬT KHẨU", "Nhận email hướng dẫn khôi phục tài khoản", this::hienDangNhap, false);
        EditText email = truong("Email đăng ký", "name@example.com", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, "");
        nut("GỬI HƯỚNG DẪN", true, () -> {
            String e = email.getText().toString().trim();
            if (e.isEmpty()) { email.setError("Vui lòng nhập email"); return; }
            kho.guiEmailDatLaiMatKhau(e, new KhoDuLieu.KetQua<Boolean>() {
                @Override public void thanhCong(Boolean ok) {
                    thongBao("Nếu email tồn tại, Firebase sẽ gửi hướng dẫn đặt lại mật khẩu.");
                    hienDangNhap();
                }
                @Override public void thatBai(String thongBao) { baoLoi(thongBao); }
            });
        });
    }

    private void hienDangKy(boolean owner) {
        if (owner) {
            ActivityRegisterOwnerBinding register = ActivityRegisterOwnerBinding.inflate(getLayoutInflater());
            register.regGymLayout.setVisibility(View.GONE);
            register.regNoteLayout.setVisibility(View.GONE);
            register.regHint.setText(R.string.register_owner_hint);
            hienManHinh(register.getRoot(), getString(R.string.register_owner_title));
            register.toolbar.setNavigationOnClickListener(v -> hienDangNhap());
            ganSuKienDangKy(true, register.regNameLayout, register.regName,
                    register.regEmailLayout, register.regEmail,
                    register.regPhoneLayout, register.regPhone,
                    register.regPasswordLayout, register.regPassword,
                    register.regConfirmLayout, register.regConfirm,
                    register.regError, register.regProgress, register.regButton);
        } else {
            ActivityRegisterMemberBinding register = ActivityRegisterMemberBinding.inflate(getLayoutInflater());
            hienManHinh(register.getRoot(), getString(R.string.register_member_title));
            register.toolbar.setNavigationOnClickListener(v -> hienDangNhap());
            ganSuKienDangKy(false, register.regNameLayout, register.regName,
                    register.regEmailLayout, register.regEmail,
                    register.regPhoneLayout, register.regPhone,
                    register.regPasswordLayout, register.regPassword,
                    register.regConfirmLayout, register.regConfirm,
                    register.regError, register.regProgress, register.regButton);
        }
    }

    private void ganSuKienDangKy(boolean owner,
                                 com.google.android.material.textfield.TextInputLayout nameLayout, EditText name,
                                 com.google.android.material.textfield.TextInputLayout emailLayout, EditText email,
                                 com.google.android.material.textfield.TextInputLayout phoneLayout, EditText phone,
                                 com.google.android.material.textfield.TextInputLayout passwordLayout, EditText password,
                                 com.google.android.material.textfield.TextInputLayout confirmLayout, EditText confirm,
                                 TextView errorView, View progress, Button submit) {
        submit.setOnClickListener(v -> {
            nameLayout.setError(null);
            emailLayout.setError(null);
            phoneLayout.setError(null);
            passwordLayout.setError(null);
            confirmLayout.setError(null);
            errorView.setVisibility(View.GONE);
            String fullName = String.valueOf(name.getText()).trim().replaceAll("\\s+", " ");
            String mail = String.valueOf(email.getText()).trim();
            String phoneNumber = String.valueOf(phone.getText()).trim();
            String pass = String.valueOf(password.getText());
            String passAgain = String.valueOf(confirm.getText());
            if (fullName.length() < 2 || fullName.length() > 80) {
                nameLayout.setError(getString(R.string.error_name));
                name.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                emailLayout.setError(getString(R.string.error_email_invalid));
                email.requestFocus();
                return;
            }
            if (!phoneNumber.matches("[0-9]{9,11}")) {
                phoneLayout.setError(getString(R.string.error_phone_invalid));
                phone.requestFocus();
                return;
            }
            if (pass.length() < 6) {
                passwordLayout.setError(getString(R.string.error_password_short));
                password.requestFocus();
                return;
            }
            if (!pass.equals(passAgain)) {
                confirmLayout.setError(getString(R.string.error_password_mismatch));
                confirm.requestFocus();
                return;
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("ho_ten", fullName);
            profile.put("email", mail);
            profile.put("so_dien_thoai", phoneNumber);
            profile.put("vai_tro", owner ? "quan_tri_gym" : "hoi_vien");
            profile.put("gym_id", "");
            profile.put("trang_thai", true);
            profile.put("ngay_tao", System.currentTimeMillis());
            progress.setVisibility(View.VISIBLE);
            submit.setEnabled(false);
            kho.dangKy(mail, pass, profile, new KhoDuLieu.KetQua<Map<String, Object>>() {
                @Override public void thanhCong(Map<String, Object> newProfile) {
                    nguoiDung = newProfile;
                    uid = KhoDuLieu.chuoi(newProfile.get("uid"));
                    if (uid.isEmpty()) uid = KhoDuLieu.chuoi(newProfile.get("id"));
                    if (owner) {
                        hienTaoThuongHieu();
                    } else {
                        tenGymHienTai = "Chưa chọn phòng Gym";
                        hienBangDieuKhien();
                    }
                }
                @Override public void thatBai(String message) {
                    progress.setVisibility(View.GONE);
                    submit.setEnabled(true);
                    errorView.setText(message);
                    errorView.setVisibility(View.VISIBLE);
                }
            });
        });
    }
    // ---------------------------------------------------------------------
    // F03 - ĐĂNG KÝ THƯƠNG HIỆU GYM / THIẾT LẬP THƯƠNG HIỆU
    // ---------------------------------------------------------------------

    private void hienTaoThuongHieu() {
        ScreenBrandSetupBinding brand = ScreenBrandSetupBinding.inflate(getLayoutInflater());
        hienManHinh(brand.getRoot(), getString(R.string.brand_setup_title));
        brand.brandLogout.setOnClickListener(v -> xuLyDangXuat());
        brand.brandCreate.setOnClickListener(v -> {
            brand.brandNameLayout.setError(null);
            brand.brandAddressLayout.setError(null);
            brand.brandPhoneLayout.setError(null);
            brand.brandError.setVisibility(View.GONE);
            String brandName = String.valueOf(brand.brandName.getText()).trim().replaceAll("\\s+", " ");
            String address = String.valueOf(brand.brandAddress.getText()).trim();
            String contact = String.valueOf(brand.brandPhone.getText()).trim();
            if (brandName.length() < 2 || brandName.length() > 80) {
                brand.brandNameLayout.setError(getString(R.string.error_name));
                brand.brandName.requestFocus();
                return;
            }
            if (address.isEmpty()) {
                brand.brandAddressLayout.setError(getString(R.string.error_required));
                brand.brandAddress.requestFocus();
                return;
            }
            if (!contact.matches("[0-9]{9,11}")) {
                brand.brandPhoneLayout.setError(getString(R.string.error_phone_invalid));
                brand.brandPhone.requestFocus();
                return;
            }

            brand.brandProgress.setVisibility(View.VISIBLE);
            brand.brandCreate.setEnabled(false);
            kho.layTatCa("phong_gym", new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                @Override public void thanhCong(List<Map<String, Object>> all) {
                    String normalized = KhoDuLieu.chuanHoaTen(brandName);
                    for (Map<String, Object> gym : all) {
                        String old = KhoDuLieu.chuoi(gym.get("ten_thuong_hieu_chuan"));
                        if (old.isEmpty()) old = KhoDuLieu.chuanHoaTen(KhoDuLieu.chuoi(gym.get("ten_thuong_hieu")));
                        if (normalized.equals(old)) {
                            hienLoiThuongHieu(brand, "Tên thương hiệu đã tồn tại. Vui lòng chọn tên khác.");
                            return;
                        }
                    }
                    Map<String, Object> gym = new HashMap<>();
                    gym.put("ten_thuong_hieu", brandName);
                    gym.put("ten_thuong_hieu_chuan", normalized);
                    gym.put("ma_gym", brandName);
                    gym.put("dia_chi", address);
                    gym.put("so_dien_thoai", contact);
                    gym.put("chu_gym_id", uid);
                    gym.put("trang_thai", "hoat_dong");
                    gym.put("ngay_tao", System.currentTimeMillis());
                    kho.them("phong_gym", gym, new KhoDuLieu.KetQua<String>() {
                        @Override public void thanhCong(String newGymId) {
                            Map<String, Object> patch = new HashMap<>();
                            patch.put("gym_id", newGymId);
                            patch.put("ten_thuong_hieu", brandName);
                            kho.capNhat("nguoi_dung", uid, patch, new KhoDuLieu.KetQua<Boolean>() {
                                @Override public void thanhCong(Boolean ok) {
                                    nguoiDung.putAll(patch);
                                    tenGymHienTai = brandName;
                                    thongBao("Đã tạo thương hiệu Gym thành công.");
                                    hienBangDieuKhien();
                                }
                                @Override public void thatBai(String error) {
                                    hienLoiThuongHieu(brand, "Đã tạo Gym nhưng không cập nhật được hồ sơ: " + error);
                                }
                            });
                        }
                        @Override public void thatBai(String error) {
                            hienLoiThuongHieu(brand, "Không tạo được thương hiệu: " + error);
                        }
                    });
                }
                @Override public void thatBai(String error) {
                    hienLoiThuongHieu(brand, "Không kiểm tra được tên thương hiệu: " + error);
                }
            });
        });
    }

    private void hienLoiThuongHieu(ScreenBrandSetupBinding brand, String message) {
        brand.brandProgress.setVisibility(View.GONE);
        brand.brandCreate.setEnabled(true);
        brand.brandError.setText(message);
        brand.brandError.setVisibility(View.VISIBLE);
    }
    // ---------------------------------------------------------------------
    // F02 - DASHBOARD CHỦ GYM / HỘI VIÊN
    // ---------------------------------------------------------------------

    private void hienBangDieuKhien() {
        taiGymSauDangNhap(() -> {
            if (laQuanTri() && TextUtils.isEmpty(gymId())) hienTaoThuongHieu();
            else hienBangDieuKhienNoReload();
        });
    }

    private void hienBangDieuKhienNoReload() {
        String brand = TextUtils.isEmpty(tenGymHienTai)
                ? KhoDuLieu.chuoi(nguoiDung.get("ten_thuong_hieu")) : tenGymHienTai;
        if (brand.isEmpty()) brand = "GymManagement";
        taoTrang(laQuanTri() ? brand : "GYM MANAGEMENT",
                laQuanTri() ? "QUẢN TRỊ GYM" : "HỘI VIÊN · " + brand, null, true);
        tuyChinhHeaderTrangChu();
        apDungNenTrangChuToi();
        if (laQuanTri()) hienTrangChuChuGym(); else hienTrangChuHoiVien();
    }

    private void apDungNenTrangChuToi() {
        View page = khungChinh.getChildAt(0);
        if (page == null) return;
        page.setBackgroundColor(0xFF050505);
        ViewCompat.setOnApplyWindowInsetsListener(page, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(page);
        if (page instanceof ViewGroup && ((ViewGroup) page).getChildCount() > 0) {
            ((ViewGroup) page).getChildAt(0).setBackgroundColor(0xFF050505);
        }
    }

    private void tuyChinhHeaderTrangChu() {
        View page = khungChinh.getChildAt(0);
        if (!(page instanceof ViewGroup) || ((ViewGroup) page).getChildCount() == 0) return;
        View header = ((ViewGroup) page).getChildAt(0);
        if (!(header instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) header;
        if (row.getChildCount() < 2) return;
        View avatar = row.getChildAt(0);
        View title = row.getChildAt(1);
        View bell = row.getChildCount() > 2 ? row.getChildAt(2) : null;
        row.removeAllViews();
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        if (bell != null) {
            LinearLayout.LayoutParams bellParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            bellParams.setMargins(dp(5), 0, 0, 0);
            row.addView(bell, bellParams);
        }
        if (laQuanTri()) {
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(42), dp(42));
            avatarParams.setMargins(dp(7), 0, 0, 0);
            row.addView(avatar, avatarParams);
        }
    }

    private void hienTrangChuHoiVien() {
        LinearLayout membershipCard = new LinearLayout(this);
        membershipCard.setOrientation(LinearLayout.HORIZONTAL);
        membershipCard.setGravity(Gravity.CENTER_VERTICAL);
        membershipCard.setPadding(dp(13), dp(12), dp(11), dp(12));
        membershipCard.setBackground(nenVien(0xFF151515, 0xFF743F43, 18));
        LinearLayout.LayoutParams membershipParams = new LinearLayout.LayoutParams(-1, -2);
        membershipParams.bottomMargin = dp(13);
        thanTrang.addView(membershipCard, membershipParams);

        TextView avatar = chu(tenNguoiDung().isEmpty() ? "?" : tenNguoiDung().substring(0, 1).toUpperCase(Locale.ROOT), 16, TRANG, true);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(0xFF292020);
        avatarBg.setStroke(dp(2), 0xFFFF5A60);
        avatar.setBackground(avatarBg);
        avatar.setGravity(Gravity.CENTER);
        membershipCard.addView(avatar, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout packageInfo = new LinearLayout(this);
        packageInfo.setOrientation(LinearLayout.VERTICAL);
        packageInfo.setPadding(dp(10), 0, dp(6), 0);
        LinearLayout.LayoutParams packageInfoParams = new LinearLayout.LayoutParams(0, -2, 1f);
        membershipCard.addView(packageInfo, packageInfoParams);
        packageInfo.addView(chu("GÓI TẬP HIỆN TẠI", 10, 0xFFAAA6AA, true));
        TextView packageName = chu("Đang tải gói tập…", 15, TRANG, true);
        packageInfo.addView(packageName);
        TextView packageDetails = chu("Thông tin hội viên", 11, 0xFFB7B3B6, false);
        packageInfo.addView(packageDetails);

        TextView viewPackages = chu("XEM GÓI  ›", 10, TRANG, true);
        viewPackages.setGravity(Gravity.CENTER);
        viewPackages.setPadding(dp(8), dp(8), dp(7), dp(8));
        viewPackages.setBackground(nenVien(0xFFB20E1A, 0xFFFF6268, 20));
        viewPackages.setOnClickListener(v -> hienDanhSachGymDangKyGoi());
        membershipCard.addView(viewPackages);
        membershipCard.setOnClickListener(v -> hienDanhSachGymDangKyGoi());

        kho.layTheoTruong("dang_ky_goi", "uid_nguoi_dung", uid,
                new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override public void thanhCong(List<Map<String, Object>> all) {
                        Map<String, Object> active = null, pending = null, latest = null;
                        long now = System.currentTimeMillis();
                        if (all != null) for (Map<String, Object> item : all) {
                            String status = KhoDuLieu.chuoi(item.get("trang_thai"));
                            long requested = laySo(item, "ngay_dang_ky");
                            if (latest == null || requested > laySo(latest, "ngay_dang_ky")) latest = item;
                            if ("dang_hoat_dong".equals(status) && laySo(item, "ngay_ket_thuc") > now
                                    && (active == null || laySo(item, "ngay_ket_thuc") > laySo(active, "ngay_ket_thuc"))) active = item;
                            if ("cho_xac_nhan".equals(status)
                                    && (pending == null || requested > laySo(pending, "ngay_dang_ky"))) pending = item;
                        }
                        Map<String, Object> selected = active != null ? active : pending != null ? pending : latest;
                        if (selected == null) {
                            packageName.setText("Chưa có gói tập");
                            packageDetails.setText("Chọn Gym và gửi yêu cầu đăng ký gói.");
                            return;
                        }
                        String status = KhoDuLieu.chuoi(selected.get("trang_thai"));
                        String label = "dang_hoat_dong".equals(status)
                                ? (laySo(selected, "ngay_ket_thuc") > now ? "Đang hoạt động" : "Hết hạn")
                                : dichTrangThai(status);
                        packageName.setText(KhoDuLieu.chuoi(selected.get("ten_goi")));
                        packageDetails.setText("Trạng thái: " + label + " · Hết hạn: " + ngay(laySo(selected, "ngay_ket_thuc")));
                        if ("dang_hoat_dong".equals(status) && laySo(selected, "ngay_ket_thuc") > now)
                            packageDetails.setTextColor(0xFF8FE3A0);
                    }
                    @Override public void thatBai(String error) {
                        packageName.setText("Chưa tải được gói tập");
                        packageDetails.setText("Kiểm tra kết nối rồi thử lại.");
                    }
                });

        LinearLayout barcodeCard = new LinearLayout(this);
        barcodeCard.setOrientation(LinearLayout.VERTICAL);
        barcodeCard.setGravity(Gravity.CENTER);
        barcodeCard.setPadding(dp(20), dp(7), dp(20), dp(6));
        barcodeCard.setBackground(nenVien(Color.WHITE, Color.WHITE, 18));
        LinearLayout.LayoutParams barcodeParams = new LinearLayout.LayoutParams(-1, dp(86));
        barcodeParams.bottomMargin = dp(14);
        thanTrang.addView(barcodeCard, barcodeParams);
        MemberBarcodeView barcode = new MemberBarcodeView(this);
        barcode.setCode(uid);
        barcodeCard.addView(barcode, new LinearLayout.LayoutParams(dp(220), dp(48)));
        String memberCode = uid.length() > 10 ? uid.substring(uid.length() - 10) : uid;
        TextView barcodeLabel = chu("MÃ HỘI VIÊN · " + memberCode, 10, 0xFF393939, false);
        barcodeLabel.setGravity(Gravity.CENTER);
        barcodeCard.addView(barcodeLabel, new LinearLayout.LayoutParams(-1, -2));
        themBannerTrangChu(this::hienDanhSachGymDangKyGoi);

        LinearLayout feature = new LinearLayout(this);
        feature.setOrientation(LinearLayout.VERTICAL);
        feature.setPadding(dp(15), dp(13), dp(15), dp(11));
        feature.setBackground(nenVien(0xFF191516, 0xFFFF5A60, 18));
        LinearLayout.LayoutParams featureParams = new LinearLayout.LayoutParams(-1, dp(116));
        featureParams.bottomMargin = dp(15);
        thanTrang.addView(feature, featureParams);
        feature.addView(chu("Hôm nay bạn tập gì?", 20, TRANG, true));
        TextView featureText = chu("Khám phá bài tập và chọn mục tiêu cho buổi tập hôm nay.", 12, 0xFFB7B3B6, false);
        feature.addView(featureText);
        TextView featureAction = chu("MỞ THƯ VIỆN BÀI TẬP  ›", 10, 0xFFFF7378, true);
        LinearLayout.LayoutParams featureActionParams = new LinearLayout.LayoutParams(-2, -2);
        featureActionParams.topMargin = dp(5);
        feature.addView(featureAction, featureActionParams);
        feature.setOnClickListener(v -> hienThuVienBaiTap());

        themChu("TRUY CẬP NHANH", 15, TRANG, true);
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(taoNutTrangChu(R.drawable.ic_gym, "Gói tập", this::hienDanhSachGymDangKyGoi));
        row1.addView(taoNutTrangChu(R.drawable.ic_calendar, "Lịch tập", this::hienLichTap));
        row1.addView(taoNutTrangChu(R.drawable.ic_assignment, "Bài tập", this::hienThuVienBaiTap));
        thanTrang.addView(row1);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(taoNutTrangChu(R.drawable.ic_ruler, "Chỉ số cơ thể", this::hienChiSoCoThe));
        row2.addView(taoNutTrangChu(R.drawable.ic_chart, "Tiến độ", this::hienTienDo));
        row2.addView(taoNutTrangChu(R.drawable.ic_notifications, "Thông báo", this::hienThongBao));
        thanTrang.addView(row2);
    }

    private void hienTrangChuChuGym() {
        LinearLayout revenueCard = new LinearLayout(this);
        revenueCard.setOrientation(LinearLayout.VERTICAL);
        revenueCard.setPadding(dp(16), dp(13), dp(16), dp(12));
        GradientDrawable revenueBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF391013, 0xFF941018, 0xFFB40816});
        revenueBg.setCornerRadius(dp(20));
        revenueBg.setStroke(dp(1), 0xFFFF676D);
        revenueCard.setBackground(revenueBg);
        LinearLayout.LayoutParams revenueParams = new LinearLayout.LayoutParams(-1, dp(128));
        revenueParams.bottomMargin = dp(8);
        thanTrang.addView(revenueCard, revenueParams);
        revenueCard.addView(chu("DOANH THU THÁNG NÀY", 13, TRANG, true));
        TextView revenueValue = chu("Đang tải…", 30, TRANG, true);
        revenueCard.addView(revenueValue);
        TextView revenueChange = chu("Đang cập nhật dữ liệu", 10, 0xFFE6E6E6, true);
        revenueChange.setPadding(dp(7), dp(4), dp(7), dp(4));
        revenueChange.setBackground(nenVien(0x55303135, 0x55303135, 7));
        revenueCard.addView(revenueChange, new LinearLayout.LayoutParams(-2, -2));
        revenueCard.setOnClickListener(v -> hienDoanhThu());
        themBannerTrangChu(this::hienGoiTap);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        TextView totalMembers = taoTheThongKe(row1, R.drawable.ic_group, "Tổng hội viên", this::hienHoiVien);
        TextView activeMembers = taoTheThongKe(row1, R.drawable.ic_person, "Hội viên đang hoạt động", this::hienHoiVien);
        thanTrang.addView(row1);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        TextView activePackages = taoTheThongKe(row2, R.drawable.ic_calendar, "Gói tập đang hoạt động", this::hienGoiTap);
        TextView attendanceToday = taoTheThongKe(row2, R.drawable.ic_event_available, "Lượt điểm danh hôm nay", this::hienDiemDanh);
        thanTrang.addView(row2);
        taiThongKeTrangChu(totalMembers, activeMembers, activePackages, attendanceToday, revenueValue, revenueChange);

        themChu("DANH MỤC QUẢN LÝ", 15, TRANG, true);
        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.addView(taoNutTrangChu(R.drawable.ic_gym, "Quản lý gói tập", this::hienGoiTap));
        row3.addView(taoNutTrangChu(R.drawable.ic_group, "Quản lý hội viên", this::hienHoiVien));
        row3.addView(taoNutTrangChu(R.drawable.ic_calendar, "Lịch tập", this::hienLichTap));
        thanTrang.addView(row3);
        LinearLayout row4 = new LinearLayout(this);
        row4.setOrientation(LinearLayout.HORIZONTAL);
        row4.addView(taoNutTrangChu(R.drawable.ic_event_available, "Điểm danh", this::hienDiemDanh));
        row4.addView(taoNutTrangChu(R.drawable.ic_notifications, "Thông báo", this::hienThongBao));
        row4.addView(taoNutTrangChu(R.drawable.ic_store, "Thông tin thương hiệu", this::hienCaiDatGym));
        thanTrang.addView(row4);
    }

    private void themBannerTrangChu(Runnable action) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(nenVien(0xFF111111, 0xFF743F43, 15));

        ImageView banner = new ImageView(this);
        banner.setImageResource(R.drawable.home_gym_banner);
        banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable clippingShape = new GradientDrawable();
        clippingShape.setColor(0xFF111111);
        clippingShape.setCornerRadius(dp(13));
        banner.setBackground(clippingShape);
        banner.setClipToOutline(true);
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(-1, -1);
        imageParams.setMargins(dp(1), dp(1), dp(1), dp(1));
        frame.addView(banner, imageParams);

        frame.setContentDescription("Banner giới thiệu dịch vụ phòng gym");
        frame.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(122));
        params.bottomMargin = dp(14);
        thanTrang.addView(frame, params);
    }

    private View taoNutTrangChu(int iconResource, String label, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(4), dp(7), dp(4), dp(7));
        tile.setBackground(nenVien(0xFF141414, 0xFF54292D, 15));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResource);
        icon.setColorFilter(0xFFFF5A60);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView title = chu(label, label.length() > 12 ? 10 : 12, TRANG, true);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(3);
        tile.addView(title, titleParams);
        tile.setContentDescription(label);
        tile.setOnClickListener(v -> action.run());
        tile.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(75).start();
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(75).start();
            return false;
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(92), 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        tile.setLayoutParams(params);
        return tile;
    }

    private TextView taoTheThongKe(LinearLayout row, int iconResource, String label, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(8), dp(8), dp(8));
        card.setBackground(nenVien(0xFF171717, 0xFFE14B53, 15));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResource);
        icon.setColorFilter(0xFFAAA6AA);
        card.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView caption = chu(label, 10, 0xFFBAB6B9, false);
        TextView value = chu("—", 23, TRANG, true);
        column.addView(caption);
        column.addView(value);
        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, -2, 1f);
        columnParams.setMargins(dp(7), 0, 0, 0);
        card.addView(column, columnParams);
        card.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(82), 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        row.addView(card, params);
        return value;
    }

    private void taiThongKeTrangChu(TextView totalMembers, TextView activeMembers,
                                    TextView activePackages, TextView attendanceToday,
                                    TextView revenueValue, TextView revenueChange) {
        long now = System.currentTimeMillis();
        kho.layTheoTruong("nguoi_dung", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> users) {
                Set<String> ids = new HashSet<>();
                for (Map<String, Object> user : users) if ("hoi_vien".equals(KhoDuLieu.chuoi(user.get("vai_tro")))) ids.add(KhoDuLieu.chuoi(user.get("id")));
                totalMembers.setText(String.valueOf(ids.size()));
            }
            @Override public void thatBai(String error) { totalMembers.setText("—"); }
        });
        kho.layTheoTruong("goi_tap", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> packages) {
                int count = 0;
                for (Map<String, Object> item : packages) if (KhoDuLieu.booleanValue(item.get("trang_thai"))) count++;
                activePackages.setText(String.valueOf(count));
            }
            @Override public void thatBai(String error) { activePackages.setText("—"); }
        });
        kho.layTheoTruong("diem_danh", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> records) {
                int count = 0;
                for (Map<String, Object> item : records) if (ngayHomNay().equals(KhoDuLieu.chuoi(item.get("ngay")))) count++;
                attendanceToday.setText(String.valueOf(count));
            }
            @Override public void thatBai(String error) { attendanceToday.setText("—"); }
        });
        kho.layTheoTruong("dang_ky_goi", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> registrations) {
                Set<String> ids = new HashSet<>();
                for (Map<String, Object> item : registrations) {
                    if ("dang_hoat_dong".equals(KhoDuLieu.chuoi(item.get("trang_thai"))) && laySo(item, "ngay_ket_thuc") > now) {
                        String memberId = KhoDuLieu.chuoi(item.get("uid_nguoi_dung"));
                        if (!memberId.isEmpty()) ids.add(memberId);
                    }
                }
                activeMembers.setText(String.valueOf(ids.size()));
                long[] revenue = doanhThuTheoThang(registrations);
                hienThiDoanhThu(revenue[0], revenue[1], revenueValue, revenueChange);
            }
            @Override public void thatBai(String error) {
                activeMembers.setText("—");
                revenueValue.setText("—");
                revenueChange.setText("Không tải được số liệu");
            }
        });
    }

    private long[] doanhThuTheoThang(List<Map<String, Object>> registrations) {
        Calendar month = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"), Locale.US);
        month.setTime(new Date());
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.set(Calendar.HOUR_OF_DAY, 0);
        month.set(Calendar.MINUTE, 0);
        month.set(Calendar.SECOND, 0);
        month.set(Calendar.MILLISECOND, 0);
        long currentMonth = month.getTimeInMillis();
        month.add(Calendar.MONTH, -1);
        long previousMonth = month.getTimeInMillis();
        month.add(Calendar.MONTH, 2);
        long nextMonth = month.getTimeInMillis();
        long currentRevenue = 0, previousRevenue = 0;
        if (registrations != null) for (Map<String, Object> item : registrations) {
            if (!"dang_hoat_dong".equals(KhoDuLieu.chuoi(item.get("trang_thai")))) continue;
            long approvedAt = laySo(item, "ngay_duyet");
            if (approvedAt <= 0) approvedAt = laySo(item, "ngay_dang_ky");
            long amount = laySo(item, "gia");
            if (approvedAt >= currentMonth && approvedAt < nextMonth) currentRevenue += amount;
            else if (approvedAt >= previousMonth && approvedAt < currentMonth) previousRevenue += amount;
        }
        return new long[]{currentRevenue, previousRevenue};
    }

    private void hienThiDoanhThu(long current, long previous, TextView value, TextView comparison) {
        value.setText(tien(current));
        comparison.setTextColor(0xFFE6E6E6);
        if (previous <= 0) {
            comparison.setText(current > 0 ? "↑ Có doanh thu trong tháng này" : "Chưa phát sinh doanh thu tháng trước");
        } else if (current == previous) {
            comparison.setText("Không đổi so với tháng trước");
        } else {
            long percent = Math.round(Math.abs(current - previous) * 100.0 / previous);
            boolean up = current > previous;
            comparison.setText((up ? "↑ " : "↓ ") + percent + "% so với tháng trước");
            comparison.setTextColor(up ? 0xFF88F0A0 : 0xFFFFB1B1);
        }
    }

    private void hienDoanhThu() {
        taoTrang("DOANH THU", tenGymHienTai, this::hienBangDieuKhien, true);
        apDungNenTrangChuToi();
        final int[] period = {0}; // 0: tháng, 1: quý, 2: năm
        final List<Map<String, Object>> cached = new ArrayList<>();

        LinearLayout periodRow = new LinearLayout(this);
        periodRow.setOrientation(LinearLayout.HORIZONTAL);
        periodRow.setPadding(0, dp(8), 0, dp(12));
        String[] periodNames = {"Tháng này", "Quý này", "Năm nay"};
        TextView[] periodButtons = new TextView[3];
        for (int i = 0; i < 3; i++) {
            final int selected = i;
            TextView tab = chu(periodNames[i], 13, TRANG, true);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(7), dp(12), dp(7), dp(12));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(44), 1);
            tp.setMargins(dp(3), 0, dp(3), 0);
            periodRow.addView(tab, tp);
            periodButtons[i] = tab;
            tab.setOnClickListener(v -> {
                period[0] = selected;
                capNhatTabDoanhThu(periodButtons, selected);
                // The same selector drives both headline totals and chart granularity.
                if (!cached.isEmpty()) capNhatNoiDungDoanhThu(cached, selected);
            });
        }
        thanTrang.addView(periodRow);
        capNhatTabDoanhThu(periodButtons, 0);

        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView currentValue = chu("Đang tải…", 27, TRANG, true);
        TextView comparison = chu("Đang cập nhật dữ liệu", 10, 0xFFFFB6B6, true);
        TextView previousValue = chu("Đang tải…", 21, TRANG, true);
        LinearLayout currentCard = taoTheDoanhThu("DOANH THU THÁNG NÀY", currentValue, comparison);
        LinearLayout previousCard = taoTheDoanhThu("DOANH THU THÁNG TRƯỚC", previousValue, null);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(142), 1);
        half.setMargins(dp(3), 0, dp(5), 0);
        summaryRow.addView(currentCard, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(142), 1);
        half2.setMargins(dp(5), 0, dp(3), 0);
        summaryRow.addView(previousCard, half2);
        thanTrang.addView(summaryRow);
        themChu("ⓘ  Doanh thu tính theo giá gói đã được duyệt trong dữ liệu đăng ký.", 12, 0xFFB9B2B5, false);

        LinearLayout chartCard = taoKhungDoanhThu();
        LinearLayout chartHeader = new LinearLayout(this);
        chartHeader.setGravity(Gravity.CENTER_VERTICAL);
        chartHeader.addView(chu("▥  Biểu đồ doanh thu", 18, TRANG, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView chartUnit = chu("VND", 11, 0xFFFF7777, true);
        chartHeader.addView(chartUnit);
        chartCard.addView(chartHeader);
        TextView chartSubtitle = chu("Doanh thu theo tháng", 12, XAM, false);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1, -2);
        subP.topMargin = dp(4);
        chartCard.addView(chartSubtitle, subP);
        View chart = taoBieuDoDoanhThu(new long[12], 0);
        LinearLayout.LayoutParams chartP = new LinearLayout.LayoutParams(-1, dp(220));
        chartP.topMargin = dp(10);
        chartCard.addView(chart, chartP);
        LinearLayout.LayoutParams chartCardP = new LinearLayout.LayoutParams(-1, -2);
        chartCardP.topMargin = dp(16);
        thanTrang.addView(chartCard, chartCardP);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsP = new LinearLayout.LayoutParams(-1, -2);
        statsP.topMargin = dp(14);
        thanTrang.addView(stats, statsP);
        TextView membersStat = chu("Hội viên\n—", 13, TRANG, true);
        TextView packagesStat = chu("Gói đã bán\n—", 13, TRANG, true);
        TextView averageStat = chu("Giá trị TB\n—", 13, TRANG, true);
        TextView[] statViews = {membersStat, packagesStat, averageStat};
        for (TextView stat : statViews) {
            stat.setGravity(Gravity.CENTER_VERTICAL);
            stat.setPadding(dp(10), dp(12), dp(8), dp(12));
            stat.setBackground(nenVien(0xFF211719, 0xFF713039, 16));
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(76), 1);
            sp.setMargins(dp(3), 0, dp(3), 0);
            stats.addView(stat, sp);
        }

        LinearLayout mixCard = taoKhungDoanhThu();
        LinearLayout.LayoutParams mixP = new LinearLayout.LayoutParams(-1, -2);
        mixP.topMargin = dp(14);
        thanTrang.addView(mixCard, mixP);
        mixCard.addView(chu("◕  Cơ cấu doanh thu", 17, TRANG, true));
        TextView mixPremium = chu("◆  Gói Premium                         0 VND\n━━━━━━━━━━━━━━━━━━━━━━━━━━  0%", 13, 0xFFFFB5B5, true);
        TextView mixStandard = chu("▰  Gói Standard                         0 VND\n━━━━━━━━━━━━━━━━━━━━━━━━━━  0%", 13, 0xFFFFB5B5, true);
        TextView mixBasic = chu("◈  Gói Basic                              0 VND\n━━━━━━━━━━━━━━━━━━━━━━━━━━  0%", 13, 0xFFFFB5B5, true);
        for (TextView item : new TextView[]{mixPremium, mixStandard, mixBasic}) {
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
            mp.topMargin = dp(12);
            mixCard.addView(item, mp);
        }
        kho.layTheoTruong("dang_ky_goi", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> registrations) {
                cached.clear();
                if (registrations != null) cached.addAll(registrations);
                capNhatNoiDungDoanhThu(cached, period[0]);
            }
            @Override public void thatBai(String error) {
                currentValue.setText("—"); previousValue.setText("—"); comparison.setText("Không tải được dữ liệu doanh thu");
            }
        });

        // Keep references on the root view so period changes can refresh every chart/card.
        thanTrang.setTag(new Object[]{currentValue, comparison, previousValue, chartCard, chartSubtitle, stats, statViews, mixPremium, mixStandard, mixBasic});
    }

    private void capNhatTabDoanhThu(TextView[] tabs, int selected) {
        for (int i = 0; i < tabs.length; i++) {
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    i == selected ? new int[]{0xFFFF343B, 0xFFB50920} : new int[]{0xFF19191C, 0xFF292326});
            bg.setCornerRadius(dp(24));
            bg.setStroke(dp(1), i == selected ? 0xFFFF555B : 0xFF393338);
            tabs[i].setBackground(bg);
            tabs[i].setTextColor(i == selected ? Color.WHITE : 0xFFBDB9BE);
            tabs[i].setElevation(i == selected ? dp(5) : 0);
        }
    }

    private LinearLayout taoKhungDoanhThu() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF351014, 0xFF191416, 0xFF211719});
        bg.setCornerRadius(dp(23));
        bg.setStroke(dp(1), 0xFF96343D);
        box.setBackground(bg);
        return box;
    }

    private void capNhatNoiDungDoanhThu(List<Map<String, Object>> rows, int mode) {
        Object tag = thanTrang.getTag();
        if (!(tag instanceof Object[])) return;
        Object[] refs = (Object[]) tag;
        TextView current = (TextView) refs[0], comparison = (TextView) refs[1], previous = (TextView) refs[2];
        LinearLayout chartCard = (LinearLayout) refs[3];
        TextView subtitle = (TextView) refs[4];
        LinearLayout stats = (LinearLayout) refs[5];
        TextView[] statViews = (TextView[]) refs[6];
        TextView premium = (TextView) refs[7], standard = (TextView) refs[8], basic = (TextView) refs[9];

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"), Locale.US);
        int year = now.get(Calendar.YEAR), month = now.get(Calendar.MONTH);
        long[] chartValues;
        String[] labels;
        long total = 0, before = 0;
        int sold = 0;
        Set<String> memberIds = new HashSet<>();
        if (mode == 0) {
            chartValues = new long[12]; labels = new String[12];
            for (int i = 0; i < 12; i++) labels[i] = "T" + (i + 1);
        } else if (mode == 1) {
            chartValues = new long[4]; labels = new String[]{"Q1", "Q2", "Q3", "Q4"};
        } else {
            chartValues = new long[5]; labels = new String[5];
            for (int i = 0; i < 5; i++) labels[i] = String.valueOf(year - 4 + i);
        }
        for (Map<String, Object> row : rows) {
            if (!"dang_hoat_dong".equals(KhoDuLieu.chuoi(row.get("trang_thai")))) continue;
            long ts = laySo(row, "ngay_duyet"); if (ts <= 0) ts = laySo(row, "ngay_dang_ky");
            if (ts <= 0) continue;
            Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"), Locale.US);
            date.setTimeInMillis(ts);
            int y = date.get(Calendar.YEAR), m = date.get(Calendar.MONTH);
            int index = mode == 0 ? (y == year ? m : -1) : mode == 1 ? (y == year ? m / 3 : -1) : (y >= year - 4 && y <= year ? y - (year - 4) : -1);
            if (index >= 0 && index < chartValues.length) chartValues[index] += laySo(row, "gia");
            boolean inCurrent = mode == 0 ? y == year && m == month : mode == 1 ? y == year && m / 3 == now.get(Calendar.MONTH) / 3 : y == year;
            Calendar prev = (Calendar) now.clone();
            if (mode == 0) prev.add(Calendar.MONTH, -1); else if (mode == 1) prev.add(Calendar.MONTH, -3); else prev.add(Calendar.YEAR, -1);
            boolean inPrevious = mode == 0 ? y == prev.get(Calendar.YEAR) && m == prev.get(Calendar.MONTH)
                    : mode == 1 ? y == prev.get(Calendar.YEAR) && m / 3 == prev.get(Calendar.MONTH) / 3 : y == prev.get(Calendar.YEAR);
            if (inCurrent) { total += laySo(row, "gia"); sold++; String member = KhoDuLieu.chuoi(row.get("uid_nguoi_dung")); if (!member.isEmpty()) memberIds.add(member); }
            if (inPrevious) before += laySo(row, "gia");
        }
        current.setText(tien(total)); previous.setText(tien(before));
        String unit = mode == 0 ? "tháng trước" : mode == 1 ? "quý trước" : "năm trước";
        if (before <= 0) comparison.setText(total > 0 ? "↑ Có doanh thu trong kỳ" : "Chưa phát sinh doanh thu kỳ trước");
        else { long pct = Math.round(Math.abs(total - before) * 100.0 / before); comparison.setText((total >= before ? "↑ " : "↓ ") + pct + "% so với " + unit); }
        ((TextView) ((LinearLayout) chartCard.getChildAt(0)).getChildAt(0)).setText("▥  Biểu đồ doanh thu");
        subtitle.setText(mode == 0 ? "Doanh thu theo tháng · VND" : mode == 1 ? "Doanh thu theo quý · VND" : "Doanh thu theo năm · VND");
        View chart = chartCard.getChildAt(2);
        chartCard.removeView(chart);
        chartCard.addView(taoBieuDoDoanhThu(chartValues, mode), new LinearLayout.LayoutParams(-1, dp(220)));
        statViews[0].setText("👥 Hội viên\n" + memberIds.size());
        statViews[1].setText("▤ Gói đã bán\n" + sold);
        statViews[2].setText("◉ Giá trị TB\n" + (sold == 0 ? "0 VND" : tien(total / sold)));
        premium.setText("◆  Gói Premium                         " + tien(0) + "\n━━━━━━━━━━━━━━━━━━━━━━━━━━  0%");
        standard.setText("▰  Gói Standard                         " + tien(0) + "\n━━━━━━━━━━━━━━━━━━━━━━━━━━  0%");
        basic.setText("◈  Gói Basic                              " + tien(0) + "\n━━━━━━━━━━━━━━━━━━━━━━━━━━  0%");
    }

    private View taoBieuDoDoanhThu(long[] values, int mode) {
        return new View(this) {
            final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float left = dp(38), right = getWidth() - dp(8), top = dp(18), bottom = getHeight() - dp(30);
                long max = 1; for (long v : values) max = Math.max(max, v);
                max = Math.max(max, ((max + 4) / 5) * 5);
                p.setColor(0x553F3A3D); p.setStrokeWidth(dp(1)); p.setStyle(Paint.Style.STROKE);
                for (int i = 0; i <= 4; i++) { float y = bottom - (bottom - top) * i / 4f; canvas.drawLine(left, y, right, y, p); }
                float slot = (right - left) / Math.max(1, values.length);
                for (int i = 0; i < values.length; i++) {
                    float h = (bottom - top) * values[i] / (float) max;
                    float x = left + i * slot + slot * .18f;
                    RectF bar = new RectF(x, bottom - h, x + slot * .64f, bottom);
                    p.setStyle(Paint.Style.FILL);
                    p.setShader(new android.graphics.LinearGradient(0, bar.top, 0, bottom, 0xFFFF555B, 0xFF8D101D, android.graphics.Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(bar, dp(5), dp(5), p); p.setShader(null);
                    p.setColor(0xFFCAC4C8); p.setTextSize(dp(values.length > 6 ? 9 : 11)); p.setTextAlign(Paint.Align.CENTER);
                    String label = mode == 0 ? "T" + (i + 1) : mode == 1 ? "Q" + (i + 1) : String.valueOf(Calendar.getInstance().get(Calendar.YEAR) - 4 + i);
                    canvas.drawText(label, x + slot * .32f, getHeight() - dp(8), p);
                }
            }
        };
    }

    private LinearLayout taoTheDoanhThu(String title, TextView value, @Nullable TextView detail) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(13), dp(16), dp(13));
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF1A1718, 0xFF391115, 0xFF721018});
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), 0xFF9C4146);
        card.setBackground(background);
        card.addView(chu(title, 13, TRANG, true));
        card.addView(value);
        if (detail != null) {
            detail.setPadding(dp(7), dp(4), dp(7), dp(4));
            detail.setBackground(nenVien(0x55303135, 0x55303135, 7));
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-2, -2);
            detailParams.topMargin = dp(4);
            card.addView(detail, detailParams);
        }
        return card;
    }

    private void hienBangDieuKhienNoReloadLegacy() {
        String brand = TextUtils.isEmpty(tenGymHienTai) ? KhoDuLieu.chuoi(nguoiDung.get("ten_thuong_hieu")) : tenGymHienTai;
        if (brand.isEmpty()) brand = "GymManagement";
        taoTrang("Xin chào, " + tenNguoiDung(), laQuanTri() ? "Bảng điều khiển quản trị · " + brand : "Hội viên · " + brand, null, true);

        // 1. TOP RUNNING NEWS BANNER (DAILY HEALTH NEWS)
        hienBannerHealthNews();

        if (laQuanTri()) {
            // OWNER / ADMIN DASHBOARD
            themChu("DANH MỤC QUẢN LÝ THƯƠNG HIỆU", 14, 0xFFE53935, true);

            LinearLayout r1 = new LinearLayout(this);
            r1.setOrientation(LinearLayout.HORIZONTAL);
            r1.addView(taoNutLuoiPro("💳", "Quản lý gói", "Thiết lập gói Gym", 0xFFFFCDD2, 0xFFB71C1C, this::hienGoiTap));
            r1.addView(taoNutLuoiPro("👥", "Hội viên", "Danh sách hội viên", 0xFFE8F5E9, 0xFF1B5E20, this::hienHoiVien));
            thanTrang.addView(r1);

            LinearLayout r2 = new LinearLayout(this);
            r2.setOrientation(LinearLayout.HORIZONTAL);
            r2.addView(taoNutLuoiPro("📩", "Yêu cầu gói", "Duyệt đơn đăng ký", 0xFFF8BBD0, 0xFF880E4F, this::hienYeuCauGoi));
            r2.addView(taoNutLuoiPro("📊", "Thể trạng", "Chỉ số BMI hội viên", 0xFFFFF9C4, 0xFFF57F17, this::hienQuanLyTheTrangHoiVien));
            thanTrang.addView(r2);

            LinearLayout r3 = new LinearLayout(this);
            r3.setOrientation(LinearLayout.HORIZONTAL);
            r3.addView(taoNutLuoiPro("📅", "Lịch tập & ca", "Phân ca theo giờ", 0xFFE1F5FE, 0xFF0277BD, this::hienLichTap));
            r3.addView(taoNutLuoiPro("🏋️", "Bài tập", "Thư viện hướng dẫn", 0xFFFFF59D, 0xFFF57F17, this::hienThuVienBaiTap));
            thanTrang.addView(r3);

            LinearLayout r4 = new LinearLayout(this);
            r4.setOrientation(LinearLayout.HORIZONTAL);
            r4.addView(taoNutLuoiPro("✉️", "Thông báo", "Gửi tin Gmail Style", 0xFFF3E5F5, 0xFF4A148C, this::hienThongBao));
            r4.addView(taoNutLuoiPro("✅", "Điểm danh", "Lượt tập hôm nay", 0xFFFFE0B2, 0xFFE65100, this::hienDiemDanh));
            thanTrang.addView(r4);

            LinearLayout r5 = new LinearLayout(this);
            r5.setOrientation(LinearLayout.HORIZONTAL);
            r5.addView(taoNutLuoiPro("🏛️", "Thương hiệu", "Thông tin phòng Gym", 0xFFE0F2F1, 0xFF004D40, this::hienCaiDatGym));
            r5.addView(taoNutLuoiPro("👤", "Hồ sơ cá nhân", "Cài đặt tài khoản", 0xFFD1C4E9, 0xFF311B92, this::hienHoSo));
            thanTrang.addView(r5);

        } else {
            // MEMBER DASHBOARD
            themChu("THỂ HỘI VIÊN CHÍNH THỨC", 14, 0xFFE53935, true);

            LinearLayout membershipCard = new LinearLayout(this);
            membershipCard.setOrientation(LinearLayout.VERTICAL);
            membershipCard.setPadding(dp(18), dp(18), dp(18), dp(18));
            membershipCard.setMinimumHeight(dp(160));

            GradientDrawable iriBg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFFE8EAF6, 0xFFF3E5F5, 0xFFE0F7FA, 0xFFFFF3E0}
            );
            iriBg.setCornerRadius(dp(20));
            iriBg.setStroke(dp(2), 0xFFD1C4E9);
            membershipCard.setBackground(iriBg);

            thanTrang.addView(membershipCard);

            kho.layTheoTruong("dang_ky_goi", "uid_nguoi_dung", uid, new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                @Override public void thanhCong(List<Map<String, Object>> all) {
                    Map<String, Object> active = null;
                    if (all != null) {
                        for (Map<String, Object> item : all) {
                            String status = KhoDuLieu.chuoi(item.get("trang_thai"));
                            if ("dang_hoat_dong".equals(status) || "cho_xac_nhan".equals(status)) {
                                if (active == null || laySo(item, "ngay_dang_ky") > laySo(active, "ngay_dang_ky")) active = item;
                            }
                        }
                    }
                    membershipCard.removeAllViews();

                    LinearLayout cardHeader = new LinearLayout(MainActivity.this);
                    cardHeader.setOrientation(LinearLayout.HORIZONTAL);
                    cardHeader.setGravity(Gravity.CENTER_VERTICAL);

                    TextView tvPassTitle = chu("🏆 GYMPRO VIP PASS", 13, 0xFF5E35B1, true);
                    cardHeader.addView(tvPassTitle, new LinearLayout.LayoutParams(0, -2, 1));

                    TextView tvQr = chu("[ BARCODE ]", 11, 0xFF37474F, true);
                    tvQr.setPadding(dp(8), dp(4), dp(8), dp(4));
                    tvQr.setBackground(nenVien(0xFFE0E0E0, 0xFFBDBDBD, 8));
                    cardHeader.addView(tvQr);

                    membershipCard.addView(cardHeader);

                    if (active == null) {
                        TextView t = chu("\nChưa đăng ký gói tập. Bấm Đăng ký gói bên dưới để kích hoạt Thẻ VIP.", 13, 0xFF37474F, false);
                        membershipCard.addView(t);
                    } else {
                        TextView tName = chu("\n💳 Gói: " + KhoDuLieu.chuoi(active.get("ten_goi")), 22, 0xFF1A1C1E, true);
                        TextView tMember = chu("Hội viên: " + tenNguoiDung(), 13, 0xFF424242, true);
                        TextView tStatus = chu("Trạng thái: " + dichTrangThai(KhoDuLieu.chuoi(active.get("trang_thai"))), 13, 0xFF2E7D32, true);
                        TextView tExpiry = chu("Hạn sử dụng: " + ngay(laySo(active, "ngay_ket_thuc")), 13, 0xFF455A64, false);

                        membershipCard.addView(tName);
                        membershipCard.addView(tMember);
                        membershipCard.addView(tStatus);
                        membershipCard.addView(tExpiry);
                    }
                }
                @Override public void thatBai(String error) {
                    membershipCard.removeAllViews();
                    membershipCard.addView(chu("Chưa tải được thông tin gói tập.", 13, 0xFF37474F, false));
                }
            });

            themChu("DANH MỤC TRUY CẬP NHANH", 14, 0xFFE53935, true);

            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.addView(taoNutLuoiPro("💳", "Đăng ký gói", "Xem các gói Gym", 0xFFFFCDD2, 0xFFB71C1C, this::hienDanhSachGymDangKyGoi));
            row1.addView(taoNutLuoiPro("📅", "Lịch tập", "Xem phân ca hôm nay", 0xFFE1F5FE, 0xFF0277BD, this::hienLichTap));
            thanTrang.addView(row1);

            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.addView(taoNutLuoiPro("🏋️", "Bài tập", "Thư viện Fitness", 0xFFFFF9C4, 0xFFF57F17, this::hienThuVienBaiTap));
            row2.addView(taoNutLuoiPro("📈", "Tiến độ", "Nhật ký tập luyện", 0xFFE8F5E9, 0xFF1B5E20, this::hienTienDo));
            thanTrang.addView(row2);

            LinearLayout row3 = new LinearLayout(this);
            row3.setOrientation(LinearLayout.HORIZONTAL);
            row3.addView(taoNutLuoiPro("📊", "Chỉ số BMI", "Cập nhật chiều cao, mỡ", 0xFFE0F2F1, 0xFF004D40, this::hienChiSoCoThe));
            row3.addView(taoNutLuoiPro("🔔", "Thông báo", "Hộp thư thông báo", 0xFFF3E5F5, 0xFF4A148C, this::hienThongBao));
            thanTrang.addView(row3);

            LinearLayout row4 = new LinearLayout(this);
            row4.setOrientation(LinearLayout.HORIZONTAL);
            row4.addView(taoNutLuoiPro("👤", "Hồ sơ cá nhân", "Cài đặt tài khoản & Ảnh đại diện", 0xFFFFE0B2, 0xFFE65100, this::hienHoSo));
            thanTrang.addView(row4);
        }
    }

    private TextView chiSo(String nhan, String giaTri) {
        TextView tv = chu(nhan + "\n" + giaTri, 11, TRANG, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(5), dp(14), dp(5), dp(14));
        tv.setBackground(nenVien(DEN_THE, Color.rgb(55, 55, 55), 12));
        return tv;
    }

    private String dichTrangThai(String status) {
        switch (status) {
            case "cho_xac_nhan": return "Chờ xác nhận";
            case "dang_hoat_dong": return "Đang hoạt động";
            case "tu_choi": return "Đã từ chối";
            case "het_han": return "Hết hạn";
            case "hoat_dong": return "Hoạt động";
            case "ngung_hoat_dong": return "Ngừng hoạt động";
            default: return status;
        }
    }

    private void xuLyDangXuat() {
        new AlertDialog.Builder(this).setTitle("Đăng xuất")
                .setMessage("Bạn có muốn đăng xuất khỏi GymManagement không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    kho.dangXuat();
                    uid = "";
                    nguoiDung.clear();
                    hienDangNhap();
                }).show();
    }

    // ---------------------------------------------------------------------
    // F03 - GÓI TẬP: CHỦ GYM CRUD, HỘI VIÊN ĐĂNG KÝ
    // ---------------------------------------------------------------------

    /**
     * Danh sách các phòng Gym đang hoạt động cho hội viên chưa chọn Gym.
     * Có tìm kiếm theo tên thương hiệu và lọc theo địa chỉ.
     */
    private void hienDanhSachGymDangKyGoi() {
        taoTrang("CHỌN PHÒNG GYM", "Tìm theo tên thương hiệu và lọc theo địa chỉ",
                this::hienBangDieuKhien, true);

        // Banner giới thiệu giúp màn hình chọn Gym đồng bộ với giao diện đỏ/đen.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF591B22, 0xFF241417, 0xFF171315});
        heroBg.setCornerRadius(dp(20));
        heroBg.setStroke(dp(1), 0xFF87363D);
        hero.setBackground(heroBg);
        TextView eyebrow = chu("GYMMANAGEMENT  /  MEMBERSHIP", 10, 0xFFFFA0A0, true);
        hero.addView(eyebrow);
        TextView heroTitle = chu("TÌM PHÒNG GYM\nPHÙ HỢP VỚI BẠN", 22, TRANG, true);
        heroTitle.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams heroTitleP = new LinearLayout.LayoutParams(-1, -2);
        heroTitleP.topMargin = dp(8);
        hero.addView(heroTitle, heroTitleP);
        TextView heroSub = chu("Khám phá thương hiệu, xem địa chỉ và lựa chọn gói tập phù hợp với mục tiêu của bạn.", 12, 0xFFD0C2C3, false);
        LinearLayout.LayoutParams heroSubP = new LinearLayout.LayoutParams(-1, -2);
        heroSubP.topMargin = dp(7);
        hero.addView(heroSub, heroSubP);
        LinearLayout.LayoutParams heroP = new LinearLayout.LayoutParams(-1, -2);
        heroP.bottomMargin = dp(18);
        thanTrang.addView(hero, heroP);

        LinearLayout searchPanel = new LinearLayout(this);
        searchPanel.setOrientation(LinearLayout.VERTICAL);
        searchPanel.setPadding(dp(14), dp(14), dp(14), dp(12));
        searchPanel.setBackground(nenVien(0xFF1D1D1F, 0xFF383438, 17));
        thanTrang.addView(searchPanel);
        TextView searchTitle = chu("TÌM KIẾM PHÒNG TẬP", 14, TRANG, true);
        searchPanel.addView(searchTitle);
        TextView nameLabel = chu("Tên thương hiệu", 12, XAM, true);
        LinearLayout.LayoutParams nameLabelP = new LinearLayout.LayoutParams(-1, -2);
        nameLabelP.topMargin = dp(12);
        searchPanel.addView(nameLabel, nameLabelP);
        EditText tuKhoa = taoNhapTrong("⌕  Nhập tên phòng Gym...", InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams inputP = new LinearLayout.LayoutParams(-1, -2);
        inputP.topMargin = dp(6);
        searchPanel.addView(tuKhoa, inputP);
        TextView addressLabel = chu("Khu vực / địa chỉ", 12, XAM, true);
        LinearLayout.LayoutParams addressLabelP = new LinearLayout.LayoutParams(-1, -2);
        addressLabelP.topMargin = dp(10);
        searchPanel.addView(addressLabel, addressLabelP);
        EditText diaChiLoc = taoNhapTrong("⌖  Ví dụ: Thủ Dầu Một, Bình Dương", InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams addressInputP = new LinearLayout.LayoutParams(-1, -2);
        addressInputP.topMargin = dp(6);
        searchPanel.addView(diaChiLoc, addressInputP);
        Button nutTim = taoNutNho("TÌM KIẾM PHÒNG GYM  →", true, null);
        nutTim.setTextSize(13);
        nutTim.setMinHeight(dp(48));
        LinearLayout.LayoutParams searchButtonP = new LinearLayout.LayoutParams(-1, -2);
        searchButtonP.topMargin = dp(12);
        searchPanel.addView(nutTim, searchButtonP);

        LinearLayout resultsHeading = new LinearLayout(this);
        resultsHeading.setOrientation(LinearLayout.HORIZONTAL);
        resultsHeading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams resultsHeadingP = new LinearLayout.LayoutParams(-1, -2);
        resultsHeadingP.topMargin = dp(20);
        resultsHeadingP.bottomMargin = dp(10);
        thanTrang.addView(resultsHeading, resultsHeadingP);
        TextView resultsTitle = chu("PHÒNG GYM GỢI Ý", 16, TRANG, true);
        resultsHeading.addView(resultsTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView resultsBadge = chu("ĐANG TẢI", 10, 0xFFFFA0A0, true);
        resultsBadge.setPadding(dp(9), dp(6), dp(9), dp(6));
        resultsBadge.setBackground(nenVien(0xFF35191D, 0xFF713238, 20));
        resultsHeading.addView(resultsBadge);

        LinearLayout ketQua = new LinearLayout(this);
        ketQua.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(ketQua);
        ketQua.addView(chu("Đang tải danh sách phòng Gym...", 13, XAM, false));

        List<Map<String, Object>> dsGym = new ArrayList<>();
        final boolean[] daTaiXong = {false};
        nutTim.setOnClickListener(v -> {
            if (!daTaiXong[0]) {
                thongBao("Đang tải danh sách phòng Gym, vui lòng thử lại sau.");
                return;
            }
            hienThiDanhSachGymDangKyGoi(dsGym,
                    tuKhoa.getText().toString(), diaChiLoc.getText().toString(), ketQua, resultsBadge);
        });

        android.text.TextWatcher liveFilter = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (daTaiXong[0]) hienThiDanhSachGymDangKyGoi(dsGym,
                        tuKhoa.getText().toString(), diaChiLoc.getText().toString(), ketQua, resultsBadge);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        tuKhoa.addTextChangedListener(liveFilter);
        diaChiLoc.addTextChangedListener(liveFilter);

        kho.layTatCa("phong_gym", new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override
            public void thanhCong(List<Map<String, Object>> data) {
                dsGym.clear();
                for (Map<String, Object> gym : data) {
                    if ("hoat_dong".equals(KhoDuLieu.chuoi(gym.get("trang_thai")))) {
                        dsGym.add(gym);
                    }
                }
                daTaiXong[0] = true;
                hienThiDanhSachGymDangKyGoi(dsGym,
                        tuKhoa.getText().toString(), diaChiLoc.getText().toString(), ketQua, resultsBadge);
            }

            @Override
            public void thatBai(String error) {
                daTaiXong[0] = true;
                ketQua.removeAllViews();
                ketQua.addView(chu("Không tải được danh sách phòng Gym: " + error, 13, TRANG, false));
                themNutVaoThe(theTrong(ketQua, "Thử lại", "Tải lại danh sách phòng Gym."),
                        "TẢI LẠI", true, MainActivity.this::hienDanhSachGymDangKyGoi);
            }
        });
    }

    /** Lọc danh sách đã tải; mỗi phòng mở riêng danh sách gói của phòng đó. */
    private EditText taoNhapTrong(String hint, int inputType) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(13);
        edit.setTextColor(TRANG);
        edit.setHintTextColor(0xFF858085);
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setPadding(dp(13), dp(10), dp(13), dp(10));
        edit.setBackground(nenVien(0xFF29282B, 0xFF484348, 12));
        edit.setMinHeight(dp(47));
        return edit;
    }

    private void hienThiDanhSachGymDangKyGoi(
            List<Map<String, Object>> dsGym,
            String tuKhoa,
            String diaChiLoc,
            LinearLayout ketQua,
            TextView resultsBadge) {

        ketQua.removeAllViews();
        String tuKhoaChuan = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT);
        String diaChiChuan = diaChiLoc == null ? "" : diaChiLoc.trim().toLowerCase(Locale.ROOT);
        int soKetQua = 0;

        for (Map<String, Object> gym : dsGym) {
            String ten = KhoDuLieu.chuoi(gym.get("ten_thuong_hieu"));
            String diaChi = KhoDuLieu.chuoi(gym.get("dia_chi"));
            String dienThoai = KhoDuLieu.chuoi(gym.get("so_dien_thoai"));

            if (!ten.toLowerCase(Locale.ROOT).contains(tuKhoaChuan)) continue;
            if (!diaChi.toLowerCase(Locale.ROOT).contains(diaChiChuan)) continue;

            LinearLayout theGym = new LinearLayout(this);
            theGym.setOrientation(LinearLayout.VERTICAL);
            theGym.setPadding(dp(15), dp(14), dp(15), dp(13));
            theGym.setBackground(nenVien(0xFF202022, 0xFF414044, 17));
            LinearLayout.LayoutParams gymCardP = new LinearLayout.LayoutParams(-1, -2);
            gymCardP.bottomMargin = dp(11);
            ketQua.addView(theGym, gymCardP);

            LinearLayout gymTop = new LinearLayout(this);
            gymTop.setOrientation(LinearLayout.HORIZONTAL);
            gymTop.setGravity(Gravity.CENTER_VERTICAL);
            TextView gymIcon = chu(ten.isEmpty() ? "G" : ten.substring(0, 1).toUpperCase(Locale.ROOT), 20, TRANG, true);
            gymIcon.setGravity(Gravity.CENTER);
            gymIcon.setBackground(nenVien(0xFF63252D, 0xFF9D4149, 13));
            LinearLayout.LayoutParams gymIconP = new LinearLayout.LayoutParams(dp(46), dp(46));
            gymIconP.setMargins(0, 0, dp(11), 0);
            gymTop.addView(gymIcon, gymIconP);
            LinearLayout gymTitleCol = new LinearLayout(this);
            gymTitleCol.setOrientation(LinearLayout.VERTICAL);
            TextView gymName = chu(ten, 15, TRANG, true);
            gymTitleCol.addView(gymName);
            TextView gymStatus = chu("●  ĐANG HOẠT ĐỘNG", 10, 0xFF8FE3A0, true);
            LinearLayout.LayoutParams gymStatusP = new LinearLayout.LayoutParams(-1, -2);
            gymStatusP.topMargin = dp(4);
            gymTitleCol.addView(gymStatus, gymStatusP);
            gymTop.addView(gymTitleCol, new LinearLayout.LayoutParams(0, -2, 1));
            theGym.addView(gymTop);

            View separator = new View(this);
            separator.setBackgroundColor(0xFF3E373A);
            LinearLayout.LayoutParams separatorP = new LinearLayout.LayoutParams(-1, dp(1));
            separatorP.topMargin = dp(12);
            separatorP.bottomMargin = dp(10);
            theGym.addView(separator, separatorP);

            TextView addressText = chu("⌖  " + (diaChi.isEmpty() ? "Chưa cập nhật địa chỉ" : diaChi), 12, 0xFFD0C9CC, false);
            addressText.setLineSpacing(dp(2), 1f);
            theGym.addView(addressText);
            TextView phoneText = chu("☎  " + (dienThoai.isEmpty() ? "Chưa cập nhật số điện thoại" : dienThoai), 12, XAM, false);
            LinearLayout.LayoutParams phoneP = new LinearLayout.LayoutParams(-1, -2);
            phoneP.topMargin = dp(5);
            theGym.addView(phoneText, phoneP);
            Button viewPackages = taoNutNho("KHÁM PHÁ GÓI TẬP  →", true,
                    () -> hienGoiTapCuaGym(gym));
            viewPackages.setTextSize(12);
            viewPackages.setMinHeight(dp(44));
            LinearLayout.LayoutParams viewPackagesP = new LinearLayout.LayoutParams(-1, -2);
            viewPackagesP.topMargin = dp(12);
            theGym.addView(viewPackages, viewPackagesP);
            soKetQua++;
        }

        resultsBadge.setText(soKetQua + " PHÒNG GYM");
        if (soKetQua == 0) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(25), dp(20), dp(25));
            empty.setBackground(nenVien(DEN_THE, 0xFF393438, 16));
            empty.addView(chu("⌕", 30, 0xFFFF666B, true));
            TextView emptyText = chu("Chưa tìm thấy phòng Gym", 15, TRANG, true);
            emptyText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams emptyTextP = new LinearLayout.LayoutParams(-1, -2);
            emptyTextP.topMargin = dp(8);
            empty.addView(emptyText, emptyTextP);
            TextView emptyHint = chu("Thử thay đổi tên thương hiệu hoặc khu vực tìm kiếm nhé.", 12, XAM, false);
            emptyHint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams emptyHintP = new LinearLayout.LayoutParams(-1, -2);
            emptyHintP.topMargin = dp(5);
            empty.addView(emptyHint, emptyHintP);
            ketQua.addView(empty);
        } else {
            TextView resultFoot = chu("Tìm thấy " + soKetQua + " phòng Gym phù hợp với tìm kiếm của bạn.", 11, XAM, false);
            LinearLayout.LayoutParams resultFootP = new LinearLayout.LayoutParams(-1, -2);
            resultFootP.topMargin = dp(2);
            ketQua.addView(resultFoot, resultFootP);
        }
    }

    /** Hiển thị các gói tập đang hoạt động của đúng Gym hội viên đã chọn. */
    private void hienGoiTapCuaGym(Map<String, Object> gym) {
        String selectedGymId = KhoDuLieu.chuoi(gym.get("id"));
        String tenGym = KhoDuLieu.chuoi(gym.get("ten_thuong_hieu"));
        String diaChi = KhoDuLieu.chuoi(gym.get("dia_chi"));
        String dienThoai = KhoDuLieu.chuoi(gym.get("so_dien_thoai"));

        if (selectedGymId.isEmpty()) {
            baoLoi("Không xác định được ID phòng Gym. Kiểm tra dữ liệu trong phong_gym.");
            return;
        }

        taoTrang("ĐĂNG KÝ GÓI TẬP", tenGym, this::hienDanhSachGymDangKyGoi, true);
        LinearLayout gymHero = new LinearLayout(this);
        gymHero.setOrientation(LinearLayout.VERTICAL);
        gymHero.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF3A1116, 0xFF171315, 0xFF351316});
        heroBg.setCornerRadius(dp(20)); heroBg.setStroke(dp(1), 0xFF74343B); gymHero.setBackground(heroBg);
        gymHero.addView(chu("CHỌN GÓI TẬP PHÙ HỢP", 19, TRANG, true));
        TextView gymTitle = chu(tenGym, 15, 0xFFFF7777, true);
        LinearLayout.LayoutParams gymTitleP = new LinearLayout.LayoutParams(-1, -2); gymTitleP.topMargin = dp(5); gymHero.addView(gymTitle, gymTitleP);
        TextView gymInfo = chu("⌖  " + (diaChi.isEmpty() ? "Chưa cập nhật địa chỉ" : diaChi)
                + (dienThoai.isEmpty() ? "" : "\n☎  " + dienThoai), 12, XAM, false);
        LinearLayout.LayoutParams gymInfoP = new LinearLayout.LayoutParams(-1, -2); gymInfoP.topMargin = dp(8); gymHero.addView(gymInfo, gymInfoP);
        LinearLayout.LayoutParams heroP = new LinearLayout.LayoutParams(-1, -2); heroP.bottomMargin = dp(14); thanTrang.addView(gymHero, heroP);
        LinearLayout perks = new LinearLayout(this); perks.setOrientation(LinearLayout.HORIZONTAL);
        String[] perkNames = {"Tập luyện\nkhông giới hạn", "Hỗ trợ\nPT cá nhân", "Theo dõi\ntiến độ"};
        String[] perkIcons = {"✣", "♙", "▥"};
        for (int i = 0; i < perkNames.length; i++) {
            LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(5), dp(12), dp(5), dp(12)); tile.setBackground(nenVien(DEN_THE, 0xFF333333, 14));
            TextView ico = chu(perkIcons[i], 21, 0xFFFF5A60, true); ico.setGravity(Gravity.CENTER); tile.addView(ico);
            TextView label = chu(perkNames[i], 11, TRANG, true); label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelP = new LinearLayout.LayoutParams(-1, -2); labelP.topMargin = dp(5); tile.addView(label, labelP);
            LinearLayout.LayoutParams tileP = new LinearLayout.LayoutParams(0, -2, 1); tileP.setMargins(0, 0, dp(7), 0); perks.addView(tile, tileP);
        }
        LinearLayout.LayoutParams perksP = new LinearLayout.LayoutParams(-1, -2); perksP.bottomMargin = dp(18); thanTrang.addView(perks, perksP);
        themChu("DANH SÁCH GÓI TẬP", 19, TRANG, true);
        themChu("Chọn một gói để xem tổng thanh toán và gửi yêu cầu đăng ký.", 12, XAM, false);

        ProgressBar loading = new ProgressBar(this);
        loading.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(DO));
        thanTrang.addView(loading, new LinearLayout.LayoutParams(dp(36), dp(36)));

        kho.layTheoTruong("goi_tap", "gym_id", selectedGymId,
                new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override
                    public void thanhCong(List<Map<String, Object>> data) {
                        loading.setVisibility(View.GONE);
                        List<Map<String, Object>> goiHoatDong = new ArrayList<>();
                        for (Map<String, Object> pack : data) {
                            if (KhoDuLieu.booleanValue(pack.get("trang_thai"))) {
                                goiHoatDong.add(pack);
                            }
                        }
                        Collections.sort(goiHoatDong,
                                (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));

                        if (goiHoatDong.isEmpty()) {
                            the("Chưa có gói tập hoạt động",
                                    "Phòng Gym này hiện chưa cung cấp gói tập để đăng ký.");
                            return;
                        }

                        final Map<String, Object>[] selected = new Map[]{goiHoatDong.get(0)};
                        LinearLayout packageList = new LinearLayout(MainActivity.this);
                        packageList.setOrientation(LinearLayout.VERTICAL);
                        thanTrang.addView(packageList);
                        LinearLayout footer = new LinearLayout(MainActivity.this);
                        footer.setOrientation(LinearLayout.HORIZONTAL); footer.setGravity(Gravity.CENTER_VERTICAL);
                        footer.setPadding(dp(14), dp(10), dp(14), dp(10));
                        footer.setBackground(nenVien(0xFF211719, 0xFF713238, 16));
                        LinearLayout.LayoutParams footerP = new LinearLayout.LayoutParams(-1, -2);
                        footerP.topMargin = dp(8); thanTrang.addView(footer, footerP);
                        TextView total = chu("TỔNG THANH TOÁN\n" + tien(laySo(selected[0], "gia")), 14, 0xFFFF7777, true);
                        footer.addView(total, new LinearLayout.LayoutParams(0, -2, 1));
                        Button register = taoNutNho("ĐĂNG KÝ NGAY  →", true, () -> dangKyGoi(selected[0]));
                        register.setMinHeight(dp(48)); footer.addView(register, new LinearLayout.LayoutParams(dp(150), -2));
                        for (Map<String, Object> pack : goiHoatDong) {
                            LinearLayout card = new LinearLayout(MainActivity.this); card.setOrientation(LinearLayout.VERTICAL);
                            card.setPadding(dp(15), dp(14), dp(15), dp(12));
                            card.setBackground(nenVien(pack == selected[0] ? 0xFF2A1719 : DEN_THE,
                                    pack == selected[0] ? DO : 0xFF303030, 17));
                            LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(-1, -2);
                            cardP.bottomMargin = dp(11); packageList.addView(card, cardP);
                            LinearLayout row = new LinearLayout(MainActivity.this); row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setGravity(Gravity.CENTER_VERTICAL);
                            LinearLayout detail = new LinearLayout(MainActivity.this); detail.setOrientation(LinearLayout.VERTICAL);
                            detail.addView(chu(KhoDuLieu.chuoi(pack.get("ten_goi")), 17, TRANG, true));
                            TextView price = chu(tien(laySo(pack, "gia")), 18, DO, true);
                            LinearLayout.LayoutParams priceP = new LinearLayout.LayoutParams(-1, -2); priceP.topMargin = dp(3); detail.addView(price, priceP);
                            detail.addView(chu(laySo(pack, "so_ngay") + " ngày", 12, XAM, true));
                            row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
                            TextView check = chu(pack == selected[0] ? "✓" : "○", 23, pack == selected[0] ? DO : XAM, true);
                            check.setGravity(Gravity.CENTER); row.addView(check, new LinearLayout.LayoutParams(dp(36), dp(36)));
                            card.addView(row);
                            String description = KhoDuLieu.chuoi(pack.get("mo_ta"));
                            if (!description.isEmpty()) {
                                TextView desc = chu("✓  " + description, 12, XAM, false);
                                LinearLayout.LayoutParams descP = new LinearLayout.LayoutParams(-1, -2);
                                descP.topMargin = dp(8); card.addView(desc, descP);
                            }
                            card.setOnClickListener(v -> {
                                selected[0] = pack;
                                total.setText("TỔNG THANH TOÁN\n" + tien(laySo(pack, "gia")));
                                for (int i = 0; i < packageList.getChildCount(); i++) {
                                    View child = packageList.getChildAt(i);
                                    boolean chosen = child == card;
                                    child.setBackground(nenVien(chosen ? 0xFF2A1719 : DEN_THE,
                                            chosen ? DO : 0xFF303030, 17));
                                    LinearLayout rowView = (LinearLayout) ((LinearLayout) child).getChildAt(0);
                                    TextView mark = (TextView) rowView.getChildAt(1);
                                    mark.setText(chosen ? "✓" : "○"); mark.setTextColor(chosen ? DO : XAM);
                                }
                            });
                        }
                    }

                    @Override
                    public void thatBai(String error) {
                        loading.setVisibility(View.GONE);
                        baoLoi("Không tải được gói tập của " + tenGym + ": " + error);
                    }
                });
    }

    private void hienGoiTap() {
        if (TextUtils.isEmpty(gymId())) {
            if (laQuanTri()) hienTaoThuongHieu();
            else hienDanhSachGymDangKyGoi();
            return;
        }
        taoTrang(laQuanTri() ? "QUẢN LÝ GÓI TẬP" : "GÓI TẬP", tenGymHienTai, this::hienBangDieuKhien, true);
        ProgressBar loading = new ProgressBar(this);
        loading.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(DO));
        thanTrang.addView(loading, new LinearLayout.LayoutParams(dp(36), dp(36)));
        kho.layTheoTruong("goi_tap", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> data) {
                loading.setVisibility(View.GONE);
                List<Map<String, Object>> filtered = new ArrayList<>();
                for (Map<String, Object> item : data) {
                    if (laQuanTri() || KhoDuLieu.booleanValue(item.get("trang_thai"))) filtered.add(item);
                }
                if (filtered.isEmpty()) {
                    if (laQuanTri()) {
                        themTheThongKeGoi(0, 0);
                        themNutGoiTap("＋  THÊM GÓI TẬP", true, () -> hienFormGoiTap(null));
                    }
                    LinearLayout empty = the("Chưa có gói tập", laQuanTri() ? "Hãy tạo gói đầu tiên cho thương hiệu " + tenGymHienTai + "." : "Gym hiện chưa có gói tập khả dụng.");
                    if (laQuanTri()) themNutVaoThe(empty, "TẠO GÓI TẬP ĐẦU TIÊN", true, () -> hienFormGoiTap(null));
                    return;
                }
                Collections.sort(filtered, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
                if (laQuanTri()) {
                    int activeCount = 0;
                    for (Map<String, Object> item : filtered) if (KhoDuLieu.booleanValue(item.get("trang_thai"))) activeCount++;
                    themTheThongKeGoi(filtered.size(), activeCount);
                    themNutGoiTap("＋  THÊM GÓI TẬP", true, () -> hienFormGoiTap(null));
                    themBoLocGoiTap(filtered);
                    return;
                } else {
                    themChu("Các gói tập thuộc thương hiệu " + tenGymHienTai, 14, XAM, false);
                }
                for (Map<String, Object> item : filtered) {
                    String name = KhoDuLieu.chuoi(item.get("ten_goi"));
                    String description = KhoDuLieu.chuoi(item.get("mo_ta"));
                    LinearLayout card = taoTheGoiTap(item, laQuanTri());
                    if (laQuanTri()) {
                        themNutVaoThe(card, "SỬA GÓI", false, () -> hienFormGoiTap(item));
                        themNutVaoThe(card, "XÓA GÓI", false, () -> xacNhanXoaGoi(item));
                        themNutVaoThe(card, KhoDuLieu.booleanValue(item.get("trang_thai")) ? "ẨN GÓI" : "MỞ BÁN GÓI", false, () -> doiTrangThaiGoi(item));
                    } else {
                        themNutVaoThe(card, "ĐĂNG KÝ GÓI", true, () -> dangKyGoi(item));
                    }
                }
            }
            @Override public void thatBai(String error) { loading.setVisibility(View.GONE); baoLoi("Không tải được gói tập: " + error); }
        });
    }

    private void themTheThongKeGoi(int total, int active) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF3B1518, 0xFF171315, 0xFF281315});
        bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), 0xFF713238);
        card.setBackground(bg);
        LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL);
        left.addView(chu("TỔNG SỐ GÓI TẬP", 11, XAM, true));
        left.addView(chu(total + " gói", 23, TRANG, true));
        View divider = new View(this); divider.setBackgroundColor(0xFF704044);
        LinearLayout.LayoutParams divP = new LinearLayout.LayoutParams(dp(1), dp(42));
        divP.setMargins(dp(18), 0, dp(18), 0);
        LinearLayout right = new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL);
        right.addView(chu("●  ĐANG HOẠT ĐỘNG", 11, 0xFF8FE3A0, true));
        right.addView(chu(active + " gói", 22, 0xFF8FE3A0, true));
        card.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(divider, divP);
        card.addView(right, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(14);
        thanTrang.addView(card, p);
    }

    private void themNutGoiTap(String label, boolean primary, Runnable action) {
        Button b = taoNut(label, primary, action);
        b.setTextSize(15); b.setMinHeight(dp(48));
        thanTrang.addView(b);
    }

    private void themBoLocGoiTap(List<Map<String, Object>> packages) {
        EditText search = oNhap("⌕  Tìm kiếm gói tập...", InputType.TYPE_CLASS_TEXT, "");
        LinearLayout chips = new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.bottomMargin = dp(12);
        thanTrang.addView(chips, cp);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(list);
        final String[] filter = {"Tất cả"};
        Runnable render = () -> {
            list.removeAllViews();
            String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            int shown = 0;
            for (Map<String, Object> item : packages) {
                boolean active = KhoDuLieu.booleanValue(item.get("trang_thai"));
                String name = KhoDuLieu.chuoi(item.get("ten_goi"));
                if (!name.toLowerCase(Locale.ROOT).contains(q)) continue;
                if ("Đang hoạt động".equals(filter[0]) && !active) continue;
                if ("Ngừng hoạt động".equals(filter[0]) && active) continue;
                LinearLayout card = taoTheGoiTap(item, true);
                themNutVaoThe(card, "✎  SỬA", false, () -> hienFormGoiTap(item));
                themNutVaoThe(card, "▣  XÓA", false, () -> xacNhanXoaGoi(item));
                themNutVaoThe(card, active ? "TẠM NGỪNG HOẠT ĐỘNG" : "KÍCH HOẠT GÓI", false, () -> doiTrangThaiGoi(item));
                list.addView(card);
                shown++;
            }
            if (shown == 0) list.addView(chu("Không tìm thấy gói tập phù hợp.", 13, XAM, false));
        };
        String[] labels = {"Tất cả", "Đang hoạt động", "Ngừng hoạt động"};
        for (String label : labels) {
            TextView chip = chu(label, 11, TRANG, true);
            chip.setGravity(Gravity.CENTER); chip.setPadding(dp(10), dp(9), dp(10), dp(9));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.setMargins(0, 0, dp(7), 0);
            chips.addView(chip, lp);
            chip.setBackground(nenVien(label.equals(filter[0]) ? DO : DEN_PHU, label.equals(filter[0]) ? DO : 0xFF555555, 22));
            chip.setOnClickListener(v -> {
                filter[0] = label;
                for (int i = 0; i < chips.getChildCount(); i++) {
                    TextView c = (TextView) chips.getChildAt(i);
                    String t = c.getText().toString();
                    c.setBackground(nenVien(t.equals(filter[0]) ? DO : DEN_PHU, t.equals(filter[0]) ? DO : 0xFF555555, 22));
                }
                render.run();
            });
        }
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { render.run(); }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });
        render.run();
    }

    private LinearLayout taoTheGoiTap(Map<String, Object> item, boolean owner) {
        String name = KhoDuLieu.chuoi(item.get("ten_goi"));
        String description = KhoDuLieu.chuoi(item.get("mo_ta"));
        boolean active = KhoDuLieu.booleanValue(item.get("trang_thai"));
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(10));
        card.setBackground(nenVien(active ? 0xFF21191A : DEN_THE, active ? 0xFFB83B40 : 0xFF555555, 16));
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(-1, -2); cardP.bottomMargin = dp(12);
        if (!owner) thanTrang.addView(card, cardP);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setOrientation(LinearLayout.HORIZONTAL);
        TextView icon = chu("✦", 21, 0xFFFFB36B, true); icon.setGravity(Gravity.CENTER);
        icon.setBackground(nenVien(0xFF422326, 0xFF63343A, 14));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(44), dp(44)); ip.setMargins(0, 0, dp(12), 0); top.addView(icon, ip);
        LinearLayout titleCol = new LinearLayout(this); titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.addView(chu(name, 17, TRANG, true));
        titleCol.addView(chu(tien(laySo(item, "gia")), 18, DO, true));
        top.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = chu(active ? "● Đang hoạt động" : "● Ngừng hoạt động", 11, active ? 0xFF8FE3A0 : 0xFFFFC46B, true);
        status.setGravity(Gravity.CENTER); status.setPadding(dp(8), dp(6), dp(8), dp(6));
        status.setBackground(nenVien(active ? 0xFF193323 : 0xFF392919, active ? 0xFF315D3B : 0xFF745329, 18));
        top.addView(status); card.addView(top);
        TextView duration = chu("◷  Thời hạn: " + laySo(item, "so_ngay") + " ngày", 13, TRANG, true);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2); dp.topMargin = dp(9); card.addView(duration, dp);
        if (!description.isEmpty()) {
            TextView desc = chu(description, 13, XAM, false); desc.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams descP = new LinearLayout.LayoutParams(-1, -2); descP.topMargin = dp(5); card.addView(desc, descP);
        }
        View line = new View(this); line.setBackgroundColor(0xFF4B393B);
        LinearLayout.LayoutParams lineP = new LinearLayout.LayoutParams(-1, dp(1)); lineP.topMargin = dp(10); lineP.bottomMargin = dp(7); card.addView(line, lineP);
        return card;
    }

    private void hienFormGoiTap(Map<String, Object> old) {
        boolean editMode = old != null;
        taoTrang(editMode ? "CHỈNH SỬA GÓI TẬP" : "THÊM GÓI TẬP", tenGymHienTai, this::hienGoiTap, false);
        EditText ten = truong("Tên gói tập", "Ví dụ: Gói 1 tháng", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, editMode ? KhoDuLieu.chuoi(old.get("ten_goi")) : "");
        EditText gia = truong("Giá tiền (VNĐ)", "300000", InputType.TYPE_CLASS_NUMBER, editMode ? KhoDuLieu.chuoi(old.get("gia")) : "");
        EditText days = truong("Thời hạn (số ngày)", "30", InputType.TYPE_CLASS_NUMBER, editMode ? KhoDuLieu.chuoi(old.get("so_ngay")) : "");
        EditText description = truong("Mô tả", "Mô tả quyền lợi của gói", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, editMode ? KhoDuLieu.chuoi(old.get("mo_ta")) : "");
        description.setSingleLine(false);
        description.setMinHeight(dp(100));
        themChu("Trạng thái gói", 13, XAM, true);
        Spinner status = spinner(java.util.Arrays.asList("Đang hoạt động", "Ẩn khỏi danh sách hội viên"));
        if (editMode && !KhoDuLieu.booleanValue(old.get("trang_thai"))) status.setSelection(1);
        nut("LƯU GÓI TẬP", true, () -> {
            String name = ten.getText().toString().trim();
            String amountText = gia.getText().toString().trim();
            String dayText = days.getText().toString().trim();
            if (name.isEmpty()) { ten.setError("Vui lòng nhập tên gói"); return; }
            if (amountText.isEmpty()) { gia.setError("Vui lòng nhập giá"); return; }
            if (dayText.isEmpty()) { days.setError("Vui lòng nhập thời hạn"); return; }
            long amount; long duration;
            try { amount = Long.parseLong(amountText); duration = Long.parseLong(dayText); }
            catch (NumberFormatException e) { baoLoi("Giá và thời hạn phải là số hợp lệ."); return; }
            if (amount <= 0) { gia.setError("Giá phải lớn hơn 0"); return; }
            if (duration <= 0 || duration > 3650) { days.setError("Thời hạn cần từ 1 đến 3650 ngày"); return; }
            Map<String, Object> data = new HashMap<>();
            data.put("gym_id", gymId());
            data.put("ten_goi", name);
            data.put("gia", amount);
            data.put("so_ngay", duration);
            data.put("mo_ta", description.getText().toString().trim());
            data.put("trang_thai", status.getSelectedItemPosition() == 0);
            data.put("nguoi_tao_uid", uid);
            data.put("ngay_cap_nhat", System.currentTimeMillis());
            if (editMode) {
                kho.capNhat("goi_tap", KhoDuLieu.chuoi(old.get("id")), data, new KhoDuLieu.KetQua<Boolean>() {
                    @Override public void thanhCong(Boolean ok) { thongBao("Đã cập nhật gói tập."); hienGoiTap(); }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            } else {
                data.put("ngay_tao", System.currentTimeMillis());
                kho.them("goi_tap", data, new KhoDuLieu.KetQua<String>() {
                    @Override public void thanhCong(String id) { thongBao("Đã thêm gói tập vào " + tenGymHienTai + "."); hienGoiTap(); }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            }
        });
        nut("HỦY", false, this::hienGoiTap);
    }

    private void xacNhanXoaGoi(Map<String, Object> item) {
        new AlertDialog.Builder(this).setTitle("Xóa gói tập")
                .setMessage("Xóa gói '" + KhoDuLieu.chuoi(item.get("ten_goi")) + "'? Các đăng ký cũ sẽ vẫn được giữ làm lịch sử.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> kho.xoa("goi_tap", KhoDuLieu.chuoi(item.get("id")), new KhoDuLieu.KetQua<Boolean>() {
                    @Override public void thanhCong(Boolean ok) { thongBao("Đã xóa gói tập."); hienGoiTap(); }
                    @Override public void thatBai(String error) { baoLoi(error); }
                })).show();
    }

    private void doiTrangThaiGoi(Map<String, Object> item) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("trang_thai", !KhoDuLieu.booleanValue(item.get("trang_thai")));
        patch.put("ngay_cap_nhat", System.currentTimeMillis());
        kho.capNhat("goi_tap", KhoDuLieu.chuoi(item.get("id")), patch, new KhoDuLieu.KetQua<Boolean>() {
            @Override public void thanhCong(Boolean ok) { hienGoiTap(); }
            @Override public void thatBai(String error) { baoLoi(error); }
        });
    }

    private void dangKyGoi(Map<String, Object> pack) {
        new AlertDialog.Builder(this)
                .setTitle("Đăng ký gói tập")
                .setMessage("Bạn muốn gửi yêu cầu đăng ký '" + KhoDuLieu.chuoi(pack.get("ten_goi")) + "' với giá " + tien(laySo(pack, "gia")) + "?")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Gửi yêu cầu", (dialog, which) -> {
                    kho.layTheoTruong("dang_ky_goi", "uid_nguoi_dung", uid, new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                        @Override public void thanhCong(List<Map<String, Object>> all) {
                            for (Map<String, Object> old : all) {
                                String st = KhoDuLieu.chuoi(old.get("trang_thai"));
                                if (KhoDuLieu.chuoi(pack.get("id")).equals(KhoDuLieu.chuoi(old.get("goi_id"))) && ("cho_xac_nhan".equals(st) || "dang_hoat_dong".equals(st))) {
                                    baoLoi("Bạn đã có yêu cầu hoặc đăng ký đang hoạt động cho gói này.");
                                    return;
                                }
                            }
                            long now = System.currentTimeMillis();
                            Map<String, Object> data = new HashMap<>();
                            data.put("uid_nguoi_dung", uid);
                            data.put("ten_hoi_vien", tenNguoiDung());
                            data.put("gym_id", KhoDuLieu.chuoi(pack.get("gym_id")));
                            data.put("goi_id", KhoDuLieu.chuoi(pack.get("id")));
                            data.put("ten_goi", KhoDuLieu.chuoi(pack.get("ten_goi")));
                            data.put("gia", laySo(pack, "gia"));
                            data.put("so_ngay", laySo(pack, "so_ngay"));
                            data.put("ngay_dang_ky", now);
                            data.put("ngay_bat_dau", now);
                            data.put("ngay_ket_thuc", now + Math.max(1, laySo(pack, "so_ngay")) * 86400000L);
                            data.put("trang_thai", "cho_xac_nhan");
                            kho.them("dang_ky_goi", data, new KhoDuLieu.KetQua<String>() {
                                @Override public void thanhCong(String id) {
                                    // Gửi thông báo trực tiếp đến tài khoản chủ Gym sở hữu thương hiệu.
                                    String selectedGymId = KhoDuLieu.chuoi(pack.get("gym_id"));
                                    kho.layMot("phong_gym", selectedGymId, new KhoDuLieu.KetQua<Map<String, Object>>() {
                                        @Override public void thanhCong(Map<String, Object> gym) {
                                            String ownerUid = gym == null ? "" : KhoDuLieu.chuoi(gym.get("chu_gym_id"));
                                            if (ownerUid.isEmpty()) {
                                                thongBao("Đã gửi yêu cầu. Chủ Gym có thể xem trong mục Yêu cầu gói.");
                                                hienDanhSachGymDangKyGoi();
                                                return;
                                            }
                                            Map<String, Object> notice = new HashMap<>();
                                            notice.put("gym_id", selectedGymId);
                                            notice.put("uid_nhan", ownerUid);
                                            notice.put("tieu_de", "Yêu cầu đăng ký gói mới");
                                            notice.put("noi_dung", tenNguoiDung() + " vừa đăng ký gói '" + KhoDuLieu.chuoi(pack.get("ten_goi")) + "'. Vui lòng xem và xác nhận.");
                                            notice.put("loai_thong_bao", "yeu_cau_goi");
                                            notice.put("dang_ky_goi_id", id);
                                            notice.put("nguoi_tao_uid", uid);
                                            notice.put("ngay_tao", System.currentTimeMillis());
                                            notice.put("danh_sach_da_doc", new ArrayList<String>());
                                            kho.them("thong_bao", notice, new KhoDuLieu.KetQua<String>() {
                                                @Override public void thanhCong(String noticeId) {
                                                    thongBao("Đã gửi yêu cầu. Chủ Gym sẽ nhận được thông báo để xác nhận.");
                                                    hienDanhSachGymDangKyGoi();
                                                }
                                                @Override public void thatBai(String error) {
                                                    thongBao("Yêu cầu đã được lưu. Chủ Gym vẫn có thể duyệt trong mục Yêu cầu gói.");
                                                    hienDanhSachGymDangKyGoi();
                                                }
                                            });
                                        }
                                        @Override public void thatBai(String error) {
                                            thongBao("Yêu cầu đã được lưu. Chủ Gym có thể duyệt trong mục Yêu cầu gói.");
                                            hienDanhSachGymDangKyGoi();
                                        }
                                    });
                                }
                                @Override public void thatBai(String error) { baoLoi(error); }
                            });
                        }
                        @Override public void thatBai(String error) { baoLoi(error); }
                    });
                }).show();
    }

    // ---------------------------------------------------------------------
    // F04 - LỊCH TẬP
    // ---------------------------------------------------------------------

    private void hienLichTap() {
        taoTrang("LỊCH TẬP", TextUtils.isEmpty(tenGymHienTai) ? "GymManagement" : tenGymHienTai, this::hienBangDieuKhien, true);

        // Banner dùng chính ảnh tạ đòn người dùng cung cấp, đã xử lý tone đỏ/đen.
        android.widget.FrameLayout hero = new android.widget.FrameLayout(this);
        hero.setBackground(nenVien(0xFF171315, 0xFF6F343D, 18));
        hero.setClipToOutline(true);
        android.widget.ImageView bannerImage = new android.widget.ImageView(this);
        bannerImage.setImageResource(R.drawable.gym_schedule_banner);
        bannerImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        hero.addView(bannerImage, new android.widget.FrameLayout.LayoutParams(-1, dp(142)));
        View imageShade = new View(this);
        imageShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x12000000, 0x18000000, 0xD9000000}));
        hero.addView(imageShade, new android.widget.FrameLayout.LayoutParams(-1, dp(142)));
        LinearLayout bannerCaption = new LinearLayout(this);
        bannerCaption.setOrientation(LinearLayout.VERTICAL);
        bannerCaption.setGravity(Gravity.BOTTOM);
        bannerCaption.setPadding(dp(15), dp(12), dp(15), dp(13));
        TextView bannerEyebrow = chu("GYM MANAGEMENT  /  WORKOUT", 9, 0xFFFFB7BC, true);
        bannerCaption.addView(bannerEyebrow);
        TextView bannerTitle = chu("LỊCH TẬP LUYỆN", 19, TRANG, true);
        LinearLayout.LayoutParams bannerTitleP = new LinearLayout.LayoutParams(-1, -2);
        bannerTitleP.topMargin = dp(4);
        bannerCaption.addView(bannerTitle, bannerTitleP);
        hero.addView(bannerCaption, new android.widget.FrameLayout.LayoutParams(-1, dp(142), Gravity.BOTTOM));
        LinearLayout.LayoutParams heroP = new LinearLayout.LayoutParams(-1, -2);
        heroP.bottomMargin = dp(14);
        thanTrang.addView(hero, heroP);

        if (laQuanTri()) {
            Button create = taoNutNho("＋  TẠO LỊCH TẬP & PHÂN CA", true, this::hienFormLichTap);
            create.setTextSize(13);
            create.setMinHeight(dp(48));
            LinearLayout.LayoutParams createP = new LinearLayout.LayoutParams(-1, -2);
            createP.bottomMargin = dp(14);
            thanTrang.addView(create, createP);
        } else {
            LinearLayout personalIntro = new LinearLayout(this);
            personalIntro.setOrientation(LinearLayout.VERTICAL);
            personalIntro.setPadding(dp(15), dp(13), dp(15), dp(13));
            personalIntro.setBackground(taoNenGradient(new int[]{0xFF35191F, 0xFF211719}, 17, 0xFF8D3945));
            personalIntro.addView(chu("✦  LỘ TRÌNH CỦA RIÊNG BẠN", 13, 0xFFFFA0A5, true));
            TextView hint = chu("Chủ động chọn ngày, giờ và bài tập phù hợp với mục tiêu của bạn.", 11, 0xFFE1D5D8, false);
            LinearLayout.LayoutParams hintP = new LinearLayout.LayoutParams(-1, -2); hintP.topMargin = dp(5);
            personalIntro.addView(hint, hintP);
            LinearLayout.LayoutParams introP = new LinearLayout.LayoutParams(-1, -2); introP.bottomMargin = dp(10);
            thanTrang.addView(personalIntro, introP);
            Button createPersonal = taoNutNho("＋  TỰ XẾP LỊCH TẬP", true, this::hienFormLichTapCaNhan);
            createPersonal.setTextSize(13); createPersonal.setMinHeight(dp(48));
            LinearLayout.LayoutParams personalP = new LinearLayout.LayoutParams(-1, -2); personalP.bottomMargin = dp(14);
            thanTrang.addView(createPersonal, personalP);
        }
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(list);

        if (laQuanTri()) {
            kho.layTheoTruong("lich_tap", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                @Override public void thanhCong(List<Map<String, Object>> all) {
                    renderLichTapList(all);
                }
                @Override public void thatBai(String error) { baoLoi("Không tải được lịch tập: " + error); }
            });
        } else {
            kho.layTatCa("lich_tap", new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                @Override public void thanhCong(List<Map<String, Object>> all) {
                    List<Map<String, Object>> memberSchedules = new ArrayList<>();
                    if (all != null) {
                        for (Map<String, Object> item : all) {
                            String assignedUid = KhoDuLieu.chuoi(item.get("uid_hoi_vien"));
                            if (assignedUid.equals(uid)) {
                                memberSchedules.add(item);
                            }
                        }
                    }
                    renderLichTapList(memberSchedules);
                }
                @Override public void thatBai(String error) { baoLoi("Không tải được lịch tập: " + error); }
            });
        }
    }

    private void renderLichTapList(List<Map<String, Object>> all) {
        if (all == null) all = new ArrayList<>();

        List<Map<String, Object>> uncompleted = new ArrayList<>();
        List<Map<String, Object>> completed = new ArrayList<>();

        for (Map<String, Object> item : all) {
            if (KhoDuLieu.booleanValue(item.get("da_hoan_thanh"))) {
                completed.add(item);
            } else {
                uncompleted.add(item);
            }
        }

        // Lịch chưa hoàn thành: ngày gần nhất lên trước. Ngày được lưu dạng YYYY-MM-DD.
        Collections.sort(uncompleted, (a, b) -> {
            String da = KhoDuLieu.chuoi(a.get("ngay"));
            String db = KhoDuLieu.chuoi(b.get("ngay"));
            int byDate = da.compareTo(db);
            return byDate != 0 ? byDate : KhoDuLieu.chuoi(a.get("gio")).compareTo(KhoDuLieu.chuoi(b.get("gio")));
        });
        Collections.sort(completed, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));

        // Hai thẻ tổng quan phản ánh dữ liệu thật của tài khoản hiện tại.
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsP = new LinearLayout.LayoutParams(-1, -2);
        statsP.bottomMargin = dp(18);
        thanTrang.addView(stats, statsP);
        stats.addView(taoTheThongKeLich("BUỔI CHƯA HOÀN THÀNH", String.valueOf(uncompleted.size()), 0xFFFF7777),
                new LinearLayout.LayoutParams(0, -2, 1));
        View statGap = new View(this);
        stats.addView(statGap, new LinearLayout.LayoutParams(dp(9), dp(1)));
        stats.addView(taoTheThongKeLich("ĐÃ HOÀN THÀNH", String.valueOf(completed.size()), 0xFF77D99A),
                new LinearLayout.LayoutParams(0, -2, 1));

        if (uncompleted.isEmpty() && completed.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(25), dp(20), dp(25));
            empty.setBackground(nenVien(DEN_THE, 0xFF3D3539, 17));
            TextView icon = chu("✦", 30, 0xFFFF646B, true); icon.setGravity(Gravity.CENTER); empty.addView(icon);
            TextView title = chu("CHƯA CÓ LỊCH TẬP", 16, TRANG, true); title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(-1, -2); titleP.topMargin = dp(8); empty.addView(title, titleP);
            TextView message = chu(laQuanTri() ? "Tạo lịch đầu tiên và phân buổi tập cho hội viên." : "Bạn chưa có buổi tập nào. Nhấn TỰ XẾP LỊCH TẬP để tạo kế hoạch riêng nhé.", 12, XAM, false);
            message.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams messageP = new LinearLayout.LayoutParams(-1, -2); messageP.topMargin = dp(5); empty.addView(message, messageP);
            thanTrang.addView(empty);
            return;
        }

        if (!uncompleted.isEmpty()) themChu("◷  LỊCH TẬP SẮP TỚI", 15, TRANG, true);
        for (Map<String, Object> item : uncompleted) {
            renderScheduleCard(item, false);
        }

        if (!completed.isEmpty()) {
            themChu("✓  LỊCH TẬP ĐÃ HOÀN THÀNH", 15, 0xFF83D99A, true);
            for (Map<String, Object> item : completed) {
                renderScheduleCard(item, true);
            }
        }
    }

    private LinearLayout taoTheThongKeLich(String label, String value, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(nenVien(0xFF202022, 0xFF3B363A, 14));
        TextView number = chu(value, 23, accent, true);
        card.addView(number);
        TextView caption = chu(label, 9, XAM, true);
        LinearLayout.LayoutParams captionP = new LinearLayout.LayoutParams(-1, -2);
        captionP.topMargin = dp(3);
        card.addView(caption, captionP);
        return card;
    }

    private void renderScheduleCard(Map<String, Object> item, boolean daHoanThanh) {
        String assignedMember = KhoDuLieu.chuoi(item.get("ten_hoi_vien"));
        String workoutName = KhoDuLieu.chuoi(item.get("ten_lich"));
        String date = KhoDuLieu.chuoi(item.get("ngay"));
        String time = KhoDuLieu.chuoi(item.get("gio"));
        String description = KhoDuLieu.chuoi(item.get("mo_ta"));
        String diet = KhoDuLieu.chuoi(item.get("huong_dan_an_uong"));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(13));
        card.setBackground(nenVien(daHoanThanh ? 0xFF1B2420 : 0xFF211C1E,
                daHoanThanh ? 0xFF385C45 : 0xFF754047, 17));
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(-1, -2);
        cardP.bottomMargin = dp(12);
        thanTrang.addView(card, cardP);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = chu(daHoanThanh ? "✓" : "✦", 19, daHoanThanh ? 0xFF8CE0A5 : 0xFFFF7777, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(nenVien(daHoanThanh ? 0xFF203D2C : 0xFF482027,
                daHoanThanh ? 0xFF477B58 : 0xFF81414A, 12));
        top.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setPadding(dp(10), 0, 0, 0);
        TextView name = chu(workoutName.isEmpty() ? "Buổi tập" : workoutName, 16, TRANG, true);
        nameCol.addView(name);
        TextView status = chu(daHoanThanh ? "●  ĐÃ HOÀN THÀNH" : "●  ĐANG CHỜ TẬP", 10,
                daHoanThanh ? 0xFF8CE0A5 : 0xFFFF9999, true);
        LinearLayout.LayoutParams statusP = new LinearLayout.LayoutParams(-1, -2);
        statusP.topMargin = dp(4); nameCol.addView(status, statusP);
        top.addView(nameCol, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(top);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setPadding(dp(10), dp(9), dp(10), dp(9));
        meta.setBackground(nenVien(0xFF171719, 0xFF373236, 10));
        LinearLayout.LayoutParams metaP = new LinearLayout.LayoutParams(-1, -2);
        metaP.topMargin = dp(12); card.addView(meta, metaP);
        TextView dateText = chu("▦  " + date, 12, 0xFFE9E2E4, true);
        meta.addView(dateText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView timeText = chu("◷  " + time, 12, 0xFFFFA0A0, true);
        meta.addView(timeText);

        if (laQuanTri() && !assignedMember.isEmpty()) {
            TextView member = chu("HỘI VIÊN  ·  " + assignedMember, 11, 0xFFD8C9CC, true);
            LinearLayout.LayoutParams memberP = new LinearLayout.LayoutParams(-1, -2);
            memberP.topMargin = dp(10); card.addView(member, memberP);
        }
        if (!description.isEmpty()) {
            TextView contentTitle = chu("NỘI DUNG BUỔI TẬP", 10, 0xFFFF858B, true);
            LinearLayout.LayoutParams contentTitleP = new LinearLayout.LayoutParams(-1, -2);
            contentTitleP.topMargin = dp(12); card.addView(contentTitle, contentTitleP);
            TextView content = chu(description, 13, 0xFFD5D0D2, false);
            content.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams contentP = new LinearLayout.LayoutParams(-1, -2);
            contentP.topMargin = dp(5); card.addView(content, contentP);
        }
        if (!diet.isEmpty()) {
            View divider = new View(this); divider.setBackgroundColor(0xFF40383B);
            LinearLayout.LayoutParams dividerP = new LinearLayout.LayoutParams(-1, dp(1));
            dividerP.topMargin = dp(11); dividerP.bottomMargin = dp(9); card.addView(divider, dividerP);
            card.addView(chu("DINH DƯỠNG & LƯU Ý", 10, 0xFFFFB46B, true));
            TextView dietText = chu(diet, 12, 0xFFD5D0D2, false);
            dietText.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams dietP = new LinearLayout.LayoutParams(-1, -2);
            dietP.topMargin = dp(5); card.addView(dietText, dietP);
        }
        if (laQuanTri()) {
            themNutVaoThe(card, "XÓA LỊCH", false, () -> new AlertDialog.Builder(MainActivity.this).setTitle("Xóa lịch tập?").setNegativeButton("Hủy", null).setPositiveButton("Xóa", (d, w) -> kho.xoa("lich_tap", KhoDuLieu.chuoi(item.get("id")), new KhoDuLieu.KetQua<Boolean>() {
                @Override public void thanhCong(Boolean ok) { hienLichTap(); }
                @Override public void thatBai(String error) { baoLoi(error); }
            })).show());
        } else {
            if (daHoanThanh) {
                Button btnDone = taoNutNho("✓  BẠN ĐÃ HOÀN THÀNH BUỔI TẬP", false, null);
                btnDone.setEnabled(false);
                card.addView(btnDone);
            } else {
                themNutVaoThe(card, "BẮT ĐẦU TẬP LUYỆN  →", true, () -> hienGiaoDienTapLuyen(item));
            }
        }
    }

    private Exercise findExerciseByName(String name) {
        try {
            List<Exercise> catalog = ExerciseCatalog.load(this);
            for (Exercise e : catalog) {
                if (e.name.equalsIgnoreCase(name) || e.name.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) {
                    return e;
                }
            }
            return !catalog.isEmpty() ? catalog.get(0) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String selectedMemberUid = "";
    private String selectedMemberName = "Tất cả hội viên (Công khai)";

    private void hienFormLichTap() {
        taoTrang("TẠO LỊCH TẬP & DINH DƯỠNG", tenGymHienTai, this::hienLichTap, false);
        // Giữ lại hội viên được chọn từ màn hình Quản lý hội viên.
        final String presetMemberUid = selectedMemberUid;
        final String presetMemberName = selectedMemberName;
        EditText name = truong("Tên buổi tập", "Ví dụ: Cardio & Đẩy ngực", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, "");
        EditText date = truong("Ngày (YYYY-MM-DD)", ngayHomNay(), InputType.TYPE_CLASS_TEXT, ngayHomNay());

        // Thay trường nhập giờ thô bằng nút chọn ca tập có sẵn hoặc nhập tự do
        final String[] caTapOptions = {
                "🌅 Ca sáng: 6h - 7h (06:00)",
                "🌅 Ca sáng: 7h - 8h (07:00)",
                "🌅 Ca sáng: 8h - 9h (08:00)",
                "☀️ Ca chiều: 2h - 3h (14:00)",
                "☀️ Ca chiều: 3h - 4h (15:00)",
                "☀️ Ca chiều: 4h - 5h (16:00)",
                "✏️ Nhập giờ tự do khác..."
        };

        final String[] timeValues = {"06:00", "07:00", "08:00", "14:00", "15:00", "16:00", ""};
        
        Button btnTime = taoNut("⏰ Chọn ca tập: 06:00 (Ca sáng: 6h - 7h)", false, null);
        final String[] selectedTimeHolder = {"06:00"};

        btnTime.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Chọn khung giờ / ca tập")
                    .setItems(caTapOptions, (dialog, which) -> {
                        if (which == 6) {
                            // Nhập giờ tự do
                            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                            alert.setTitle("Nhập giờ tự do");
                            final EditText input = new EditText(MainActivity.this);
                            input.setHint("Ví dụ: 06:00 - 07:00");
                            input.setTextColor(0xFF000000);
                            input.setHintTextColor(0xFF888888);
                            // Dùng TYPE_CLASS_TEXT để cho phép gõ dấu gạch ngang "-"
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            alert.setView(input);
                            alert.setPositiveButton("Xác nhận", (d, w) -> {
                                String customTime = input.getText().toString().trim();
                                if (!customTime.isEmpty()) {
                                    selectedTimeHolder[0] = customTime;
                                    btnTime.setText("⏰ Giờ tập: " + selectedTimeHolder[0] + " (Tự do)");
                                }
                            });
                            alert.setNegativeButton("Hủy", null);
                            alert.show();
                        } else {
                            selectedTimeHolder[0] = timeValues[which];
                            btnTime.setText("⏰ Ca tập: " + caTapOptions[which]);
                        }
                    })
                    .show();
        });
        thanTrang.addView(btnTime);

        selectedMemberUid = presetMemberUid;
        selectedMemberName = presetMemberName;
        Button btnMember = taoNut(TextUtils.isEmpty(presetMemberUid)
                ? "👤 Chọn hội viên nhận lịch tập..."
                : "👤 Hội viên: " + presetMemberName, false, null);
        btnMember.setOnClickListener(v -> {
            kho.layTatCa("nguoi_dung", new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                @Override public void thanhCong(List<Map<String, Object>> members) {
                    List<String> names = new ArrayList<>();
                    List<String> uids = new ArrayList<>();
                    if (members != null) {
                        for (Map<String, Object> m : members) {
                            if ("hoi_vien".equals(KhoDuLieu.chuoi(m.get("vai_tro")))) {
                                String mGym = KhoDuLieu.chuoi(m.get("gym_id"));
                                if (mGym.isEmpty() || mGym.equals(gymId())) {
                                    names.add(KhoDuLieu.chuoi(m.get("ho_ten")) + " (" + KhoDuLieu.chuoi(m.get("email")) + ")");
                                    uids.add(KhoDuLieu.chuoi(m.get("id")));
                                }
                            }
                        }
                    }
                    if (names.isEmpty()) {
                        baoLoi("Không tìm thấy hội viên nào trong hệ thống.");
                        return;
                    }
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Chọn hội viên nhận lịch tập")
                            .setItems(names.toArray(new String[0]), (d, which) -> {
                                selectedMemberUid = uids.get(which);
                                selectedMemberName = names.get(which);
                                btnMember.setText("👤 Hội viên: " + selectedMemberName);
                            })
                            .show();
                }
                @Override public void thatBai(String error) { baoLoi(error); }
            });
        });
        thanTrang.addView(btnMember);

        EditText note = truong("Nội dung bài tập (Mỗi dòng 1 bài)", "Chọn từ thư viện bên dưới hoặc nhập tay", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, "");
        note.setSingleLine(false);
        note.setMinHeight(dp(90));

        nut("+ CHỌN BÀI TẬP TỪ THƯ VIỆN", false, () -> {
            try {
                List<Exercise> catalog = ExerciseCatalog.load(this);
                String[] exNames = new String[catalog.size()];
                for (int i = 0; i < catalog.size(); i++) exNames[i] = catalog.get(i).name + " (" + catalog.get(i).category + ")";
                boolean[] checked = new boolean[catalog.size()];
                new AlertDialog.Builder(this)
                        .setTitle("Chọn bài tập từ Thư viện")
                        .setMultiChoiceItems(exNames, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                        .setPositiveButton("Thêm vào lịch", (dialog, which) -> {
                            StringBuilder sb = new StringBuilder(note.getText().toString().trim());
                            for (int i = 0; i < catalog.size(); i++) {
                                if (checked[i]) {
                                    if (sb.length() > 0) sb.append("\n");
                                    sb.append(catalog.get(i).name);
                                }
                            }
                            note.setText(sb.toString());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            } catch (Exception e) {
                baoLoi("Không tải được thư viện bài tập: " + e.getMessage());
            }
        });

        EditText diet = truong("Dinh dưỡng / Thực đơn hôm nay", "Ví dụ: Ăn nhiều protein, uống 3L nước, tránh đồ ngọt", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, "");
        diet.setSingleLine(false);
        diet.setMinHeight(dp(70));

        nut("LƯU LỊCH TẬP & DINH DƯỠNG", true, () -> {
            String n = name.getText().toString().trim();
            String d = date.getText().toString().trim();
            String t = selectedTimeHolder[0];
            if (n.isEmpty()) { name.setError("Nhập tên buổi tập"); return; }
            if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) { date.setError("Dùng định dạng YYYY-MM-DD"); return; }
            if (t.isEmpty()) { baoLoi("Vui lòng chọn hoặc nhập giờ bắt đầu."); return; }
            if (selectedMemberUid.isEmpty()) {
                baoLoi("Vui lòng chọn hội viên để phân lịch tập cá nhân!");
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("gym_id", gymId());
            data.put("ten_lich", n);
            data.put("ngay", d);
            data.put("gio", t);
            data.put("mo_ta", note.getText().toString().trim());
            data.put("huong_dan_an_uong", diet.getText().toString().trim());
            data.put("uid_hoi_vien", selectedMemberUid);
            data.put("ten_hoi_vien", selectedMemberName);
            data.put("nguoi_tao_uid", uid);
            data.put("trang_thai", "sap_dien_ra");
            data.put("ngay_tao", System.currentTimeMillis());
            kho.them("lich_tap", data, new KhoDuLieu.KetQua<String>() {
                @Override public void thanhCong(String id) { thongBao("Đã phân lịch tập & dinh dưỡng thành công."); hienLichTap(); }
                @Override public void thatBai(String error) { baoLoi(error); }
            });
        });
        nut("HỦY", false, this::hienLichTap);
    }

    /** Hội viên tự tạo lịch cá nhân, không cần chờ chủ Gym phân buổi tập. */
    private void hienFormLichTapCaNhan() {
        if (laQuanTri()) { hienFormLichTap(); return; }
        taoTrang("TẠO LỊCH CÁ NHÂN", "Thiết kế buổi tập theo mục tiêu của bạn", this::hienLichTap, false);

        LinearLayout intro = new LinearLayout(this);
        intro.setOrientation(LinearLayout.VERTICAL); intro.setPadding(dp(15), dp(14), dp(15), dp(14));
        intro.setBackground(taoNenGradient(new int[]{0xFF3B1B22, 0xFF211719}, 18, 0xFF9D3D49));
        intro.addView(chu("YOUR WORKOUT · YOUR RULES", 10, 0xFFFFA0A5, true));
        TextView introText = chu("Một kế hoạch nhỏ hôm nay sẽ tạo nên thay đổi lớn ngày mai.", 13, TRANG, true);
        LinearLayout.LayoutParams itp = new LinearLayout.LayoutParams(-1, -2); itp.topMargin = dp(6); intro.addView(introText, itp);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2); ip.bottomMargin = dp(15); thanTrang.addView(intro, ip);

        themChu("01  ·  THÔNG TIN BUỔI TẬP", 13, 0xFFFF858D, true);
        EditText name = truong("Tên buổi tập", "Ví dụ: Ngực + Tay sau, Cardio...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, "");
        EditText date = truong("Ngày tập (YYYY-MM-DD)", "Chọn ngày tập", InputType.TYPE_CLASS_DATETIME, ngayHomNay());
        final String[] timeValue = {"06:00"};
        Button timeButton = taoNut("◷  GIỜ TẬP  ·  06:00", false, null);
        timeButton.setOnClickListener(v -> {
            String[] options = {"05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "Nhập giờ khác"};
            new AlertDialog.Builder(this).setTitle("Chọn giờ bắt đầu").setItems(options, (d, which) -> {
                if (which == options.length - 1) {
                    EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Ví dụ 18:30"); input.setInputType(InputType.TYPE_CLASS_DATETIME);
                    new AlertDialog.Builder(this).setTitle("Giờ tập tùy chỉnh").setView(input)
                            .setNegativeButton("Hủy", null).setPositiveButton("Chọn", (dd, ww) -> {
                                String value = input.getText().toString().trim();
                                if (value.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) { timeValue[0] = value; timeButton.setText("◷  GIỜ TẬP  ·  " + value); }
                                else baoLoi("Giờ không hợp lệ. Ví dụ: 18:30");
                            }).show();
                } else { timeValue[0] = options[which]; timeButton.setText("◷  GIỜ TẬP  ·  " + timeValue[0]); }
            }).show();
        });
        thanTrang.addView(timeButton);

        themChu("02  ·  BÀI TẬP CỦA BẠN", 13, 0xFFFF858D, true);
        EditText note = truong("Danh sách bài tập", "Mỗi bài tập viết trên một dòng. Ví dụ:\nBench Press – 3 hiệp x 12 lần\nPush Up – 3 hiệp x 15 lần", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, "");
        note.setSingleLine(false); note.setMinHeight(dp(112));
        nut("＋  CHỌN TỪ THƯ VIỆN BÀI TẬP", false, () -> {
            try {
                List<Exercise> catalog = ExerciseCatalog.load(this);
                String[] labels = new String[catalog.size()]; boolean[] checked = new boolean[catalog.size()];
                for (int i = 0; i < catalog.size(); i++) labels[i] = catalog.get(i).name + "  ·  " + catalog.get(i).category;
                new AlertDialog.Builder(this).setTitle("Thêm bài tập vào kế hoạch")
                        .setMultiChoiceItems(labels, checked, (d, which, selected) -> checked[which] = selected)
                        .setNegativeButton("Hủy", null).setPositiveButton("Thêm bài tập", (d, w) -> {
                            StringBuilder value = new StringBuilder(note.getText().toString().trim());
                            for (int i = 0; i < catalog.size(); i++) if (checked[i]) { if (value.length() > 0) value.append("\n"); value.append(catalog.get(i).name); }
                            note.setText(value.toString());
                        }).show();
            } catch (Exception e) { baoLoi("Không tải được thư viện: " + e.getMessage()); }
        });
        EditText diet = truong("Dinh dưỡng / ghi chú", "Ví dụ: Bổ sung protein, uống đủ nước...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, "");
        diet.setSingleLine(false); diet.setMinHeight(dp(76));

        themChu("03  ·  HOÀN TẤT KẾ HOẠCH", 13, 0xFFFF858D, true);
        nut("✓  LƯU LỊCH TẬP CỦA TÔI", true, () -> {
            String workout = name.getText().toString().trim();
            String day = date.getText().toString().trim();
            if (workout.isEmpty()) { name.setError("Hãy đặt tên buổi tập"); return; }
            if (!day.matches("\\d{4}-\\d{2}-\\d{2}")) { date.setError("Dùng định dạng YYYY-MM-DD"); return; }
            try { new SimpleDateFormat("yyyy-MM-dd", Locale.US).setLenient(false); SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.setLenient(false); sdf.parse(day); }
            catch (Exception e) { date.setError("Ngày không hợp lệ"); return; }
            if (day.compareTo(ngayHomNay()) < 0) { date.setError("Không thể xếp lịch vào ngày đã qua"); return; }
            if (note.getText().toString().trim().isEmpty()) { note.setError("Thêm ít nhất một bài tập"); return; }
            Map<String, Object> data = new HashMap<>();
            data.put("gym_id", gymId()); data.put("ten_lich", workout); data.put("ngay", day); data.put("gio", timeValue[0]);
            data.put("mo_ta", note.getText().toString().trim()); data.put("huong_dan_an_uong", diet.getText().toString().trim());
            data.put("uid_hoi_vien", uid); data.put("ten_hoi_vien", tenNguoiDung()); data.put("nguoi_tao_uid", uid);
            data.put("loai_lich", "hoi_vien_tu_tao"); data.put("da_hoan_thanh", false); data.put("trang_thai", "sap_dien_ra");
            data.put("ngay_tao", System.currentTimeMillis());
            kho.them("lich_tap", data, new KhoDuLieu.KetQua<String>() {
                @Override public void thanhCong(String id) { thongBao("Đã lưu kế hoạch tập luyện của bạn!"); hienLichTap(); }
                @Override public void thatBai(String error) { baoLoi("Không lưu được lịch tập: " + error); }
            });
        });
        nut("QUAY LẠI", false, this::hienLichTap);
    }

    private long totalActiveWorkoutSeconds = 0;

    private void hienGiaoDienTapLuyen(Map<String, Object> item) {
        totalActiveWorkoutSeconds = 0;
        List<String> exercises = parseExercises(KhoDuLieu.chuoi(item.get("mo_ta")));
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF121212);
        dialog.setContentView(root);
        dialog.show();
        showStepInDialog(dialog, root, item, exercises, 0, 1, false);
    }

    private List<String> parseExercises(String moTa) {
        List<String> list = new ArrayList<>();
        if (moTa == null || moTa.trim().isEmpty()) {
            list.add("Bài tập chính");
            return list;
        }
        String[] lines = moTa.split("\n");
        for (String l : lines) {
            String trimmed = l.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        if (list.isEmpty()) list.add(moTa);
        return list;
    }

    private void showStepInDialog(Dialog dialog, LinearLayout root, Map<String, Object> item, List<String> exercises, int exerciseIndex, int setNumber, boolean autoStart) {
        final Map<String, Object> safeItem = item == null ? new HashMap<>() : item;
        if (exerciseIndex >= exercises.size()) {
            dialog.dismiss();
            thongBao("🎉 Bạn đã hoàn thành hết lịch hôm nay!");

            final long finalActiveSecs = totalActiveWorkoutSeconds;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                showCompletionDialog(safeItem, finalActiveSecs);
            }, 300);
            return;
        }

        root.removeAllViews();
        String currentExercise = exercises.get(exerciseIndex);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(16), dp(16), dp(16));
        header.setBackgroundResource(R.drawable.bg_cover_gradient);

        ImageView btnBack = new ImageView(this);
        btnBack.setImageResource(R.drawable.ic_arrow_back);
        btnBack.setPadding(dp(8), dp(8), dp(8), dp(8));
        btnBack.setOnClickListener(v -> dialog.dismiss());
        header.addView(btnBack);

        TextView title = new TextView(this);
        title.setText("🏋️ Bài " + (exerciseIndex + 1) + "/" + exercises.size() + " · Set " + setNumber + "/3");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(16), 0, 0, 0);
        header.addView(title);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView tvExName = new TextView(this);
        tvExName.setText(currentExercise);
        tvExName.setTextColor(0xFFE53935);
        tvExName.setTextSize(18);
        tvExName.setTypeface(null, Typeface.BOLD);
        tvExName.setPadding(0, 0, 0, dp(12));
        content.addView(tvExName);

        VideoPlayerView videoView = new VideoPlayerView(this);
        Exercise matchedEx = findExerciseByName(currentExercise);
        String videoUrl = (matchedEx != null && !matchedEx.videoUrl.isEmpty()) ? matchedEx.videoUrl : Exercise.getRelevantVideoUrl(matchedEx != null ? matchedEx.category : null);
        String videoType = (matchedEx != null && !matchedEx.videoType.isEmpty()) ? matchedEx.videoType : "mp4";
        String posterUrl = (matchedEx != null && !matchedEx.imageUrl.isEmpty()) ? matchedEx.imageUrl : (matchedEx != null ? matchedEx.videoThumbnailUrl : null);
        videoView.setSource(videoUrl, videoType, posterUrl);
        content.addView(videoView);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("Sẵn sàng tập Set " + setNumber + " (30 giây tập, 10 giây nghỉ)");
        tvStatus.setTextColor(0xFFFF5722);
        tvStatus.setTextSize(18);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, dp(16), 0, dp(8));
        content.addView(tvStatus);

        TextView tvTimer = new TextView(this);
        tvTimer.setText("00:30");
        tvTimer.setTextColor(0xFFFFFFFF);
        tvTimer.setTextSize(48);
        tvTimer.setTypeface(null, Typeface.BOLD);
        tvTimer.setGravity(Gravity.CENTER);
        content.addView(tvTimer);

        Button btnStart = new Button(this);
        btnStart.setText("BẮT ĐẦU SET " + setNumber + " (30s)");
        btnStart.setBackgroundColor(0xFFE53935);
        btnStart.setTextColor(0xFFFFFFFF);
        btnStart.setPadding(dp(16), dp(12), dp(16), dp(12));
        content.addView(btnStart);

        Button btnNext = new Button(this);
        btnNext.setText("BỎ QUA / QUA BÀI TIẾP");
        btnNext.setBackgroundColor(0xFF4CAF50);
        btnNext.setTextColor(0xFFFFFFFF);
        btnNext.setPadding(dp(16), dp(12), dp(16), dp(12));
        btnNext.setVisibility(View.GONE);
        content.addView(btnNext);

        scroll.addView(content);
        root.addView(scroll);

        final android.os.CountDownTimer[] activeTimer = {null};

        btnStart.setOnClickListener(v -> {
            btnStart.setEnabled(false);
            tvStatus.setText("🔥 Đang tập Set " + setNumber + " (30 giây)");
            videoView.start();
            btnNext.setVisibility(View.VISIBLE);

            activeTimer[0] = new android.os.CountDownTimer(30000, 1000) {
                @Override public void onTick(long millisUntilFinished) {
                    long sec = millisUntilFinished / 1000;
                    tvTimer.setText(String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60));
                    totalActiveWorkoutSeconds++;
                }
                @Override public void onFinish() {
                    videoView.stop();
                    if (setNumber < 3) {
                        tvStatus.setText("☕ Nghỉ giữa hiệp (10 giây)");
                        new android.os.CountDownTimer(10000, 1000) {
                            @Override public void onTick(long millisUntilFinished) {
                                long sec = millisUntilFinished / 1000;
                                tvTimer.setText(String.format(Locale.US, "Nghỉ: %02d", sec));
                            }
                            @Override public void onFinish() {
                                showStepInDialog(dialog, root, item, exercises, exerciseIndex, setNumber + 1, true);
                            }
                        }.start();
                    } else {
                        showStepInDialog(dialog, root, item, exercises, exerciseIndex + 1, 1, false);
                    }
                }
            }.start();
        });

        btnNext.setOnClickListener(v -> {
            if (activeTimer[0] != null) activeTimer[0].cancel();
            videoView.stop();
            showStepInDialog(dialog, root, item, exercises, exerciseIndex + 1, 1, false);
        });

        dialog.setOnDismissListener(d -> videoView.stop());

        if (autoStart) {
            btnStart.performClick();
        }
    }

    private void showCompletionDialog(Map<String, Object> item, long activeSecs) {
        if (isFinishing() || isDestroyed()) return;

        String scheduleId = KhoDuLieu.chuoi(item.get("id"));
        String tenLich = KhoDuLieu.chuoi(item.get("ten_lich"));
        String dietGuide = KhoDuLieu.chuoi(item.get("huong_dan_an_uong"));
        long minutes = Math.max(1, activeSecs / 60);

        if (!scheduleId.isEmpty()) {
            Map<String, Object> patch = new HashMap<>();
            patch.put("da_hoan_thanh", true);
            kho.capNhat("lich_tap", scheduleId, patch, null);
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        content.setPadding(pad, pad, pad, pad);

        TextView tvSummary = new TextView(this);
        tvSummary.setText("🏋️ Lịch tập: " + (tenLich.isEmpty() ? "Lịch tập cá nhân" : tenLich) + "\n⏱️ Thời gian tập thực tế: " + minutes + " phút (" + activeSecs + " giây)");
        tvSummary.setTextSize(14);
        tvSummary.setTextColor(0xFF333333);
        tvSummary.setPadding(dp(12), dp(12), dp(12), dp(12));
        tvSummary.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(-1, -2);
        p1.bottomMargin = dp(12);
        content.addView(tvSummary, p1);

        TextView tvDietTitle = new TextView(this);
        tvDietTitle.setText("🥗 CHẾ ĐỘ DINH DƯỠNG DO GYM PHÂN CÔNG:");
        tvDietTitle.setTextSize(13);
        tvDietTitle.setTypeface(null, Typeface.BOLD);
        tvDietTitle.setTextColor(0xFF2E7D32);
        tvDietTitle.setPadding(0, 0, 0, dp(4));
        content.addView(tvDietTitle);

        TextView tvDiet = new TextView(this);
        tvDiet.setText(dietGuide.isEmpty() ? "Bổ sung đủ nước (2-3 lit/ngày), tăng cường protein (thịt, cá, trứng, whey) và nghỉ ngơi điều độ." : dietGuide);
        tvDiet.setTextSize(14);
        tvDiet.setTextColor(0xFF333333);
        tvDiet.setPadding(dp(12), dp(12), dp(12), dp(12));
        tvDiet.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(-1, -2);
        p2.bottomMargin = dp(12);
        content.addView(tvDiet, p2);

        TextView tvFeedbackTitle = new TextView(this);
        tvFeedbackTitle.setText("💬 CẢM NGHĨ / GHI CHÚ BUỔI TẬP (TÙY CHỌN):");
        tvFeedbackTitle.setTextSize(13);
        tvFeedbackTitle.setTypeface(null, Typeface.BOLD);
        tvFeedbackTitle.setTextColor(0xFFE65100);
        tvFeedbackTitle.setPadding(0, 0, 0, dp(4));
        content.addView(tvFeedbackTitle);

        EditText etFeedback = new EditText(this);
        etFeedback.setHint("Nhập cảm nghĩ về độ mệt, động lực hoặc nhận xét bài tập hôm nay...");
        etFeedback.setTextSize(14);
        etFeedback.setSingleLine(false);
        etFeedback.setMinHeight(dp(90));
        etFeedback.setPadding(dp(12), dp(12), dp(12), dp(12));
        etFeedback.setBackgroundResource(R.drawable.bg_search);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(-1, -2);
        p3.bottomMargin = dp(8);
        content.addView(etFeedback, p3);

        scroll.addView(content);

        new AlertDialog.Builder(this)
                .setTitle("🎉 CHÚC MỪNG HOÀN THÀNH!")
                .setView(scroll)
                .setCancelable(false)
                .setPositiveButton("HOÀN THÀNH & LƯU TIẾN ĐỘ", (dialogInterface, which) -> {
                    String noteText = etFeedback.getText().toString().trim();

                    Map<String, Object> checkin = new HashMap<>();
                    checkin.put("gym_id", gymId());
                    checkin.put("uid_nguoi_dung", uid);
                    checkin.put("ten_nguoi_dung", tenNguoiDung());
                    checkin.put("ngay", ngayHomNay());
                    checkin.put("noi_dung", "Hoàn thành lịch: " + tenLich + (noteText.isEmpty() ? "" : " - " + noteText));
                    checkin.put("ngay_tao", System.currentTimeMillis());
                    kho.them("diem_danh", checkin, null);

                    Map<String, Object> progress = new HashMap<>();
                    progress.put("uid_nguoi_dung", uid);
                    progress.put("gym_id", gymId());
                    progress.put("ten_buoi_tap", tenLich.isEmpty() ? "Lịch tập cá nhân" : tenLich);
                    progress.put("thoi_luong_phut", minutes);
                    progress.put("ngay", ngayHomNay());
                    progress.put("ghi_chu", noteText.isEmpty() ? "Đã hoàn thành lịch tập." : noteText);
                    progress.put("ngay_tao", System.currentTimeMillis());

                    kho.them("tien_do", progress, new KhoDuLieu.KetQua<String>() {
                        @Override
                        public void thanhCong(String id) {
                            thongBao("🎉 Đã hoàn thành lịch tập và ghi nhận tiến độ!");
                            hienLichTap();
                        }

                        @Override
                        public void thatBai(String error) {
                            thongBao("Đã hoàn thành lịch tập!");
                            hienLichTap();
                        }
                    });
                })
                .show();
    }

    // ---------------------------------------------------------------------
    // F05 - THƯ VIỆN BÀI TẬP
    // ---------------------------------------------------------------------

    private void hienThuVienBaiTap() {
        taoTrang("THƯ VIỆN BÀI TẬP", "Khám phá · Luyện tập · Phát triển", this::hienBangDieuKhien, true);
        // Giữ lại bộ lọc khi người dùng mở chi tiết rồi quay về thư viện.
        exerciseFavorites.clear();
        SharedPreferences prefs = getSharedPreferences("gym_exercise_favorites", MODE_PRIVATE);
        exerciseFavorites.addAll(prefs.getStringSet("favorites_" + uid, new HashSet<>()));
        exerciseCatalog.clear();
        try {
            exerciseCatalog.addAll(ExerciseCatalog.load(this));
        } catch (Exception e) {
            themChu("Không tải được danh mục bài tập trong ứng dụng.", 14, XAM, false);
            return;
        }

        // Banner hình ảnh gym thực tế, phủ lớp tối để tiêu đề dễ đọc.
        FrameLayout banner = new FrameLayout(this);
        banner.setBackground(nenVien(0xFF241316, 0xFF6C292F, 18));
        banner.setClipToOutline(true);
        ImageView cover = new ImageView(this);
        cover.setImageResource(R.drawable.home_gym_banner);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        banner.addView(cover, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        GradientDrawable shadeBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xE610090B, 0xB51A080B, 0x35100000});
        shade.setBackground(shadeBg);
        banner.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout bannerText = new LinearLayout(this);
        bannerText.setOrientation(LinearLayout.VERTICAL);
        bannerText.setGravity(Gravity.CENTER_VERTICAL);
        bannerText.setPadding(dp(17), dp(13), dp(12), dp(13));
        TextView bannerTitle = chu("THƯ VIỆN\nBÀI TẬP", 24, TRANG, true);
        bannerTitle.setShadowLayer(dp(7), 0, dp(2), 0xCC000000);
        bannerText.addView(bannerTitle);
        TextView bannerSub = chu("Hơn " + exerciseCatalog.size() + " bài tập · Hướng dẫn chi tiết", 11, 0xFFFFB6B8, true);
        LinearLayout.LayoutParams bannerSubP = new LinearLayout.LayoutParams(-1, -2);
        bannerSubP.topMargin = dp(5);
        bannerText.addView(bannerSub, bannerSubP);
        banner.addView(bannerText, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams bannerP = new LinearLayout.LayoutParams(-1, dp(142));
        bannerP.bottomMargin = dp(14);
        thanTrang.addView(banner, bannerP);

        // Tìm kiếm + nút thêm chỉ dành cho chủ Gym.
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchRowP = new LinearLayout.LayoutParams(-1, -2);
        searchRowP.bottomMargin = dp(12);
        thanTrang.addView(searchRow, searchRowP);
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(TRANG);
        search.setHintTextColor(0xFF969198);
        search.setHint("⌕  Tìm bài tập, nhóm cơ, dụng cụ...");
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setPadding(dp(14), dp(9), dp(12), dp(9));
        search.setBackground(nenVien(0xFF252326, 0xFF484047, 24));
        searchRow.addView(search, new LinearLayout.LayoutParams(0, dp(49), 1));
        if (laQuanTri()) {
            Button add = taoNutNho("＋", true, () -> hienFormBaiTap(null));
            add.setTextSize(23);
            LinearLayout.LayoutParams addP = new LinearLayout.LayoutParams(dp(49), dp(49));
            addP.setMargins(dp(9), 0, 0, 0);
            searchRow.addView(add, addP);
        }

        // Chế độ tập: Gym / tại nhà / bài tập có dụng cụ.
        HorizontalScrollView modeScroll = new HorizontalScrollView(this);
        modeScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] modes = {"Phòng gym", "Tại nhà", "Dụng cụ của tôi"};
        for (String mode : modes) {
            TextView chip = taoChipBaiTap(mode, mode.equals(exerciseMode));
            LinearLayout.LayoutParams chipP = new LinearLayout.LayoutParams(-2, dp(40));
            chipP.setMargins(0, 0, dp(8), 0);
            modeRow.addView(chip, chipP);
            chip.setOnClickListener(v -> {
                exerciseMode = mode;
                for (int i = 0; i < modeRow.getChildCount(); i++) {
                    TextView c = (TextView) modeRow.getChildAt(i);
                    c.setBackground(nenVien(c.getText().toString().equals(exerciseMode) ? DO : DEN_PHU,
                            c.getText().toString().equals(exerciseMode) ? DO : 0xFF555057, 22));
                    c.setTextColor(c.getText().toString().equals(exerciseMode) ? TRANG : XAM);
                }
                renderExerciseList();
            });
        }
        modeScroll.addView(modeRow);
        LinearLayout.LayoutParams modeP = new LinearLayout.LayoutParams(-1, -2);
        modeP.bottomMargin = dp(13);
        thanTrang.addView(modeScroll, modeP);

        exerciseFilterHost = new LinearLayout(this);
        exerciseFilterHost.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(exerciseFilterHost);
        LinearLayout countRow = new LinearLayout(this);
        countRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams countRowP = new LinearLayout.LayoutParams(-1, -2);
        countRowP.topMargin = dp(4);
        countRowP.bottomMargin = dp(9);
        thanTrang.addView(countRow, countRowP);
        exerciseCount = chu("", 12, XAM, true);
        countRow.addView(exerciseCount, new LinearLayout.LayoutParams(0, -2, 1));
        countRow.addView(chu("CHỌN NHÓM CƠ", 9, 0xFFFF7777, true));

        // Cột nhóm cơ bên trái + lưới bài tập 2 cột bên phải.
        LinearLayout catalogRow = new LinearLayout(this);
        catalogRow.setOrientation(LinearLayout.HORIZONTAL);
        thanTrang.addView(catalogRow, new LinearLayout.LayoutParams(-1, -2));
        ScrollView categoryScroll = new ScrollView(this);
        categoryScroll.setVerticalScrollBarEnabled(false);
        LinearLayout categoryColumn = new LinearLayout(this);
        categoryColumn.setOrientation(LinearLayout.VERTICAL);
        categoryColumn.setPadding(0, dp(2), dp(8), dp(8));
        categoryScroll.addView(categoryColumn);
        String[] sideCategories = {"Ngực", "Lưng", "Vai", "Cánh tay", "Chân", "Mông", "Cơ core", "Sức bật", "Khởi động", "Cardio", "Quyền Anh", "Đã lưu"};
        String[] sideIcons = {"♧", "♙", "✥", "♧", "♢", "◉", "▦", "ϟ", "↗", "♡", "🥊", "♡"};
        for (int i = 0; i < sideCategories.length; i++) {
            String category = sideCategories[i];
            boolean selected = category.equals(exerciseSidebarCategory);
            LinearLayout sideItem = new LinearLayout(this);
            sideItem.setOrientation(LinearLayout.VERTICAL);
            sideItem.setGravity(Gravity.CENTER);
            sideItem.setPadding(dp(4), dp(10), dp(4), dp(10));
            sideItem.setBackground(nenVien(selected ? 0xFF641D25 : 0x00101010,
                    selected ? DO : 0x00101010, 13));
            TextView icon = chu(sideIcons[i], 17, selected ? TRANG : 0xFFAAA5AC, true);
            icon.setGravity(Gravity.CENTER);
            sideItem.addView(icon);
            TextView label = chu(category, 10, selected ? TRANG : 0xFFAAA5AC, selected);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            LinearLayout.LayoutParams labelP = new LinearLayout.LayoutParams(-1, -2);
            labelP.topMargin = dp(3);
            sideItem.addView(label, labelP);
            LinearLayout.LayoutParams sideP = new LinearLayout.LayoutParams(dp(78), dp(61));
            sideP.bottomMargin = dp(4);
            categoryColumn.addView(sideItem, sideP);
            sideItem.setOnClickListener(v -> {
                exerciseSidebarCategory = category;
                renderExerciseSidebar(categoryColumn, sideCategories, sideIcons);
                renderExerciseList();
            });
        }
        catalogRow.addView(categoryScroll, new LinearLayout.LayoutParams(dp(82), -1));
        exerciseListHost = new LinearLayout(this);
        exerciseListHost.setOrientation(LinearLayout.VERTICAL);
        catalogRow.addView(exerciseListHost, new LinearLayout.LayoutParams(0, -2, 1));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                exerciseQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                renderExerciseList();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        renderExerciseFilters();
        renderExerciseList();
    }

    private void renderExerciseFilters() {
        if (exerciseFilterHost == null) return;
        exerciseFilterHost.removeAllViews();
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] filters = {"Tất cả", "Có video", "Không dụng cụ", "Tạ đơn", "Tạ đòn", "Máy", "Thể trọng", "Dây kháng lực", "Xà kép", "Vòng treo"};
        for (String filter : filters) {
            boolean selected = filter.equals("Tất cả")
                    ? exerciseEquipmentFilter.isEmpty() && !exerciseOnlyVideo
                    : filter.equals("Có video") ? exerciseOnlyVideo : filter.equals(exerciseEquipmentFilter);
            TextView chip = taoChipBaiTap(filter, selected);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(34));
            cp.setMargins(0, 0, dp(7), 0);
            row.addView(chip, cp);
            chip.setOnClickListener(v -> {
                if (filter.equals("Tất cả")) {
                    exerciseEquipmentFilter = "";
                    exerciseOnlyVideo = false;
                } else if (filter.equals("Có video")) {
                    exerciseOnlyVideo = !exerciseOnlyVideo;
                    if (exerciseOnlyVideo) exerciseEquipmentFilter = "";
                } else {
                    exerciseEquipmentFilter = exerciseEquipmentFilter.equals(filter) ? "" : filter;
                    exerciseOnlyVideo = false;
                }
                renderExerciseFilters();
                renderExerciseList();
            });
        }
        scroll.addView(row);
        exerciseFilterHost.addView(scroll);
    }

    private TextView taoChipBaiTap(String label, boolean selected) {
        TextView chip = chu(label, 11, selected ? TRANG : 0xFFD0CCD1, selected);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(13), dp(7), dp(13), dp(7));
        chip.setBackground(nenVien(selected ? DO : 0xFF252326,
                selected ? 0xFFFF6268 : 0xFF555057, 22));
        return chip;
    }

    private void renderExerciseSidebar(LinearLayout host, String[] categories, String[] icons) {
        host.removeAllViews();
        for (int i = 0; i < categories.length; i++) {
            final String category = categories[i];
            boolean selected = category.equals(exerciseSidebarCategory);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(8), dp(4), dp(8));
            item.setBackground(nenVien(selected ? 0xFF641D25 : 0x00101010,
                    selected ? DO : 0x00101010, 13));
            TextView ico = chu(icons[i], 17, selected ? TRANG : 0xFFAAA5AC, true);
            ico.setGravity(Gravity.CENTER); item.addView(ico);
            TextView name = chu(category, 10, selected ? TRANG : 0xFFAAA5AC, selected);
            name.setGravity(Gravity.CENTER); name.setMaxLines(2);
            LinearLayout.LayoutParams nameP = new LinearLayout.LayoutParams(-1, -2);
            nameP.topMargin = dp(3); item.addView(name, nameP);
            LinearLayout.LayoutParams itemP = new LinearLayout.LayoutParams(dp(78), dp(61));
            itemP.bottomMargin = dp(4); host.addView(item, itemP);
            item.setOnClickListener(v -> {
                exerciseSidebarCategory = category;
                renderExerciseSidebar(host, categories, icons);
                renderExerciseList();
            });
        }
    }

    private void addExerciseChipRow(String[] values, boolean categoryFilter) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2), dp(8), dp(5));
        for (int i = 0; i < values.length; i++) {
            String value = i == 0 ? "" : values[i];
            String selected = categoryFilter ? exerciseCategory : exerciseDifficulty;
            boolean checked = selected.equals(value);
            TextView chip = chu(values[i], 12, checked ? TRANG : XAM, checked);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(13), dp(7), dp(13), dp(7));
            chip.setBackground(nenVien(checked ? DO : DEN_PHU, checked ? DO : Color.rgb(67, 67, 67), 18));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2);
            chipParams.rightMargin = dp(7);
            row.addView(chip, chipParams);
            chip.setOnClickListener(v -> {
                if (categoryFilter) exerciseCategory = value;
                else exerciseDifficulty = value;
                renderExerciseFilters();
                renderExerciseList();
            });
        }
        scroll.addView(row, new HorizontalScrollView.LayoutParams(-2, -2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(2);
        exerciseFilterHost.addView(scroll, params);
    }

    private void renderExerciseList() {
        if (exerciseListHost == null || exerciseCount == null) return;
        exerciseListHost.removeAllViews();
        List<Exercise> filtered = new ArrayList<>();
        for (Exercise exercise : exerciseCatalog) {
            if (!matchesExerciseSidebar(exercise)) continue;
            if (!exerciseDifficulty.isEmpty() && !exerciseDifficulty.equals(exercise.difficulty)) continue;
            if (!exerciseQuery.isEmpty() && !exercise.searchText().contains(exerciseQuery)) continue;
            if (exerciseOnlyVideo && !exercise.hasVideo()) continue;
            String equipment = exercise.equipment == null ? "" : exercise.equipment.toLowerCase(Locale.ROOT);
            if (!exerciseEquipmentFilter.isEmpty() && !matchesExerciseEquipment(equipment, exerciseEquipmentFilter)) continue;
            boolean noEquipment = equipment.contains("không cần") || equipment.contains("không dụng cụ");
            if ("Tại nhà".equals(exerciseMode) && !noEquipment && !equipment.contains("tùy chọn")) continue;
            if ("Dụng cụ của tôi".equals(exerciseMode) && noEquipment) continue;
            if ("Phòng gym".equals(exerciseMode) && noEquipment) continue;
            if ("Đã lưu".equals(exerciseSidebarCategory) && !exerciseFavorites.contains(exercise.id)) continue;
            filtered.add(exercise);
        }
        exerciseCount.setText(filtered.size() + " BÀI TẬP");
        if (filtered.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(22), dp(12), dp(22));
            empty.setBackground(nenVien(DEN_THE, 0xFF393438, 14));
            TextView emptyTitle = chu("Chưa có bài tập phù hợp", 13, TRANG, true);
            emptyTitle.setGravity(Gravity.CENTER); empty.addView(emptyTitle);
            TextView emptySub = chu("Thử đổi nhóm cơ hoặc bộ lọc.", 11, XAM, false);
            emptySub.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2); ep.topMargin = dp(5); empty.addView(emptySub, ep);
            exerciseListHost.addView(empty);
            return;
        }
        for (int i = 0; i < filtered.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(-1, -2);
            rowP.bottomMargin = dp(9);
            exerciseListHost.addView(row, rowP);
            row.addView(addExerciseCard(filtered.get(i)), new LinearLayout.LayoutParams(0, -2, 1));
            if (i + 1 < filtered.size()) {
                View gap = new View(this);
                row.addView(gap, new LinearLayout.LayoutParams(dp(7), dp(1)));
                row.addView(addExerciseCard(filtered.get(i + 1)), new LinearLayout.LayoutParams(0, -2, 1));
            } else {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            }
        }
    }

    private boolean matchesExerciseSidebar(Exercise exercise) {
        String selected = exerciseSidebarCategory;
        if (selected == null || selected.isEmpty()) return true;
        if ("Đã lưu".equals(selected)) return exerciseFavorites.contains(exercise.id);
        if ("Cánh tay".equals(selected)) return "Tay".equalsIgnoreCase(exercise.category);
        if ("Cơ core".equals(selected)) return "Bụng".equalsIgnoreCase(exercise.category);
        if ("Mông".equals(selected)) return TextUtils.join(" ", exercise.targetMuscles).toLowerCase(Locale.ROOT).contains("mông");
        if ("Sức bật".equals(selected) || "Khởi động".equals(selected) || "Quyền Anh".equals(selected)) {
            String text = exercise.searchText();
            return text.contains(selected.toLowerCase(Locale.ROOT))
                    || ("Khởi động".equals(selected) && text.contains("warm"))
                    || ("Sức bật".equals(selected) && (text.contains("jump") || text.contains("nhảy")))
                    || ("Quyền Anh".equals(selected) && (text.contains("boxing") || text.contains("đấm")));
        }
        if ("Cardio".equals(selected)) return "Cardio".equalsIgnoreCase(exercise.category);
        return selected.equalsIgnoreCase(exercise.category);
    }

    private boolean matchesExerciseEquipment(String equipment, String filter) {
        if ("Không dụng cụ".equals(filter) || "Thể trọng".equals(filter))
            return equipment.contains("không cần") || equipment.contains("không dụng cụ");
        if ("Tạ đơn".equals(filter)) return equipment.contains("tạ đơn");
        if ("Tạ đòn".equals(filter)) return equipment.contains("tạ đòn");
        if ("Máy".equals(filter)) return equipment.contains("máy");
        if ("Dây kháng lực".equals(filter)) return equipment.contains("dây kháng lực");
        if ("Xà kép".equals(filter)) return equipment.contains("xà kép");
        if ("Vòng treo".equals(filter)) return equipment.contains("vòng treo") || equipment.contains("treo");
        return equipment.contains(filter.toLowerCase(Locale.ROOT));
    }

    private LinearLayout addExerciseCard(Exercise exercise) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.setBackground(nenVien(0xFF21191B, 0xFF78353D, 15));
        card.setClickable(true);
        card.setFocusable(true);

        FrameLayout imageFrame = new FrameLayout(this);
        imageFrame.setBackground(nenVien(0xFFFFFFFF, 0xFF4B4144, 12));
        imageFrame.setClipToOutline(true);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ImageLoader.load(image, exercise.imageUrl, R.drawable.bg_gym_placeholder);
        imageFrame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        TextView play = chu("▶", 13, TRANG, true);
        play.setGravity(Gravity.CENTER);
        play.setBackground(nenVien(0xFFE5252B, 0xFFFF7777, 30));
        FrameLayout.LayoutParams playP = new FrameLayout.LayoutParams(dp(29), dp(29), Gravity.RIGHT | Gravity.BOTTOM);
        playP.setMargins(0, 0, dp(5), dp(5));
        imageFrame.addView(play, playP);
        LinearLayout.LayoutParams imageP = new LinearLayout.LayoutParams(-1, dp(112));
        imageP.bottomMargin = dp(8);
        card.addView(imageFrame, imageP);

        TextView name = chu(exercise.name, 13, TRANG, true);
        name.setMaxLines(3);
        name.setMinHeight(dp(47));
        card.addView(name);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bottomP = new LinearLayout.LayoutParams(-1, -2);
        bottomP.topMargin = dp(5);
        card.addView(bottom, bottomP);
        TextView category = chu("⌁  " + exercise.category, 10, 0xFFFFB5B8, true);
        category.setPadding(dp(7), dp(4), dp(7), dp(4));
        category.setBackground(nenVien(0xFF4A1D23, 0xFF71323A, 18));
        bottom.addView(category, new LinearLayout.LayoutParams(0, -2, 1));
        TextView favorite = chu(exerciseFavorites.contains(exercise.id) ? "♥" : "♡", 22,
                exerciseFavorites.contains(exercise.id) ? 0xFFFF5963 : TRANG, true);
        favorite.setGravity(Gravity.CENTER);
        bottom.addView(favorite, new LinearLayout.LayoutParams(dp(31), dp(31)));
        favorite.setOnClickListener(v -> {
            if (exerciseFavorites.contains(exercise.id)) exerciseFavorites.remove(exercise.id);
            else exerciseFavorites.add(exercise.id);
            getSharedPreferences("gym_exercise_favorites", MODE_PRIVATE).edit()
                    .putStringSet("favorites_" + uid, new HashSet<>(exerciseFavorites)).apply();
            favorite.setText(exerciseFavorites.contains(exercise.id) ? "♥" : "♡");
            favorite.setTextColor(exerciseFavorites.contains(exercise.id) ? 0xFFFF5963 : TRANG);
            if ("Đã lưu".equals(exerciseSidebarCategory) && !exerciseFavorites.contains(exercise.id)) renderExerciseList();
        });
        card.setOnClickListener(v -> xemChiTietBaiTap(exercise));
        return card;
    }

    private void xemChiTietBaiTap(Exercise exercise) {
        taoTrang("CHI TIẾT BÀI TẬP", exercise.category + " · " + exercise.difficulty,
                this::hienThuVienBaiTap, true);
        if (exercise.hasVideo()) {
            themChu("VIDEO HƯỚNG DẪN", 14, XAM, true);
            VideoPlayerView video = new VideoPlayerView(this);
            String poster = exercise.videoThumbnailUrl.isEmpty() ? exercise.imageUrl : exercise.videoThumbnailUrl;
            video.setSource(exercise.videoUrl, exercise.videoType, poster);
            LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(-1, -2);
            videoParams.bottomMargin = dp(10);
            thanTrang.addView(video, videoParams);
            activeExerciseVideo = video;
            video.start();
        } else {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.load(image, exercise.imageUrl, R.drawable.bg_gym_placeholder);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-1, dp(200));
            imageParams.bottomMargin = dp(10);
            thanTrang.addView(image, imageParams);
            themChu("Chưa có video cho bài này. Xem các bước hướng dẫn bên dưới.", 12, XAM, false);
        }

        if (!exercise.videoAttribution.isEmpty() || !exercise.imageAttribution.isEmpty()) {
            StringBuilder credit = new StringBuilder();
            if (!exercise.imageAttribution.isEmpty()) {
                credit.append("Ảnh: ").append(exercise.imageAttribution).append(" · ").append(exercise.imageLicense);
            }
            if (!exercise.videoAttribution.isEmpty()) {
                if (credit.length() > 0) credit.append('\n');
                credit.append("Video: ").append(exercise.videoAttribution).append(" · ").append(exercise.videoLicense);
            }
            TextView attribution = themChu(credit.toString() + "\nNguồn và giấy phép: wger.de", 10, XAM, false);
            String source = !exercise.videoSourceUrl.isEmpty() ? exercise.videoSourceUrl : exercise.imageSourceUrl;
            attribution.setOnClickListener(v -> openExternalUrl(source));
            String licenseUrl = !exercise.videoLicenseUrl.isEmpty()
                    ? exercise.videoLicenseUrl : exercise.imageLicenseUrl;
            TextView licenseLink = themChu("Mở giấy phép " + (!exercise.videoLicense.isEmpty()
                    ? exercise.videoLicense : exercise.imageLicense), 10, DO, false);
            licenseLink.setOnClickListener(v -> openExternalUrl(licenseUrl));
        }

        themChu(exercise.name, 22, TRANG, true);
        the("NHÓM CƠ TÁC ĐỘNG", "Chính: " + TextUtils.join(", ", exercise.targetMuscles)
                + "\nPhụ: " + (exercise.secondaryMuscles.isEmpty() ? "Không có" : TextUtils.join(", ", exercise.secondaryMuscles)));
        the("THÔNG SỐ GỢI Ý", exercise.recommendedSets + " hiệp · " + exercise.recommendedReps
                + " lần · nghỉ " + exercise.recommendedRestSeconds + " giây\nDụng cụ: " + exercise.equipment);
        if (!exercise.description.isEmpty()) the("GIỚI THIỆU", exercise.description);
        if (!exercise.setupPosition.isEmpty()) the("TƯ THẾ CHUẨN BỊ", exercise.setupPosition);
        if (!exercise.bodyPlacement.isEmpty()) the("VỊ TRÍ ĐẶT TAY / CHÂN / THÂN", exercise.bodyPlacement);
        if (!exercise.instructions.isEmpty()) {
            StringBuilder steps = new StringBuilder();
            for (int i = 0; i < exercise.instructions.size(); i++) {
                if (steps.length() > 0) steps.append('\n');
                steps.append(i + 1).append(". ").append(exercise.instructions.get(i));
            }
            the("CÁC BƯỚC THỰC HIỆN", steps.toString());
        }
        if (!exercise.breathingGuide.isEmpty()) the("NHỊP THỞ", exercise.breathingGuide);
        if (!exercise.rangeOfMotion.isEmpty()) the("BIÊN ĐỘ CHUYỂN ĐỘNG", exercise.rangeOfMotion);
        if (!exercise.commonMistakes.isEmpty()) the("LỖI THƯỜNG GẶP", "• " + TextUtils.join("\n• ", exercise.commonMistakes));
        if (!exercise.safetyNotes.isEmpty()) the("LƯU Ý AN TOÀN", "• " + TextUtils.join("\n• ", exercise.safetyNotes));
    }

    private void openExternalUrl(String url) {
        if (url == null || !url.startsWith("https://")) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception e) {
            thongBao("Không mở được liên kết nguồn.");
        }
    }

    private void hienFormBaiTap(Map<String, Object> old) {
        boolean edit = old != null;
        taoTrang(edit ? "SỬA BÀI TẬP" : "THÊM BÀI TẬP", tenGymHienTai, this::hienThuVienBaiTap, false);
        EditText name = truong("Tên bài tập", "Ví dụ: Squat", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, edit ? KhoDuLieu.chuoi(old.get("ten_bai_tap")) : "");
        EditText muscle = truong("Nhóm cơ", "Chân, ngực, lưng...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, edit ? KhoDuLieu.chuoi(old.get("nhom_co")) : "");
        EditText level = truong("Độ khó", "Cơ bản / Trung bình / Nâng cao", InputType.TYPE_CLASS_TEXT, edit ? KhoDuLieu.chuoi(old.get("do_kho")) : "Cơ bản");
        EditText equipment = truong("Dụng cụ", "Không cần / Tạ / Máy...", InputType.TYPE_CLASS_TEXT, edit ? KhoDuLieu.chuoi(old.get("dung_cu")) : "");
        EditText sets = truong("Số hiệp", "3", InputType.TYPE_CLASS_NUMBER, edit ? KhoDuLieu.chuoi(old.get("so_hiep")) : "3");
        EditText reps = truong("Số lần / thời lượng", "12", InputType.TYPE_CLASS_TEXT, edit ? KhoDuLieu.chuoi(old.get("so_lan")) : "12");
        EditText instruction = truong("Hướng dẫn thực hiện", "Mô tả từng bước", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, edit ? KhoDuLieu.chuoi(old.get("huong_dan")) : "");
        instruction.setSingleLine(false);
        instruction.setMinHeight(dp(110));
        nut("LƯU BÀI TẬP", true, () -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) { name.setError("Vui lòng nhập tên bài tập"); return; }
            Map<String, Object> data = new HashMap<>();
            data.put("gym_id", gymId());
            data.put("ten_bai_tap", n);
            data.put("nhom_co", muscle.getText().toString().trim());
            data.put("do_kho", level.getText().toString().trim());
            data.put("dung_cu", equipment.getText().toString().trim());
            data.put("so_hiep", sets.getText().toString().trim());
            data.put("so_lan", reps.getText().toString().trim());
            data.put("huong_dan", instruction.getText().toString().trim());
            data.put("nguoi_tao_uid", uid);
            data.put("ngay_cap_nhat", System.currentTimeMillis());
            if (edit) {
                kho.capNhat("bai_tap", KhoDuLieu.chuoi(old.get("id")), data, new KhoDuLieu.KetQua<Boolean>() {
                    @Override public void thanhCong(Boolean ok) { thongBao("Đã cập nhật bài tập."); hienThuVienBaiTap(); }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            } else {
                data.put("ngay_tao", System.currentTimeMillis());
                kho.them("bai_tap", data, new KhoDuLieu.KetQua<String>() {
                    @Override public void thanhCong(String id) { thongBao("Đã thêm bài tập."); hienThuVienBaiTap(); }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            }
        });
    }

    // ---------------------------------------------------------------------
    // F06 - CHỈ SỐ CƠ THỂ
    // ---------------------------------------------------------------------

    private void hienChiSoCoThe() {
        taoTrang("CHỈ SỐ CƠ THỂ", "Theo dõi sức khỏe và thể trạng", this::hienBangDieuKhien, true);
        thanTrang.setPadding(dp(14), dp(12), dp(14), dp(24));
        kho.layTheoTruong("chi_so_co_the", "uid_nguoi_dung", uid, new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> items) {
                if (items == null) items = new ArrayList<>();
                Collections.sort(items, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
                Map<String, Object> latest = items.isEmpty() ? null : items.get(0);
                themBannerChiSoCoThe(bmiTuMap(latest));
                LinearLayout section = new LinearLayout(MainActivity.this);
                section.setGravity(Gravity.CENTER_VERTICAL);
                TextView sectionTitle = chu("▣  Chỉ số gần nhất", 19, TRANG, true);
                section.addView(sectionTitle, new LinearLayout.LayoutParams(0, -2, 1));
                section.addView(chu(latest == null ? "Chưa có dữ liệu" : "▦  " + KhoDuLieu.chuoi(latest.get("ngay_do")), 11, XAM, false));
                LinearLayout.LayoutParams sectionP = new LinearLayout.LayoutParams(-1, -2);
                sectionP.topMargin = dp(14); sectionP.bottomMargin = dp(12);
                thanTrang.addView(section, sectionP);

                if (latest == null) {
                    the("Chưa có dữ liệu đo", "Cập nhật cân nặng, chiều cao và tỷ lệ mỡ để bắt đầu theo dõi hành trình của bạn.");
                    Button first = nut("＋  CẬP NHẬT CHỈ SỐ", true, MainActivity.this::hienFormChiSo);
                    first.setMinHeight(dp(56));
                    return;
                }
                double weight = doubleValue(latest.get("can_nang"));
                double height = doubleValue(latest.get("chieu_cao"));
                double bmi = doubleValue(latest.get("bmi"));
                double fat = doubleValue(latest.get("ti_le_mo"));
                String bmiLabel = nhanBMI(bmi);

                LinearLayout metricRow = new LinearLayout(MainActivity.this);
                metricRow.setOrientation(LinearLayout.HORIZONTAL);
                metricRow.addView(theChiSoMini("⚖", "Cân nặng", decimal(weight), "kg", "−", 0xFFFF454D), new LinearLayout.LayoutParams(0, dp(150), 1));
                metricRow.addView(theChiSoMini("↕", "Chiều cao", decimal(height), "cm", "", 0xFFB58BFF), taoMetricParams());
                metricRow.addView(theChiSoMini("♡", "BMI", decimal(bmi), "", bmiLabel, 0xFF28D477), taoMetricParams());
                metricRow.addView(theChiSoMini("％", "Tỷ lệ mỡ", fat > 0 ? decimal(fat) : "—", fat > 0 ? "%" : "", fat > 0 ? "Đã cập nhật" : "Chưa nhập", 0xFFFF9A35), taoMetricParams());
                thanTrang.addView(metricRow);

                Button update = nut("▧   Cập nhật chỉ số", true, MainActivity.this::hienFormChiSo);
                update.setTextSize(16); update.setMinHeight(dp(58));
                update.setBackground(taoNenGradient(new int[]{0xFFFF343A, 0xFFBD101C, 0xFFE92530}, 26, 0xFFFF676D));
                LinearLayout.LayoutParams updateP = new LinearLayout.LayoutParams(-1, dp(58));
                updateP.topMargin = dp(14); updateP.bottomMargin = dp(16);
                update.setLayoutParams(updateP);

                themTieuDeChiSo("▥  Đánh giá chỉ số BMI của bạn");
                thanTrang.addView(taoThanhBMI(bmi));

                LinearLayout chartCard = taoThe(thanTrang, "⌁  Lịch sử & tiến trình", "");
                chartCard.setPadding(dp(14), dp(14), dp(14), dp(12));
                LinearLayout legend = new LinearLayout(MainActivity.this);
                legend.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                legend.addView(chu("●  Cân nặng (kg)    ", 10, 0xFFFF555B, true));
                legend.addView(chu("●  Tỷ lệ mỡ (%)", 10, 0xFFBEB8BC, true));
                chartCard.addView(legend);
                if (items.size() >= 2) {
                    List<Map<String, Object>> chartItems = new ArrayList<>(items.subList(0, Math.min(5, items.size())));
                    Collections.reverse(chartItems);
                    chartCard.addView(new BieuDoChiSoView(chartItems), new LinearLayout.LayoutParams(-1, dp(190)));
                } else {
                    TextView hint = chu("Cần ít nhất 2 lần đo để hiển thị biểu đồ tiến trình.", 12, XAM, false);
                    chartCard.addView(hint);
                }

                LinearLayout historyCard = taoThe(thanTrang, "◷  Lịch sử đo", "");
                historyCard.setPadding(dp(14), dp(14), dp(14), dp(12));
                TextView all = chu("Xem tất cả  ›", 12, 0xFFFF555B, true);
                all.setGravity(Gravity.RIGHT);
                historyCard.addView(all);
                int count = Math.min(3, items.size());
                for (int i = 0; i < count; i++) historyCard.addView(taoDongLichSuChiSo(items.get(i)));
                final List<Map<String, Object>> historyItems = new ArrayList<>(items);
                all.setOnClickListener(v -> hienTatCaLichSuChiSo(historyItems));
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được chỉ số: " + error); }
        });
    }

    private LinearLayout.LayoutParams taoMetricParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(150), 1);
        p.setMargins(dp(6), 0, 0, 0);
        return p;
    }

    private double bmiTuMap(Map<String, Object> item) {
        return item == null ? 0 : doubleValue(item.get("bmi"));
    }

    private boolean laNu() {
        String gender = KhoDuLieu.chuoi(nguoiDung.get("gioi_tinh"));
        if (gender.isEmpty()) gender = KhoDuLieu.chuoi(nguoiDung.get("gender"));
        if (gender.isEmpty()) gender = KhoDuLieu.chuoi(nguoiDung.get("sex"));
        gender = gender.trim().toLowerCase(new Locale("vi", "VN"));
        return gender.equals("nữ") || gender.equals("nu") || gender.equals("female") || gender.equals("woman") || gender.equals("f");
    }

    private int mauTheoBMI(double bmi) {
        if (bmi <= 0) return 0xFF9BCB26;
        if (bmi < 18.5) return 0xFF45B9EE;
        if (bmi < 25) return 0xFF55D65B;
        if (bmi < 30) return 0xFFFFC52E;
        if (bmi < 40) return 0xFFFF8A28;
        return 0xFFF33F2D;
    }

    private void themBannerChiSoCoThe(double bmi) {
        LinearLayout banner = new LinearLayout(this);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(12), dp(12), dp(10), dp(12));
        banner.setBackground(taoNenGradient(new int[]{0xFF401014, 0xFF1B1014, 0xFF70151B}, 22, 0xFFB52D36));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = chu("THEO DÕI\nCHỈ SỐ CƠ THỂ", 18, TRANG, true);
        title.setLineSpacing(dp(1), 1f);
        TextView pulse = chu("⌁  ♥  ⌁", 22, mauTheoBMI(bmi), true);
        TextView subtitle = chu("Đánh giá tình trạng sức khỏe,\nkiểm soát cân nặng và tỷ lệ mỡ\nđể đạt mục tiêu tập luyện hiệu quả.", 10, 0xFFE3DADD, false);
        subtitle.setPadding(0, dp(5), 0, 0);
        copy.addView(title); copy.addView(pulse); copy.addView(subtitle);
        banner.addView(copy, new LinearLayout.LayoutParams(0, -2, 1.05f));

        FrameLayout anatomy = new FrameLayout(this);
        anatomy.addView(new BieuTuongCoTheView(bmi, laNu()), new FrameLayout.LayoutParams(-1, -1));
        banner.addView(anatomy, new LinearLayout.LayoutParams(0, dp(188), 0.82f));

        LinearLayout who = new LinearLayout(this);
        who.setOrientation(LinearLayout.VERTICAL);
        who.setGravity(Gravity.CENTER);
        who.setPadding(dp(4), dp(7), dp(4), dp(7));
        who.setBackground(nenVien(0x442A2024, 0xFF754047, 16));
        TextView whoTitle = chu("PHÂN LOẠI BMI\n(WHO)", 11, TRANG, true);
        whoTitle.setGravity(Gravity.CENTER);
        who.addView(whoTitle);
        String[] bmiText = {"<18.5\nThiếu cân", "18.5–24.9\nBình thường", "25–29.9\nThừa cân", "30–39.9\nBéo phì\nđộ 1–2", "≥40\nBéo phì\nnặng"};
        int[] bmiColors = {0xFF45B9EE, 0xFF9BCB26, 0xFFFFC52E, 0xFFFF8A28, 0xFFF33F2D};
        LinearLayout bmiLegend = new LinearLayout(this);
        bmiLegend.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < bmiText.length; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER);
            item.setOrientation(LinearLayout.VERTICAL);
            TextView person = chu("♟", 25, bmiColors[i], true);
            person.setGravity(Gravity.CENTER);
            TextView label = chu(bmiText[i], 8, 0xFFF6F0F0, false);
            label.setGravity(Gravity.CENTER);
            item.addView(person); item.addView(label);
            bmiLegend.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        who.addView(bmiLegend);
        banner.addView(who, new LinearLayout.LayoutParams(0, dp(174), 1.12f));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(212));
        p.bottomMargin = dp(15);
        thanTrang.addView(banner, p);
    }

    private class BieuTuongCoTheView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final double bmi;
        private final boolean female;
        BieuTuongCoTheView(double value, boolean isFemale) { super(MainActivity.this); bmi = value; female = isFemale; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            float scale = Math.min(w / 112f, h / 190f);
            canvas.save(); canvas.translate((w - 112f * scale) / 2f, (h - 190f * scale) / 2f); canvas.scale(scale, scale);
            int color = mauTheoBMI(bmi);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.2f); p.setColor((color & 0x00FFFFFF) | 0x66000000);
            canvas.drawCircle(56, 96, 53, p);
            p.setStyle(Paint.Style.FILL);
            p.setShadowLayer(13, 0, 0, color);
            p.setColor(color);
            android.graphics.Path body = new android.graphics.Path();
            // Silhouette toàn thân: dáng nam rộng vai hoặc dáng nữ eo/hông thon rõ hơn.
            body.moveTo(49, 26); body.quadTo(56, 19, 63, 26); body.lineTo(65, 39);
            if (female) {
                body.quadTo(72, 42, 78, 47); body.quadTo(83, 55, 82, 67);
                body.lineTo(76, 91); body.quadTo(72, 99, 69, 105);
                body.quadTo(75, 118, 72, 132); body.lineTo(68, 174);
                body.quadTo(67, 181, 62, 181); body.quadTo(57, 181, 58, 173);
                body.lineTo(55, 137); body.lineTo(53, 137); body.lineTo(50, 173);
                body.quadTo(51, 181, 46, 181); body.quadTo(41, 181, 40, 174);
                body.lineTo(36, 132); body.quadTo(33, 118, 39, 105);
                body.quadTo(36, 99, 32, 91); body.lineTo(26, 67);
                body.quadTo(25, 55, 30, 47); body.quadTo(36, 42, 47, 39);
            } else {
                body.quadTo(39, 41, 29, 45); body.quadTo(22, 48, 21, 60);
                body.lineTo(15, 91); body.quadTo(13, 98, 17, 100);
                body.quadTo(21, 102, 23, 94); body.lineTo(32, 67);
                body.lineTo(37, 65); body.lineTo(33, 100); body.quadTo(31, 111, 34, 120);
                body.lineTo(39, 174); body.quadTo(40, 182, 46, 182);
                body.quadTo(52, 182, 51, 174); body.lineTo(54, 132);
                body.lineTo(58, 132); body.lineTo(61, 174); body.quadTo(60, 182, 66, 182);
                body.quadTo(72, 182, 73, 174); body.lineTo(78, 120);
                body.quadTo(81, 111, 79, 100); body.lineTo(75, 65);
                body.lineTo(80, 67); body.lineTo(89, 94); body.quadTo(91, 102, 95, 100);
                body.quadTo(99, 98, 97, 91); body.lineTo(91, 60);
                body.quadTo(90, 48, 83, 45); body.quadTo(73, 41, 65, 39);
            }
            body.close(); canvas.drawPath(body, p);
            p.clearShadowLayer();
            // Đầu, cổ và bàn chân được nối liền thị giác như hình silhouette tham chiếu.
            p.setColor(color); canvas.drawOval(46, 3, 66, 29, p);
            canvas.drawRoundRect(49, 24, 63, 43, 5, 5, p);
            canvas.drawOval(31, 177, 48, 185, p); canvas.drawOval(61, 177, 79, 185, p);
            canvas.restore();
        }
    }

    private LinearLayout theChiSoMini(String icon, String label, String value, String unit, String note, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(5), dp(10), dp(5), dp(8));
        card.setBackground(taoNenGradient(new int[]{0xFF32191E, 0xFF19171B, 0xFF21191D}, 20, accent));
        card.setElevation(dp(3));
        TextView ico = chu(icon, 27, accent, true); ico.setGravity(Gravity.CENTER);
        card.addView(ico);
        TextView name = chu(label, 11, 0xFFE4E0E1, true); name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2); np.topMargin = dp(5); card.addView(name, np);
        LinearLayout valueRow = new LinearLayout(this); valueRow.setGravity(Gravity.CENTER);
        TextView number = chu(value, 23, TRANG, true); valueRow.addView(number);
        if (!unit.isEmpty()) { TextView u = chu(" " + unit, 12, accent, true); valueRow.addView(u); }
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2); vp.topMargin = dp(5); card.addView(valueRow, vp);
        TextView status = chu(note, 9, note.equals("Bình thường") ? 0xFFFFFFFF : 0xFFD0C8CB, false);
        status.setGravity(Gravity.CENTER); status.setMaxLines(1);
        status.setPadding(dp(4), dp(4), dp(4), dp(4));
        if (note.equals("Bình thường")) status.setBackground(taoNenGradient(new int[]{0xFF2BC34A, 0xFF168B3C}, 18, 0xFF5BEE77));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2); sp.topMargin = dp(4); card.addView(status, sp);
        return card;
    }

    private void themTieuDeChiSo(String text) {
        TextView title = chu(text, 17, TRANG, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(2); p.bottomMargin = dp(10);
        thanTrang.addView(title, p);
    }

    private String nhanBMI(double bmi) {
        if (bmi <= 0) return "Chưa đủ dữ liệu";
        if (bmi < 18.5) return "Thiếu cân";
        if (bmi < 25) return "Bình thường";
        if (bmi < 30) return "Thừa cân";
        if (bmi < 40) return "Béo phì độ 1–2";
        return "Béo phì nặng";
    }

    private View taoThanhBMI(double bmi) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"< 18.5\nThiếu cân", "18.5 – 24.9\nBình thường", "25 – 29.9\nThừa cân", "30 – 39.9\nBéo phì độ 1–2", "≥ 40\nBéo phì nặng"};
        int[] colors = {0xFF49A8D5, 0xFF24A64A, 0xFFFFC52E, 0xFFFF9632, 0xFFF3544D};
        int active = bmi < 18.5 ? 0 : bmi < 25 ? 1 : bmi < 30 ? 2 : bmi < 40 ? 3 : 4;
        for (int i = 0; i < labels.length; i++) {
            TextView cell = chu(labels[i], 10, i == active ? Color.WHITE : Color.BLACK, true);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(2), dp(11), dp(2), dp(11));
            GradientDrawable bg = new GradientDrawable(); bg.setColor(colors[i]);
            bg.setCornerRadii(i == 0 ? new float[]{dp(12),dp(12),0,0,0,0,dp(12),dp(12)} : i == 4 ? new float[]{0,0,dp(12),dp(12),dp(12),dp(12),0,0} : new float[8]);
            if (i == active) bg.setStroke(dp(2), Color.WHITE);
            cell.setBackground(bg);
            wrap.addView(cell, new LinearLayout.LayoutParams(0, dp(66), 1));
        }
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(16);
        return wrap;
    }

    private View taoDongLichSuChiSo(Map<String, Object> item) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(11), dp(10), dp(11));
        row.setBackground(taoNenGradient(new int[]{0xFF382326, 0xFF211A1D}, 17, 0xFF75434A));
        row.setElevation(dp(2));
        TextView icon = chu("▦", 23, 0xFFFF5A60, true);
        row.addView(icon, new LinearLayout.LayoutParams(dp(38), -2));
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
        info.addView(chu(KhoDuLieu.chuoi(item.get("ngay_do")), 14, TRANG, true));
        String detail = "Cân nặng: " + decimal(item.get("can_nang")) + " kg   ·   Chiều cao: " + decimal(item.get("chieu_cao")) + " cm\nBMI: " + decimal(item.get("bmi")) + "   ·   Tỷ lệ mỡ: " + (doubleValue(item.get("ti_le_mo")) > 0 ? decimal(item.get("ti_le_mo")) + "%" : "—");
        TextView values = chu(detail, 10, 0xFFD4CDD0, false); values.setPadding(0, dp(4), 0, 0);
        info.addView(values);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(chu("›", 25, XAM, false));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(8);
        row.setLayoutParams(p);
        return row;
    }

    private void hienTatCaLichSuChiSo(List<Map<String, Object>> items) {
        taoTrang("LỊCH SỬ ĐO", "Các lần cập nhật chỉ số cơ thể", this::hienChiSoCoThe, false);
        for (Map<String, Object> item : items) thanTrang.addView(taoDongLichSuChiSo(item));
    }

    private class BieuDoChiSoView extends View {
        private final List<Map<String, Object>> data;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BieuDoChiSoView(List<Map<String, Object>> values) { super(MainActivity.this); data = values; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float left = dp(26), right = getWidth() - dp(24), top = dp(12), bottom = getHeight() - dp(28);
            paint.setStrokeWidth(dp(1)); paint.setColor(0x44FFFFFF);
            for (int i = 0; i < 4; i++) { float y = top + (bottom-top)*i/3f; canvas.drawLine(left,y,right,y,paint); }
            if (data.isEmpty()) return;
            double minW = Double.MAX_VALUE, maxW = -Double.MAX_VALUE;
            for (Map<String,Object> item : data) { double w = doubleValue(item.get("can_nang")); minW=Math.min(minW,w); maxW=Math.max(maxW,w); }
            if (maxW-minW < 1) { minW -= 1; maxW += 1; } else { minW -= 1; maxW += 1; }
            PathHolder weightPath = new PathHolder();
            for (int i=0;i<data.size();i++) {
                float x = left + (right-left)*(data.size()==1?0.5f:(float)i/(data.size()-1));
                float y = bottom - (float)((doubleValue(data.get(i).get("can_nang"))-minW)/(maxW-minW))*(bottom-top);
                if (i==0) weightPath.path.moveTo(x,y); else weightPath.path.lineTo(x,y);
            }
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(3)); paint.setColor(0xFFFF454D); paint.setStrokeCap(Paint.Cap.ROUND); canvas.drawPath(weightPath.path,paint);
            android.graphics.Path fatPath = new android.graphics.Path();
            boolean hasFat = false;
            for (int i=0;i<data.size();i++) {
                double fat = doubleValue(data.get(i).get("ti_le_mo"));
                if (fat <= 0) continue;
                float x = left + (right-left)*(data.size()==1?0.5f:(float)i/(data.size()-1));
                float y = bottom - (float)((fat-5.0)/30.0)*(bottom-top);
                if (!hasFat) { fatPath.moveTo(x,y); hasFat = true; } else fatPath.lineTo(x,y);
            }
            if (hasFat) {
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(3)); paint.setColor(0xFFBEB8BC); canvas.drawPath(fatPath, paint);
            }
            for (int i=0;i<data.size();i++) {
                float x=left+(right-left)*(data.size()==1?0.5f:(float)i/(data.size()-1));
                float y=bottom-(float)((doubleValue(data.get(i).get("can_nang"))-minW)/(maxW-minW))*(bottom-top);
                paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE); canvas.drawCircle(x,y,dp(4),paint);
                paint.setColor(0xFFFF454D); canvas.drawCircle(x,y,dp(3),paint);
                double fat = doubleValue(data.get(i).get("ti_le_mo"));
                if (fat > 0) {
                    float fy = bottom - (float)((fat-5.0)/30.0)*(bottom-top);
                    paint.setColor(Color.WHITE); canvas.drawCircle(x,fy,dp(4),paint);
                    paint.setColor(0xFFBEB8BC); canvas.drawCircle(x,fy,dp(3),paint);
                }
                paint.setColor(0xFFBDB6B9); paint.setTextSize(dp(9)); paint.setTextAlign(Paint.Align.CENTER);
                String date=KhoDuLieu.chuoi(data.get(i).get("ngay_do")); if(date.length()>=7) date=date.substring(5,7)+"/"+date.substring(2,4);
                canvas.drawText(date,x,bottom+dp(17),paint);
            }
        }
        class PathHolder { android.graphics.Path path = new android.graphics.Path(); }
    }

    private void hienFormChiSo() {
        taoTrang("CẬP NHẬT CHỈ SỐ", "Các giá trị chỉ để theo dõi cá nhân, không thay thế tư vấn y tế", this::hienChiSoCoThe, false);
        EditText weight = truong("Cân nặng (kg)", "Ví dụ: 72.5", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, "");
        EditText height = truong("Chiều cao (cm)", "Ví dụ: 175", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, "");
        EditText fat = truong("Tỷ lệ mỡ cơ thể (%) - tùy chọn", "Ví dụ: 18", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, "");
        EditText date = truong("Ngày đo (YYYY-MM-DD)", ngayHomNay(), InputType.TYPE_CLASS_TEXT, ngayHomNay());
        nut("LƯU CHỈ SỐ", true, () -> {
            double w, h; double f = 0;
            try { w = Double.parseDouble(weight.getText().toString().trim()); h = Double.parseDouble(height.getText().toString().trim()); }
            catch (Exception e) { baoLoi("Cân nặng và chiều cao phải là số hợp lệ."); return; }
            if (fat.getText().length() > 0) { try { f = Double.parseDouble(fat.getText().toString().trim()); } catch (Exception e) { baoLoi("Tỷ lệ mỡ không hợp lệ."); return; } }
            String d = date.getText().toString().trim();
            if (w <= 0 || w > 500) { weight.setError("Cân nặng không hợp lệ"); return; }
            if (h < 50 || h > 260) { height.setError("Chiều cao không hợp lệ"); return; }
            if (fat.getText().length() > 0 && (f < 0 || f > 75)) { fat.setError("Tỷ lệ mỡ cần từ 0 đến 75%"); return; }
            if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) { date.setError("Dùng định dạng YYYY-MM-DD"); return; }
            double meters = h / 100.0;
            double bmi = w / (meters * meters);
            Map<String, Object> data = new HashMap<>();
            data.put("uid_nguoi_dung", uid);
            data.put("gym_id", gymId());
            data.put("can_nang", w);
            data.put("chieu_cao", h);
            data.put("bmi", Math.round(bmi * 10.0) / 10.0);
            data.put("ngay_do", d);
            data.put("ngay_tao", System.currentTimeMillis());
            if (fat.getText().length() > 0) data.put("ti_le_mo", f);
            kho.them("chi_so_co_the", data, new KhoDuLieu.KetQua<String>() {
                @Override public void thanhCong(String id) { thongBao("Đã lưu chỉ số cơ thể."); hienChiSoCoThe(); }
                @Override public void thatBai(String error) { baoLoi(error); }
            });
        });
    }

    // ---------------------------------------------------------------------
    // F07 - TIẾN ĐỘ VÀ THỐNG KÊ
    // ---------------------------------------------------------------------

    private void hienTienDo() {
        taoTrang("TIẾN ĐỘ TẬP LUYỆN", tenGymHienTai, this::hienBangDieuKhien, true);
        themChu("THEO DÕI HÀNH TRÌNH CỦA BẠN", 11, 0xFFAAAAAA, true);

        FrameLayout banner = new FrameLayout(this);
        banner.setBackground(nenVien(0xFF171719, 0xFF383838, 16));
        banner.setClipToOutline(true);
        ImageView bannerImage = new ImageView(this);
        bannerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int bannerResource = getResources().getIdentifier("progress_training_banner", "drawable", getPackageName());
        if (bannerResource != 0) bannerImage.setImageResource(bannerResource);
        banner.addView(bannerImage, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout bannerText = new LinearLayout(this);
        bannerText.setOrientation(LinearLayout.VERTICAL);
        bannerText.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        bannerText.setPadding(dp(12), dp(10), dp(18), dp(10));
        TextView bannerTitle = chu("VƯỢT GIỚI HẠN", 17, TRANG, true);
        bannerTitle.setGravity(Gravity.END);
        TextView bannerSubtitle = chu("MẠNH MẼ HƠN MỖI NGÀY", 10, 0xFFFF7772, true);
        bannerSubtitle.setGravity(Gravity.END);
        bannerText.addView(bannerTitle);
        bannerText.addView(bannerSubtitle);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(-1, -1, Gravity.END | Gravity.CENTER_VERTICAL);
        banner.addView(bannerText, textParams);
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, dp(142));
        bannerParams.bottomMargin = dp(15);
        thanTrang.addView(banner, bannerParams);

        LinearLayout period = new LinearLayout(this);
        period.setGravity(Gravity.CENTER);
        period.setPadding(dp(5), dp(5), dp(5), dp(5));
        period.setBackground(nenVien(0xFF292929, 0xFF383838, 24));
        TextView periodLabel = chu("30 NGÀY GẦN ĐÂY     ·     CẬP NHẬT THEO DỮ LIỆU THỰC", 11, 0xFFFF7772, true);
        periodLabel.setGravity(Gravity.CENTER);
        period.addView(periodLabel, new LinearLayout.LayoutParams(-1, dp(34)));
        LinearLayout.LayoutParams periodParams = new LinearLayout.LayoutParams(-1, -2);
        periodParams.bottomMargin = dp(14);
        thanTrang.addView(period, periodParams);

        nut("＋  GHI NHẬN BUỔI TẬP", true, this::hienFormTienDo);

        kho.layTatCa("lich_tap", new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> all) {
                List<Map<String, Object>> completed = new ArrayList<>();
                if (all != null) for (Map<String, Object> item : all) {
                    String assignedUid = KhoDuLieu.chuoi(item.get("uid_hoi_vien"));
                    if (KhoDuLieu.booleanValue(item.get("da_hoan_thanh"))
                            && (assignedUid.equals(uid) || assignedUid.isEmpty())) completed.add(item);
                }
                long totalMinutes = 0;
                for (Map<String, Object> item : completed) {
                    totalMinutes += laySo(item, "thoi_luong_phut");
                }

                LinearLayout statsRow1 = new LinearLayout(MainActivity.this);
                statsRow1.setOrientation(LinearLayout.HORIZONTAL);
                themTheThongKe(statsRow1, "▮▮", "BUỔI TẬP HOÀN THÀNH", String.valueOf(completed.size()), "buổi", 0xFFE94B45);
                themTheThongKe(statsRow1, "◷", "TỔNG THỜI GIAN", String.valueOf(totalMinutes / 60.0 == 0 ? 0 : Math.round(totalMinutes / 6.0) / 10.0), "giờ", 0xFF56C596);
                thanTrang.addView(statsRow1);

                themTheThongKe(thanTrang, "✓", "HÀNH TRÌNH TẬP LUYỆN", completed.size() + " buổi", "Tổng số buổi đã hoàn thành", 0xFFE94B45);

                kho.layTheoTruong("chi_so_co_the", "uid_nguoi_dung", uid, new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override public void thanhCong(List<Map<String, Object>> metrics) {
                        if (metrics == null) metrics = new ArrayList<>();
                        Collections.sort(metrics, (a, b) -> Long.compare(laySo(a, "ngay_tao"), laySo(b, "ngay_tao")));
                        if (!metrics.isEmpty()) {
                            Map<String, Object> latest = metrics.get(metrics.size() - 1);
                            String weight = decimal(latest.get("can_nang"));
                            String bmi = decimal(latest.get("bmi"));
                            LinearLayout bodyCard = taoThe(thanTrang, "CHỈ SỐ CƠ THỂ GẦN NHẤT", "");
                            LinearLayout bodyRow = new LinearLayout(MainActivity.this);
                            bodyRow.setGravity(Gravity.CENTER_VERTICAL);
                            TextView weightValue = chu(weight + " kg", 25, TRANG, true);
                            TextView bmiValue = chu("BMI  " + bmi, 17, 0xFFFF7772, true);
                            bodyRow.addView(weightValue, new LinearLayout.LayoutParams(0, -2, 1));
                            bodyRow.addView(bmiValue);
                            bodyCard.addView(bodyRow);
                            themChu("Cập nhật: " + KhoDuLieu.chuoi(latest.get("ngay_do")), 11, XAM, false);
                            if (metrics.size() > 1) {
                                Map<String, Object> first = metrics.get(0);
                                double delta = doubleValue(latest.get("can_nang")) - doubleValue(first.get("can_nang"));
                                String change = String.format(Locale.getDefault(), "%+.1f kg so với lần đo đầu", delta);
                                themChu(change, 12, delta <= 0 ? 0xFF56C596 : 0xFFFF7772, true);
                            }
                            LinearLayout chartCard = taoThe(thanTrang, "BIỂU ĐỒ CÂN NẶNG", "Diễn biến theo các lần cập nhật chỉ số");
                            if (metrics.size() >= 2) {
                                for (int i = Math.max(0, metrics.size() - 5); i < metrics.size(); i++) {
                                    Map<String, Object> point = metrics.get(i);
                                    LinearLayout line = new LinearLayout(MainActivity.this);
                                    line.setGravity(Gravity.CENTER_VERTICAL);
                                    TextView date = chu(KhoDuLieu.chuoi(point.get("ngay_do")), 11, XAM, false);
                                    TextView value = chu(decimal(point.get("can_nang")) + " kg", 12, TRANG, true);
                                    line.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
                                    line.addView(value);
                                    chartCard.addView(line);
                                    ProgressBar bar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleHorizontal);
                                    bar.setMax(150);
                                    bar.setProgress((int) Math.max(1, Math.min(150, doubleValue(point.get("can_nang")) * 2)));
                                    bar.setProgressTintList(android.content.res.ColorStateList.valueOf(DO));
                                    bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF414141));
                                    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(7));
                                    bp.topMargin = dp(5); bp.bottomMargin = dp(9);
                                    chartCard.addView(bar, bp);
                                }
                            } else {
                                themChu("Cần thêm ít nhất 2 lần đo để hiển thị xu hướng cân nặng.", 12, XAM, false);
                            }
                        } else {
                            the("CHỈ SỐ CƠ THỂ", "Bạn chưa cập nhật cân nặng. Hãy thêm chỉ số cơ thể để theo dõi sự thay đổi.");
                        }

                        themChu("NHẬT KÝ TẬP LUYỆN", 15, TRANG, true);
                        if (completed.isEmpty()) {
                            the("Chưa có buổi tập hoàn thành", "Hoàn thành buổi tập trong mục Lịch tập để lịch sử được ghi nhận tại đây.");
                            return;
                        }
                        Collections.sort(completed, (a, b) -> {
                            int c = KhoDuLieu.chuoi(b.get("ngay")).compareTo(KhoDuLieu.chuoi(a.get("ngay")));
                            return c != 0 ? c : Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao"));
                        });
                        for (Map<String, Object> session : completed) {
                            String name = KhoDuLieu.chuoi(session.get("ten_lich"));
                            if (name.isEmpty()) name = KhoDuLieu.chuoi(session.get("ten_buoi_tap"));
                            String date = KhoDuLieu.chuoi(session.get("ngay"));
                            String description = "📅 " + (date.isEmpty() ? "Chưa rõ ngày" : date)
                                    + "     ·     ⏰ " + KhoDuLieu.chuoi(session.get("gio"))
                                    + "\n" + KhoDuLieu.chuoi(session.get("mo_ta"));
                            LinearLayout historyCard = the("✓  " + (name.isEmpty() ? "Buổi tập" : name), description);
                            final String detailName = name;
                            final String detailDate = date;
                            final Map<String, Object> detailSession = session;
                            themNutVaoThe(historyCard, "XEM CHI TIẾT", false, () -> new AlertDialog.Builder(MainActivity.this)
                                    .setTitle(detailName.isEmpty() ? "Chi tiết buổi tập" : detailName)
                                    .setMessage("Ngày tập: " + detailDate + "\nGiờ: " + KhoDuLieu.chuoi(detailSession.get("gio"))
                                            + "\n\nNội dung:\n" + KhoDuLieu.chuoi(detailSession.get("mo_ta"))
                                            + (KhoDuLieu.chuoi(detailSession.get("huong_dan_an_uong")).isEmpty() ? "" : "\n\nDinh dưỡng:\n" + KhoDuLieu.chuoi(detailSession.get("huong_dan_an_uong"))))
                                    .setPositiveButton("Đóng", null).show());
                        }
                    }
                    @Override public void thatBai(String error) { baoLoi("Không tải được chỉ số cơ thể: " + error); }
                });
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được lịch sử tập: " + error); }
        });
    }

    private void themTheThongKe(LinearLayout parent, String icon, String label, String value, String unit, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(13), dp(13), dp(13));
        card.setBackground(nenVien(0xFF202024, 0xFF34343A, 17));
        TextView heading = chu(icon + "  " + label, 10, 0xFFB8B8BE, true);
        TextView number = chu(value, 24, TRANG, true);
        TextView caption = chu(unit, 11, accent, true);
        card.addView(heading);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
        vp.topMargin = dp(9);
        card.addView(number, vp);
        card.addView(caption);
        boolean horizontal = parent.getOrientation() == LinearLayout.HORIZONTAL;
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(horizontal ? 0 : -1, -2, horizontal ? 1 : 0);
        cp.setMargins(dp(4), dp(4), dp(4), dp(10));
        parent.addView(card, cp);
    }

    private void hienFormTienDo() {
        taoTrang("GHI NHẬN BUỔI TẬP", "Lưu lịch sử tập luyện cá nhân", this::hienTienDo, false);
        EditText name = truong("Tên buổi tập", "Ví dụ: Tập chân", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, "");
        EditText duration = truong("Thời lượng (phút)", "45", InputType.TYPE_CLASS_NUMBER, "45");
        EditText date = truong("Ngày tập (YYYY-MM-DD)", ngayHomNay(), InputType.TYPE_CLASS_TEXT, ngayHomNay());
        EditText note = truong("Ghi chú", "Cảm nhận hoặc nội dung đã tập", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, "");
        note.setSingleLine(false);
        note.setMinHeight(dp(80));
        nut("LƯU BUỔI TẬP", true, () -> {
            String n = name.getText().toString().trim(), d = date.getText().toString().trim();
            long mins;
            try { mins = Long.parseLong(duration.getText().toString().trim()); }
            catch (Exception e) { baoLoi("Thời lượng phải là số."); return; }
            if (n.isEmpty()) { name.setError("Nhập tên buổi tập"); return; }
            if (mins < 1 || mins > 600) { duration.setError("Nhập thời lượng từ 1 đến 600 phút"); return; }
            if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) { date.setError("Dùng định dạng YYYY-MM-DD"); return; }
            Map<String, Object> data = new HashMap<>();
            data.put("uid_nguoi_dung", uid);
            data.put("gym_id", gymId());
            data.put("ten_buoi_tap", n);
            data.put("thoi_luong_phut", mins);
            data.put("ngay", d);
            data.put("ghi_chu", note.getText().toString().trim());
            data.put("ngay_tao", System.currentTimeMillis());
            kho.them("tien_do", data, new KhoDuLieu.KetQua<String>() {
                @Override public void thanhCong(String id) { thongBao("Đã lưu tiến độ."); hienTienDo(); }
                @Override public void thatBai(String error) { baoLoi(error); }
            });
        });
    }

    // ---------------------------------------------------------------------
    // F08 - THÔNG BÁO
    // ---------------------------------------------------------------------

    private void hienThongBao() {
        taoTrang("THÔNG BÁO", laQuanTri() ? "QUẢN LÝ PHÒNG TẬP" : tenGymHienTai, this::hienBangDieuKhien, true);
        if (laQuanTri()) {
            LinearLayout send = new LinearLayout(this);
            send.setGravity(Gravity.CENTER);
            send.setOrientation(LinearLayout.HORIZONTAL);
            send.setPadding(dp(16), dp(17), dp(16), dp(17));
            GradientDrawable sendBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFFFF171D, 0xFFCF0712, 0xFF9E0710});
            sendBg.setCornerRadius(dp(34));
            sendBg.setStroke(dp(2), 0xFFFF424B);
            send.setBackground(sendBg);
            send.setElevation(dp(8));
            TextView sendText = chu("✉   + GỬI THÔNG BÁO ĐẾN HỘI VIÊN   ›", 16, TRANG, true);
            sendText.setGravity(Gravity.CENTER);
            send.addView(sendText);
            LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(-1, dp(62));
            sendParams.bottomMargin = dp(26);
            thanTrang.addView(send, sendParams);
            send.setOnClickListener(v -> hienFormThongBao());

        }

        // Bộ lọc dùng chung cho cả hội viên và chủ phòng tập.
        {
            LinearLayout filterLabel = new LinearLayout(this);
            filterLabel.setGravity(Gravity.CENTER_VERTICAL);
            TextView bookmark = chu("▱", 23, 0xFFFF2635, true);
            filterLabel.addView(bookmark, new LinearLayout.LayoutParams(dp(42), -2));
            filterLabel.addView(chu("Loại thông báo", 15, 0xFFD2D0D8, false));
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2);
            flp.topMargin = dp(8);
            flp.bottomMargin = dp(8);
            thanTrang.addView(filterLabel, flp);

            LinearLayout filters = new LinearLayout(this);
            filters.setOrientation(LinearLayout.HORIZONTAL);
            filters.setPadding(dp(4), dp(4), dp(4), dp(4));
            filters.setBackground(nenVien(0xFF09090B, 0xFF343238, 28));
            String[] names = {"Tất cả", "Ưu đãi", "Cập nhật", "Khác"};
            String[] icons = {"▦", "♟", "▤", "●"};
            for (int i = 0; i < names.length; i++) {
                final String selected = names[i];
                boolean active = boLocThongBao.equals(selected);
                TextView chip = chu(icons[i] + "  " + names[i], 12, active ? TRANG : 0xFFB8B6C0, active);
                chip.setGravity(Gravity.CENTER);
                chip.setPadding(dp(5), dp(13), dp(5), dp(13));
                chip.setBackground(nenVien(active ? 0xFFE50918 : 0xFF17171B,
                        active ? 0xFFFF3643 : 0xFF29292F, 22));
                LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(0, dp(48), 1);
                chipParams.setMargins(dp(2), 0, dp(2), 0);
                filters.addView(chip, chipParams);
                chip.setOnClickListener(v -> {
                    boLocThongBao = selected;
                    hienThongBao();
                });
            }
            LinearLayout.LayoutParams filtersParams = new LinearLayout.LayoutParams(-1, -2);
            filtersParams.bottomMargin = dp(18);
            thanTrang.addView(filters, filtersParams);
        }
        kho.layThongBaoHienThi(gymId(), uid, laQuanTri(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> all) {
                if (all == null) all = new ArrayList<>();
                Collections.sort(all, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
                int count = 0;
                for (Map<String, Object> item : all) {
                    String receiver = KhoDuLieu.chuoi(item.get("uid_nhan"));
                    if (!laQuanTri() && !receiver.isEmpty() && !receiver.equals(uid)) continue;
                    if (!phuHopBoLocThongBao(item)) continue;
                    count++;
                    boolean read = daDoc(item);
                    LinearLayout card = the("🔔  " + KhoDuLieu.chuoi(item.get("tieu_de")),
                            KhoDuLieu.chuoi(item.get("noi_dung")) + "\n\n" + ngay(laySo(item, "ngay_tao"))
                            + (laQuanTri() ? "" : (read ? "\n✓ Đã đọc" : "\n● Chưa đọc")));
                    card.setBackground(taoNenGradient(new int[]{0xFF25191D, 0xFF151417}, 18, read ? 0xFF49343A : 0xFF9E303A));
                    card.setElevation(dp(3));

                    if (!laQuanTri()) {
                        if (!read) themNutVaoThe(card, "ĐÁNH DẤU ĐÃ ĐỌC", true, () -> danhDauDaDoc(item));
                        themNutVaoThe(card, "XEM CHI TIẾT", false, () -> hienChiTietThongBao(item));
                    } else {
                        if ("yeu_cau_goi".equals(KhoDuLieu.chuoi(item.get("loai_thong_bao")))) {
                            themNutVaoThe(card, "MỞ YÊU CẦU · XÁC NHẬN ĐĂNG KÝ  ›", true,
                                    () -> moYeuCauGoiTuThongBao(item));
                        }
                        // Chủ gym có quyền xem chi tiết, sửa hoặc xóa thông báo
                        themNutVaoThe(card, "XEM CHI TIẾT", false, () -> hienChiTietThongBao(item));
                        themNutVaoThe(card, "✏️ CẬP NHẬT THÔNG BÁO", true, () -> hienFormSuaThongBao(item));
                        themNutVaoThe(card, "🗑️ XÓA THÔNG BÁO", false, () -> new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Xóa thông báo?")
                                .setMessage("Bạn có chắc chắn muốn xóa thông báo này không?")
                                .setNegativeButton("Hủy", null)
                                .setPositiveButton("Xóa", (d, w) -> kho.xoa("thong_bao", KhoDuLieu.chuoi(item.get("id")), new KhoDuLieu.KetQua<Boolean>() {
                                    @Override public void thanhCong(Boolean ok) {
                                        thongBao("Đã xóa thông báo.");
                                        hienThongBao();
                                    }
                                    @Override public void thatBai(String error) { baoLoi(error); }
                                })).show());
                    }
                }
                if (count == 0) {
                    if (laQuanTri()) {
                        LinearLayout emptyHeading = new LinearLayout(MainActivity.this);
                        emptyHeading.setGravity(Gravity.CENTER_VERTICAL);
                        emptyHeading.addView(chu("▣", 26, 0xFFFF2635, true), new LinearLayout.LayoutParams(dp(42), -2));
                        emptyHeading.addView(chu("Chưa có thông báo nào được gửi", 18, TRANG, true), new LinearLayout.LayoutParams(0, -2, 1));
                        thanTrang.addView(emptyHeading, new LinearLayout.LayoutParams(-1, -2));
                        View divider = new View(MainActivity.this);
                        divider.setBackgroundColor(0xFF555055);
                        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
                        dividerParams.topMargin = dp(12);
                        dividerParams.bottomMargin = dp(12);
                        thanTrang.addView(divider, dividerParams);
                        themChu("Bấm + GỬI THÔNG BÁO ĐẾN HỘI VIÊN ở trên để tạo thông báo mới.", 14, 0xFFB9B8C2, false);
                        LinearLayout empty = new LinearLayout(MainActivity.this);
                        empty.setOrientation(LinearLayout.VERTICAL);
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(dp(12), dp(32), dp(12), dp(34));
                        TextView bell = chu("♟", 64, 0xFFFF2635, true);
                        bell.setGravity(Gravity.CENTER);
                        GradientDrawable glow = new GradientDrawable();
                        glow.setShape(GradientDrawable.OVAL);
                        glow.setColor(0x221F1F25);
                        bell.setBackground(glow);
                        empty.addView(bell, new LinearLayout.LayoutParams(dp(142), dp(142)));
                        TextView title = chu(boLocThongBao.equals("Tất cả") ? "Không có thông báo nào" : "Không tìm thấy thông báo", 19, TRANG, true);
                        title.setGravity(Gravity.CENTER);
                        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(-1, -2);
                        titleP.topMargin = dp(12);
                        empty.addView(title, titleP);
                        TextView desc = chu("Khi có thông báo mới, chúng sẽ xuất hiện ở đây để bạn theo dõi.", 15, 0xFFB9B8C2, false);
                        desc.setGravity(Gravity.CENTER);
                        desc.setLineSpacing(dp(4), 1f);
                        LinearLayout.LayoutParams descP = new LinearLayout.LayoutParams(-1, -2);
                        descP.topMargin = dp(10);
                        descP.leftMargin = dp(18);
                        descP.rightMargin = dp(18);
                        empty.addView(desc, descP);
                        thanTrang.addView(empty, new LinearLayout.LayoutParams(-1, -2));
                    } else {
                        LinearLayout empty = new LinearLayout(MainActivity.this);
                        empty.setOrientation(LinearLayout.VERTICAL);
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(dp(14), dp(42), dp(14), dp(50));
                        FrameLayout illustration = new FrameLayout(MainActivity.this);
                        GradientDrawable halo = new GradientDrawable();
                        halo.setShape(GradientDrawable.OVAL);
                        halo.setColor(0xFF171719);
                        halo.setStroke(dp(1), 0xFF342126);
                        illustration.setBackground(halo);
                        TextView bell = chu("♟", 66, 0xFFFF2635, true);
                        bell.setGravity(Gravity.CENTER);
                        illustration.addView(bell, new FrameLayout.LayoutParams(-1, -1));
                        empty.addView(illustration, new LinearLayout.LayoutParams(dp(142), dp(142)));
                        TextView title = chu(boLocThongBao.equals("Tất cả") ? "Không có thông báo nào" : "Không tìm thấy thông báo", 19, TRANG, true);
                        title.setGravity(Gravity.CENTER);
                        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(-1, -2);
                        titleP.topMargin = dp(15);
                        empty.addView(title, titleP);
                        TextView desc = chu("Khi có thông báo mới từ phòng tập, chúng sẽ xuất hiện ở đây để bạn tiện theo dõi.", 14, 0xFFB9B8C2, false);
                        desc.setGravity(Gravity.CENTER);
                        desc.setLineSpacing(dp(4), 1f);
                        LinearLayout.LayoutParams descP = new LinearLayout.LayoutParams(-1, -2);
                        descP.topMargin = dp(9);
                        descP.leftMargin = dp(20); descP.rightMargin = dp(20);
                        empty.addView(desc, descP);
                        thanTrang.addView(empty, new LinearLayout.LayoutParams(-1, -2));
                    }
                }
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được thông báo: " + error); }
        });
    }

    private boolean phuHopBoLocThongBao(Map<String, Object> item) {
        if ("Tất cả".equals(boLocThongBao)) return true;
        String type = (KhoDuLieu.chuoi(item.get("loai")) + " "
                + KhoDuLieu.chuoi(item.get("phan_loai")) + " "
                + KhoDuLieu.chuoi(item.get("category")) + " "
                + KhoDuLieu.chuoi(item.get("tieu_de")) + " "
                + KhoDuLieu.chuoi(item.get("noi_dung"))).toLowerCase(Locale.ROOT);
        if ("Ưu đãi".equals(boLocThongBao)) return type.contains("ưu đãi") || type.contains("uu dai") || type.contains("khuyến mãi") || type.contains("giam giá") || type.contains("sale");
        if ("Cập nhật".equals(boLocThongBao)) return type.contains("cập nhật") || type.contains("cap nhat") || type.contains("lịch") || type.contains("bảo trì");
        return !(type.contains("ưu đãi") || type.contains("uu dai") || type.contains("khuyến mãi")
                || type.contains("giam giá") || type.contains("sale") || type.contains("cập nhật")
                || type.contains("cap nhat") || type.contains("lịch") || type.contains("bảo trì"));
    }

    private void hienChiTietThongBao(Map<String, Object> item) {
        String tieuDe = KhoDuLieu.chuoi(item.get("tieu_de"));
        String noiDung = KhoDuLieu.chuoi(item.get("noi_dung"));
        long thoiGian = laySo(item, "ngay_tao");

        taoTrang("CHI TIẾT THÔNG BÁO", tenGymHienTai, this::hienThongBao, false);

        LinearLayout card = the("📌 " + tieuDe, 
                "🏢 Thương hiệu: " + (TextUtils.isEmpty(tenGymHienTai) ? "Hệ thống GymManagement" : tenGymHienTai)
                + "\n🕒 Thời gian gửi: " + ngay(thoiGian)
                + "\n🏷️ Trạng thái: " + (daDoc(item) ? "Đã đọc" : "Chưa đọc")
                + "\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "📝 NỘI DUNG THÔNG BÁO CHI TIẾT:\n\n" + noiDung
                + "\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                + "💡 Lưu ý: Vui lòng tuân thủ các thông báo và quy định mới nhất từ Ban quản lý phòng tập.");

        if (!daDoc(item)) {
            nut("✨ ĐÁNH DẤU ĐÃ ĐỌC", true, () -> {
                danhDauDaDoc(item);
                thongBao("Đã đánh dấu thông báo là đã đọc.");
                hienChiTietThongBao(item);
            });
        }
        nut("⬅ QUAY LẠI DANH SÁCH THÔNG BÁO", false, this::hienThongBao);
    }

    private boolean daDoc(Map<String, Object> item) {
        Object obj = item.get("danh_sach_da_doc");
        if (obj instanceof List<?>) {
            for (Object value : (List<?>) obj) if (uid.equals(KhoDuLieu.chuoi(value))) return true;
        }
        return false;
    }

    private void danhDauDaDoc(Map<String, Object> item) {
        List<String> readBy = new ArrayList<>();
        Object old = item.get("danh_sach_da_doc");
        if (old instanceof List<?>) for (Object v : (List<?>) old) readBy.add(KhoDuLieu.chuoi(v));
        if (!readBy.contains(uid)) readBy.add(uid);
        Map<String, Object> patch = new HashMap<>();
        patch.put("danh_sach_da_doc", readBy);
        kho.capNhat("thong_bao", KhoDuLieu.chuoi(item.get("id")), patch, new KhoDuLieu.KetQua<Boolean>() {
            @Override public void thanhCong(Boolean ok) { hienThongBao(); }
            @Override public void thatBai(String error) { baoLoi(error); }
        });
    }

    private void hienFormThongBao() {
        hienFormSuaThongBao(null);
    }

    private void hienFormSuaThongBao(Map<String, Object> itemToEdit) {
        boolean isEdit = itemToEdit != null;
        taoTrang(isEdit ? "CẬP NHẬT THÔNG BÁO" : "GỬI THÔNG BÁO", isEdit ? "Chỉnh sửa nội dung thông báo" : "Thông báo sẽ chỉ gửi đến thành viên thuộc " + tenGymHienTai, this::hienThongBao, false);
        EditText title = truong("Tiêu đề", "Ví dụ: Lịch nghỉ lễ", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, isEdit ? KhoDuLieu.chuoi(itemToEdit.get("tieu_de")) : "");
        EditText content = truong("Nội dung", "Nhập nội dung thông báo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, isEdit ? KhoDuLieu.chuoi(itemToEdit.get("noi_dung")) : "");
        content.setSingleLine(false);
        content.setMinHeight(dp(120));
        
        nut(isEdit ? "CẬP NHẬT THÔNG BÁO" : "GỬI THÔNG BÁO", true, () -> {
            String t = title.getText().toString().trim(), c = content.getText().toString().trim();
            if (t.isEmpty()) { title.setError("Nhập tiêu đề"); return; }
            if (c.isEmpty()) { content.setError("Nhập nội dung"); return; }
            
            if (isEdit) {
                Map<String, Object> patch = new HashMap<>();
                patch.put("tieu_de", t);
                patch.put("noi_dung", c);
                kho.capNhat("thong_bao", KhoDuLieu.chuoi(itemToEdit.get("id")), patch, new KhoDuLieu.KetQua<Boolean>() {
                    @Override public void thanhCong(Boolean ok) {
                        thongBao("Đã cập nhật thông báo thành công.");
                        hienThongBao();
                    }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("gym_id", gymId());
                data.put("uid_nhan", "");
                data.put("tieu_de", t);
                data.put("noi_dung", c);
                data.put("nguoi_tao_uid", uid);
                data.put("ngay_tao", System.currentTimeMillis());
                data.put("danh_sach_da_doc", new ArrayList<String>());
                kho.them("thong_bao", data, new KhoDuLieu.KetQua<String>() {
                    @Override public void thanhCong(String id) { thongBao("Đã gửi thông báo đến thương hiệu " + tenGymHienTai + "."); hienThongBao(); }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            }
        });
        nut("HỦY", false, this::hienThongBao);
    }

    // ---------------------------------------------------------------------
    // F09 - QUẢN TRỊ GYM: HỘI VIÊN, YÊU CẦU GÓI, ĐIỂM DANH, THƯƠNG HIỆU
    // ---------------------------------------------------------------------

    private void hienHoiVien() {
        if (!laQuanTri()) { baoLoi("Chức năng này dành cho quản trị Gym."); return; }
        taoTrang("QUẢN LÝ HỘI VIÊN", tenGymHienTai, this::hienBangDieuKhien, true);

        LinearLayout topActions = new LinearLayout(this);
        topActions.setOrientation(LinearLayout.HORIZONTAL);
        Button requestButton = taoNutNho("✉  YÊU CẦU GÓI", true, this::hienYeuCauGoi);
        Button attendanceButton = taoNutNho("✓  ĐIỂM DANH", false, this::hienDiemDanh);
        requestButton.setTextSize(11); attendanceButton.setTextSize(11);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(43), 1);
        half.setMargins(0, 0, dp(6), 0); topActions.addView(requestButton, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(43), 1);
        half2.setMargins(dp(6), 0, 0, 0); topActions.addView(attendanceButton, half2);
        thanTrang.addView(topActions);

        TextView totalLabel = chu("TỔNG SỐ: ĐANG TẢI HỘI VIÊN", 12, 0xFFE9E9E9, true);
        totalLabel.setPadding(dp(10), dp(7), dp(10), dp(7));
        totalLabel.setBackground(nenVien(0xFF292929, 0xFF484848, 9));
        LinearLayout.LayoutParams totalP = new LinearLayout.LayoutParams(-2, -2);
        totalP.topMargin = dp(8); totalP.bottomMargin = dp(13); thanTrang.addView(totalLabel, totalP);

        EditText search = oNhap("⌕  Tìm tên hoặc email hội viên...",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, "");
        search.setSingleLine(true);

        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filterScroll.addView(filters);
        thanTrang.addView(filterScroll);

        LinearLayout listHost = new LinearLayout(this);
        listHost.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listP = new LinearLayout.LayoutParams(-1, -2);
        listP.topMargin = dp(15); thanTrang.addView(listHost, listP);

        final String[] selectedFilter = {"Tất cả"};
        final List<Map<String, Object>> loadedMembers = new ArrayList<>();
        Runnable renderList = () -> {
            listHost.removeAllViews();
            String query = search.getText().toString().trim().toLowerCase(new Locale("vi", "VN"));
            int visible = 0;
            long now = System.currentTimeMillis();
            for (Map<String, Object> member : loadedMembers) {
                String name = KhoDuLieu.chuoi(member.get("ho_ten"));
                String email = KhoDuLieu.chuoi(member.get("email"));
                String phone = KhoDuLieu.chuoi(member.get("so_dien_thoai"));
                String packageName = KhoDuLieu.chuoi(member.get("_ten_goi"));
                long expiry = laySo(member, "_ngay_het_han");
                boolean locked = Boolean.FALSE.equals(member.get("trang_thai"));
                boolean expired = expiry > 0 && expiry <= now;
                boolean expiring = expiry > now && expiry - now <= 7L * 86400000L;
                String category = locked ? "Đã khóa" : (expiry <= 0 ? "Chưa có gói" : (expired ? "Hết hạn" : (expiring ? "Sắp hết hạn" : "Đang hoạt động")));
                boolean matchQuery = (name + " " + email + " " + phone).toLowerCase(new Locale("vi", "VN")).contains(query);
                boolean matchFilter = "Tất cả".equals(selectedFilter[0])
                        || ("Đang hoạt động".equals(selectedFilter[0]) && "Đang hoạt động".equals(category))
                        || ("Sắp hết hạn".equals(selectedFilter[0]) && "Sắp hết hạn".equals(category))
                        || ("Hết hạn".equals(selectedFilter[0]) && "Hết hạn".equals(category));
                if (!matchQuery || !matchFilter) continue;
                visible++;

                LinearLayout card = new LinearLayout(MainActivity.this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setPadding(dp(13), dp(13), dp(10), dp(13));
                GradientDrawable cardBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF1E1E20, 0xFF151516, 0xFF191719});
                cardBg.setCornerRadius(dp(16)); cardBg.setStroke(dp(1), 0xFF555052);
                card.setBackground(cardBg);
                LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(-1, -2);
                cardP.bottomMargin = dp(10); listHost.addView(card, cardP);

                ImageView avatar = new ImageView(MainActivity.this);
                avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                avatar.setBackground(nenVien(0xFF30343A, 0xFF777777, 50));
                avatar.setClipToOutline(true);
                String avatarUrl = KhoDuLieu.chuoi(member.get("anh_dai_dien"));
                if (!TextUtils.isEmpty(avatarUrl)) ImageLoader.load(avatar, avatarUrl, R.drawable.bg_circle_surface);
                else {
                    TextView fallback = chu(layChuVietTat(name), 16, TRANG, true);
                    fallback.setGravity(Gravity.CENTER);
                    fallback.setBackground(nenVien(0xFF4A252B, 0xFF9A414B, 50));
                    card.addView(fallback, new LinearLayout.LayoutParams(dp(48), dp(48)));
                }
                if (!TextUtils.isEmpty(avatarUrl)) card.addView(avatar, new LinearLayout.LayoutParams(dp(48), dp(48)));

                LinearLayout info = new LinearLayout(MainActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(dp(11), 0, dp(4), 0);
                TextView nameView = chu(name.isEmpty() ? "Hội viên" : name, 15, TRANG, true);
                info.addView(nameView);
                int statusColor = "Đang hoạt động".equals(category) ? 0xFF69D68D
                        : ("Sắp hết hạn".equals(category) ? 0xFFFFC45C : 0xFFFF7777);
                TextView statusView = chu("● " + category, 10, statusColor, true);
                statusView.setPadding(dp(7), dp(3), dp(7), dp(3));
                statusView.setBackground(nenVien(0x33222222, 0x33444444, 20));
                LinearLayout.LayoutParams statusP = new LinearLayout.LayoutParams(-2, -2);
                statusP.topMargin = dp(3); info.addView(statusView, statusP);
                TextView contact = chu((phone.isEmpty() ? "Chưa có SĐT" : phone) + " · " + email, 11, 0xFFBDBDBD, false);
                contact.setMaxLines(1); contact.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams contactP = new LinearLayout.LayoutParams(-1, -2);
                contactP.topMargin = dp(3); info.addView(contact, contactP);
                TextView pack = chu(packageName.isEmpty() ? "Chưa đăng ký gói tập" : packageName, 11, 0xFFE8E8E8, false);
                pack.setPadding(dp(7), dp(3), dp(7), dp(3));
                pack.setBackground(nenVien(0xFF303033, 0xFF444448, 9));
                LinearLayout.LayoutParams packP = new LinearLayout.LayoutParams(-2, -2);
                packP.topMargin = dp(5); info.addView(pack, packP);
                TextView expiryView = chu(expiry > 0 ? ((expired ? "Đã hết hạn: " : "Hết hạn: ") + ngay(expiry)) : "Chưa có ngày hết hạn", 11, 0xFFBDBDBD, false);
                LinearLayout.LayoutParams expiryP = new LinearLayout.LayoutParams(-1, -2);
                expiryP.topMargin = dp(3); info.addView(expiryView, expiryP);
                card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
                TextView arrow = chu("›", 29, 0xFFBDBDBD, false);
                card.addView(arrow, new LinearLayout.LayoutParams(dp(20), -2));
                card.setOnClickListener(v -> hienChiTietHoiVien(member, category, packageName, expiry));
            }
            if (visible == 0) {
                TextView empty = chu(query.isEmpty() ? "Không có hội viên trong nhóm này." : "Không tìm thấy hội viên phù hợp.", 13, XAM, false);
                empty.setGravity(Gravity.CENTER); empty.setPadding(dp(15), dp(30), dp(15), dp(30));
                listHost.addView(empty);
            }
        };

        String[] filterNames = {"Tất cả", "Đang hoạt động", "Sắp hết hạn", "Hết hạn"};
        for (String filter : filterNames) {
            Button chip = new Button(this);
            chip.setText(filter); chip.setAllCaps(false); chip.setTextSize(12);
            chip.setTextColor(TRANG); chip.setMinHeight(dp(38)); chip.setPadding(dp(13), 0, dp(13), 0);
            chip.setBackground(nenVien("Tất cả".equals(filter) ? DO : 0xFF252528,
                    "Tất cả".equals(filter) ? 0xFFFF5C61 : 0xFF505055, 22));
            LinearLayout.LayoutParams chipP = new LinearLayout.LayoutParams(-2, dp(38));
            chipP.setMargins(0, 0, dp(8), 0); filters.addView(chip, chipP);
            chip.setOnClickListener(v -> {
                selectedFilter[0] = filter;
                for (int i = 0; i < filters.getChildCount(); i++) {
                    Button b = (Button) filters.getChildAt(i);
                    String label = b.getText().toString();
                    b.setBackground(nenVien(label.equals(filter) ? DO : 0xFF252528,
                            label.equals(filter) ? 0xFFFF5C61 : 0xFF505055, 22));
                }
                renderList.run();
            });
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderList.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        kho.layTheoTruong("nguoi_dung", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> gymUsers) {
                kho.layTheoTruong("dang_ky_goi", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override public void thanhCong(List<Map<String, Object>> registrations) {
                        Map<String, Map<String, Object>> memberPackageMap = new HashMap<>();
                        Set<String> memberUids = new HashSet<>();
                        if (gymUsers != null) {
                            for (Map<String, Object> u : gymUsers) {
                                if ("hoi_vien".equals(KhoDuLieu.chuoi(u.get("vai_tro")))) {
                                    memberUids.add(KhoDuLieu.chuoi(u.get("id")));
                                }
                            }
                        }
                        if (registrations != null) {
                            for (Map<String, Object> reg : registrations) {
                                String mUid = KhoDuLieu.chuoi(reg.get("uid_nguoi_dung"));
                                String st = KhoDuLieu.chuoi(reg.get("trang_thai"));
                                if (!mUid.isEmpty() && ("dang_hoat_dong".equals(st) || "het_han".equals(st))) {
                                    memberUids.add(mUid);
                                    Map<String, Object> previous = memberPackageMap.get(mUid);
                                    if (previous == null || laySo(reg, "ngay_ket_thuc") >= laySo(previous, "ngay_ket_thuc")) memberPackageMap.put(mUid, reg);
                                }
                            }
                        }

                        totalLabel.setText("TỔNG SỐ: " + memberUids.size() + " HỘI VIÊN");
                        if (memberUids.isEmpty()) {
                            renderList.run();
                            return;
                        }

                        final int[] completed = {0};
                        for (String mId : memberUids) {
                            kho.layMot("nguoi_dung", mId, new KhoDuLieu.KetQua<Map<String, Object>>() {
                                @Override public void thanhCong(Map<String, Object> member) {
                                    if (member != null) {
                                        Map<String, Object> item = new HashMap<>(member);
                                        item.put("_uid", mId);
                                        Map<String, Object> reg = memberPackageMap.get(mId);
                                        item.put("_ten_goi", reg == null ? "" : KhoDuLieu.chuoi(reg.get("ten_goi")));
                                        item.put("_ngay_het_han", reg == null ? 0L : laySo(reg, "ngay_ket_thuc"));
                                        loadedMembers.add(item);
                                    }
                                    completed[0]++;
                                    if (completed[0] == memberUids.size()) {
                                        loadedMembers.sort((a, b) -> KhoDuLieu.chuoi(a.get("ho_ten")).compareToIgnoreCase(KhoDuLieu.chuoi(b.get("ho_ten"))));
                                        renderList.run();
                                    }
                                }
                                @Override public void thatBai(String error) { completed[0]++; if (completed[0] == memberUids.size()) renderList.run(); }
                            });
                        }
                    }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được hội viên: " + error); }
        });
    }

    private void hienChiTietHoiVien(Map<String, Object> member, String category, String packageName, long expiry) {
        String memberId = KhoDuLieu.chuoi(member.get("_uid"));
        String name = KhoDuLieu.chuoi(member.get("ho_ten"));
        new AlertDialog.Builder(this)
                .setTitle(name.isEmpty() ? "Thông tin hội viên" : name)
                .setMessage("Email: " + KhoDuLieu.chuoi(member.get("email"))
                        + "\nSố điện thoại: " + KhoDuLieu.chuoi(member.get("so_dien_thoai"))
                        + "\nGói tập: " + (packageName.isEmpty() ? "Chưa đăng ký" : packageName)
                        + "\nTrạng thái: " + category
                        + "\nHạn sử dụng: " + (expiry > 0 ? ngay(expiry) : "Chưa cập nhật"))
                .setNeutralButton("Phân lịch tập", (d, w) -> hienFormLichTapChoThanhVien(memberId, name))
                .setNegativeButton(Boolean.FALSE.equals(member.get("trang_thai")) ? "Mở khóa" : "Khóa tài khoản", (d, w) -> {
                    Map<String, Object> patch = new HashMap<>();
                    patch.put("trang_thai", Boolean.FALSE.equals(member.get("trang_thai")));
                    kho.capNhat("nguoi_dung", memberId, patch, new KhoDuLieu.KetQua<Boolean>() {
                        @Override public void thanhCong(Boolean ok) { thongBao("Đã cập nhật trạng thái tài khoản."); hienHoiVien(); }
                        @Override public void thatBai(String error) { baoLoi(error); }
                    });
                })
                .setPositiveButton("Đóng", null).show();
    }

    private void hienFormLichTapChoThanhVien(String memberUid, String memberName) {
        selectedMemberUid = memberUid;
        selectedMemberName = memberName + " (Hội viên)";
        hienFormLichTap();
    }

    private String layChuVietTat(String name) {
        if (TextUtils.isEmpty(name)) return "HV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private void hienYeuCauGoi() {
        if (!laQuanTri()) { baoLoi("Chỉ quản trị Gym mới được xử lý yêu cầu."); return; }
        taoTrang("YÊU CẦU ĐĂNG KÝ GÓI", tenGymHienTai, this::hienHoiVien, true);
        kho.layTheoTruong("dang_ky_goi", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> all) {
                Collections.sort(all, (a, b) -> Long.compare(laySo(b, "ngay_dang_ky"), laySo(a, "ngay_dang_ky")));
                int pending = 0;
                for (Map<String, Object> item : all) {
                    String status = KhoDuLieu.chuoi(item.get("trang_thai"));
                    if (!"cho_xac_nhan".equals(status)) continue;
                    pending++;
                    LinearLayout card = the(KhoDuLieu.chuoi(item.get("ten_hoi_vien")), "Gói: " + KhoDuLieu.chuoi(item.get("ten_goi")) + "\nGiá: " + tien(laySo(item, "gia")) + "\nNgày yêu cầu: " + ngay(laySo(item, "ngay_dang_ky")));
                    themNutVaoThe(card, "XÁC NHẬN ĐĂNG KÝ", true, () -> capNhatYeuCauGoi(item, "dang_hoat_dong"));
                    themNutVaoThe(card, "TỪ CHỐI", false, () -> capNhatYeuCauGoi(item, "tu_choi"));
                }
                if (pending == 0) the("Không có yêu cầu đang chờ", "Yêu cầu mới của hội viên sẽ xuất hiện ở đây.");
            }
            @Override public void thatBai(String error) { baoLoi(error); }
        });
    }

    private void moYeuCauGoiTuThongBao(Map<String, Object> notice) {
        if (!laQuanTri()) { baoLoi("Chỉ chủ Gym mới được xác nhận đăng ký."); return; }
        String registrationId = KhoDuLieu.chuoi(notice.get("dang_ky_goi_id"));
        if (registrationId.isEmpty()) { hienYeuCauGoi(); return; }
        kho.layMot("dang_ky_goi", registrationId, new KhoDuLieu.KetQua<Map<String, Object>>() {
            @Override public void thanhCong(Map<String, Object> registration) {
                if (registration == null) { baoLoi("Không tìm thấy yêu cầu đăng ký này."); hienYeuCauGoi(); return; }
                if (!gymId().equals(KhoDuLieu.chuoi(registration.get("gym_id")))) {
                    baoLoi("Yêu cầu này không thuộc phòng Gym của bạn."); return;
                }
                if (!"cho_xac_nhan".equals(KhoDuLieu.chuoi(registration.get("trang_thai")))) {
                    thongBao("Yêu cầu này đã được xử lý trước đó."); hienYeuCauGoi(); return;
                }
                taoTrang("XÁC NHẬN ĐĂNG KÝ", "Kiểm tra thông tin trước khi duyệt", () -> hienThongBao(), false);
                LinearLayout detail = the("📩  YÊU CẦU TỪ HỘI VIÊN",
                        "Hội viên: " + KhoDuLieu.chuoi(registration.get("ten_hoi_vien"))
                                + "\nGói tập: " + KhoDuLieu.chuoi(registration.get("ten_goi"))
                                + "\nGiá: " + tien(laySo(registration, "gia"))
                                + "\nThời hạn: " + laySo(registration, "so_ngay") + " ngày"
                                + "\nNgày gửi: " + ngay(laySo(registration, "ngay_dang_ky"))
                                + "\n\nTrạng thái: ĐANG CHỜ XÁC NHẬN");
                detail.setBackground(taoNenGradient(new int[]{0xFF32171D, 0xFF171518}, 22, 0xFF9E303A));
                themNutVaoThe(detail, "✓  XÁC NHẬN ĐĂNG KÝ", true, () -> capNhatYeuCauGoi(registration, "dang_hoat_dong"));
                themNutVaoThe(detail, "✕  TỪ CHỐI YÊU CẦU", false, () -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Từ chối đăng ký?")
                        .setMessage("Hội viên sẽ nhận được thông báo từ chối yêu cầu này.")
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton("Từ chối", (d, w) -> capNhatYeuCauGoi(registration, "tu_choi")).show());
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được yêu cầu: " + error); }
        });
    }

    private void capNhatYeuCauGoi(Map<String, Object> item, String status) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("trang_thai", status);
        patch.put("nguoi_duyet_uid", uid);
        patch.put("ngay_duyet", System.currentTimeMillis());
        kho.capNhat("dang_ky_goi", KhoDuLieu.chuoi(item.get("id")), patch, new KhoDuLieu.KetQua<Boolean>() {
            @Override public void thanhCong(Boolean ok) {
                thongBao("Đã cập nhật yêu cầu.");
                Map<String, Object> notice = new HashMap<>();
                notice.put("gym_id", gymId());
                notice.put("uid_nhan", KhoDuLieu.chuoi(item.get("uid_nguoi_dung")));
                notice.put("tieu_de", "Cập nhật yêu cầu gói tập");
                notice.put("noi_dung", "Yêu cầu gói '" + KhoDuLieu.chuoi(item.get("ten_goi")) + "' đã được cập nhật: " + dichTrangThai(status) + ".");
                notice.put("nguoi_tao_uid", uid);
                notice.put("ngay_tao", System.currentTimeMillis());
                notice.put("danh_sach_da_doc", new ArrayList<String>());
                kho.them("thong_bao", notice, new KhoDuLieu.KetQua<String>() {
                    @Override public void thanhCong(String id) { hienYeuCauGoi(); }
                    @Override public void thatBai(String error) { hienYeuCauGoi(); }
                });
            }
            @Override public void thatBai(String error) { baoLoi(error); }
        });
    }

    private void hienDiemDanh() {
        if (!laQuanTri()) { baoLoi("Chỉ quản trị Gym mới được xem điểm danh."); return; }
        taoTrang("QUẢN LÝ ĐIỂM DANH", tenGymHienTai, this::hienBangDieuKhien, true);

        TextView dateTitle = chu(new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN")).format(new Date()), 19, TRANG, true);
        dateTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dateP = new LinearLayout.LayoutParams(-1, -2);
        dateP.topMargin = dp(4); dateP.bottomMargin = dp(15); thanTrang.addView(dateTitle, dateP);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        TextView totalStat = taoTheThongKeDiemDanh(stats, "LƯỢT ĐIỂM DANH", "—");
        TextView memberStat = taoTheThongKeDiemDanh(stats, "HỘI VIÊN ĐÃ ĐẾN", "—");
        TextView invalidStat = taoTheThongKeDiemDanh(stats, "KHÔNG HỢP LỆ", "0");
        LinearLayout.LayoutParams statsP = new LinearLayout.LayoutParams(-1, -2);
        statsP.bottomMargin = dp(14); thanTrang.addView(stats, statsP);

        Button manualInfo = taoNutNho("＋  GHI NHẬN ĐIỂM DANH HỘI VIÊN", true, () ->
                thongBao("Điểm danh hiện được ghi nhận từ luồng xác thực hội viên và hoàn thành buổi tập."));
        manualInfo.setTextSize(13);
        LinearLayout.LayoutParams actionP = new LinearLayout.LayoutParams(-1, dp(54));
        actionP.bottomMargin = dp(14); thanTrang.addView(manualInfo, actionP);

        EditText search = oNhap("⌕  Tìm tên, số điện thoại hoặc mã hội viên...", InputType.TYPE_CLASS_TEXT, "");
        // oNhap() tự thêm trường vào thanTrang; không addView lần thứ hai.

        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabsScroll.addView(tabs); thanTrang.addView(tabsScroll);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listP = new LinearLayout.LayoutParams(-1, -2);
        listP.topMargin = dp(9); thanTrang.addView(list, listP);
        final String[] selectedTab = {"Tất cả"};
        final List<Map<String, Object>> allRows = new ArrayList<>();
        Runnable renderRows = () -> {
            list.removeAllViews();
            String query = search.getText().toString().trim().toLowerCase(new Locale("vi", "VN"));
            int shown = 0;
            for (Map<String, Object> row : allRows) {
                String name = KhoDuLieu.chuoi(row.get("_member_name"));
                String phone = KhoDuLieu.chuoi(row.get("_phone"));
                String email = KhoDuLieu.chuoi(row.get("_email"));
                String content = KhoDuLieu.chuoi(row.get("noi_dung"));
                String searchable = (name + " " + phone + " " + email + " " + KhoDuLieu.chuoi(row.get("uid_nguoi_dung"))).toLowerCase(new Locale("vi", "VN"));
                if (!searchable.contains(query)) continue;
                if ("Lỗi/Từ chối".equals(selectedTab[0])) continue; // Firestore hiện chưa lưu bản ghi từ chối.
                shown++;
                LinearLayout item = new LinearLayout(MainActivity.this);
                item.setOrientation(LinearLayout.HORIZONTAL); item.setGravity(Gravity.CENTER_VERTICAL);
                item.setPadding(dp(12), dp(13), dp(10), dp(13));
                GradientDrawable itemBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF242326, 0xFF171719});
                itemBg.setCornerRadius(dp(15)); itemBg.setStroke(dp(1), 0xFF454247); item.setBackground(itemBg);
                LinearLayout.LayoutParams itemP = new LinearLayout.LayoutParams(-1, -2);
                itemP.bottomMargin = dp(9); list.addView(item, itemP);

                TextView avatar = chu(layChuVietTat(name), 15, TRANG, true);
                avatar.setGravity(Gravity.CENTER);
                avatar.setBackground(nenVien(0xFF3A292D, 0xFF8B424B, 40));
                item.addView(avatar, new LinearLayout.LayoutParams(dp(46), dp(46)));

                LinearLayout details = new LinearLayout(MainActivity.this);
                details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(11), 0, dp(4), 0);
                details.addView(chu(name.isEmpty() ? "Hội viên" : name, 14, TRANG, true));
                long time = laySo(row, "ngay_tao");
                String clock = time > 0 ? new SimpleDateFormat("HH:mm", new Locale("vi", "VN")).format(new Date(time)) : "--:--";
                details.addView(chu(clock + "   " + content, 11, 0xFFD0CDD0, false));
                TextView status = chu("✓  ĐÃ GHI NHẬN", 10, 0xFF71D99A, true);
                LinearLayout.LayoutParams statusP = new LinearLayout.LayoutParams(-2, -2);
                statusP.topMargin = dp(4); details.addView(status, statusP);
                item.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
                item.setOnClickListener(v -> hienChiTietDiemDanhHoiVien(name, Collections.singletonList(row)));
            }
            if (shown == 0) {
                TextView empty = chu("Lỗi/từ chối chưa có dữ liệu ghi nhận.\nCác lượt điểm danh hợp lệ sẽ xuất hiện tại đây.", 12, XAM, false);
                if (!"Lỗi/Từ chối".equals(selectedTab[0])) empty.setText("Không tìm thấy lượt điểm danh phù hợp.");
                empty.setGravity(Gravity.CENTER); empty.setPadding(dp(14), dp(26), dp(14), dp(26)); list.addView(empty);
            }
        };
        String[] tabNames = {"Tất cả", "Thành công", "Lỗi/Từ chối"};
        for (String tabName : tabNames) {
            TextView tab = chu(tabName, 12, "Tất cả".equals(tabName) ? 0xFFFF666B : 0xFFCBC8CC, true);
            tab.setGravity(Gravity.CENTER); tab.setPadding(dp(14), dp(10), dp(14), dp(10));
            tabs.addView(tab, new LinearLayout.LayoutParams(-2, dp(42)));
            tab.setOnClickListener(v -> {
                selectedTab[0] = tabName;
                for (int i = 0; i < tabs.getChildCount(); i++) {
                    TextView t = (TextView) tabs.getChildAt(i);
                    boolean active = t.getText().toString().equals(tabName);
                    t.setTextColor(active ? 0xFFFF666B : 0xFFCBC8CC);
                    t.setBackground(active ? nenVien(0xFF32191D, 0xFFB33D46, 7) : null);
                }
                renderRows.run();
            });
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderRows.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        kho.layTheoTruong("diem_danh", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> allCheckins) {
                Map<String, List<Map<String, Object>>> memberCheckinMap = new HashMap<>();
                int todayCount = 0;
                Set<String> todayMembers = new HashSet<>();
                if (allCheckins != null) {
                    for (Map<String, Object> ci : allCheckins) {
                        if (ngayHomNay().equals(KhoDuLieu.chuoi(ci.get("ngay")))) {
                            todayCount++;
                            String todayUid = KhoDuLieu.chuoi(ci.get("uid_nguoi_dung"));
                            if (todayUid.isEmpty()) todayUid = KhoDuLieu.chuoi(ci.get("uid_hoi_vien"));
                            if (!todayUid.isEmpty()) todayMembers.add(todayUid);
                        }
                        String mUid = KhoDuLieu.chuoi(ci.get("uid_nguoi_dung"));
                        if (mUid.isEmpty()) mUid = KhoDuLieu.chuoi(ci.get("uid_hoi_vien"));
                        if (!mUid.isEmpty()) {
                            if (!memberCheckinMap.containsKey(mUid)) memberCheckinMap.put(mUid, new ArrayList<>());
                            memberCheckinMap.get(mUid).add(ci);
                        }
                    }
                }
                totalStat.setText(String.valueOf(todayCount));
                memberStat.setText(String.valueOf(todayMembers.size()));

                kho.layTheoTruong("nguoi_dung", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override public void thanhCong(List<Map<String, Object>> gymUsers) {
                        Map<String, String> memberNameMap = new HashMap<>();
                        if (gymUsers != null) {
                            for (Map<String, Object> u : gymUsers) {
                                if ("hoi_vien".equals(KhoDuLieu.chuoi(u.get("vai_tro")))) {
                                    memberNameMap.put(KhoDuLieu.chuoi(u.get("id")), KhoDuLieu.chuoi(u.get("ho_ten")));
                                }
                            }
                        }

                        for (Map.Entry<String, List<Map<String, Object>>> entry : memberCheckinMap.entrySet()) {
                            if (!memberNameMap.containsKey(entry.getKey())) {
                                List<Map<String, Object>> listCi = entry.getValue();
                                String name = !listCi.isEmpty() ? KhoDuLieu.chuoi(listCi.get(0).get("ten_nguoi_dung")) : "";
                                if (name.isEmpty() && !listCi.isEmpty()) name = KhoDuLieu.chuoi(listCi.get(0).get("ten_hoi_vien"));
                                if (name.isEmpty()) name = "Hội viên #" + entry.getKey().substring(0, Math.min(5, entry.getKey().length()));
                                memberNameMap.put(entry.getKey(), name);
                            }
                        }

                        if (allCheckins != null) {
                            for (Map<String, Object> ci : allCheckins) {
                                String id = KhoDuLieu.chuoi(ci.get("uid_nguoi_dung"));
                                if (id.isEmpty()) id = KhoDuLieu.chuoi(ci.get("uid_hoi_vien"));
                                Map<String, Object> row = new HashMap<>(ci);
                                row.put("_member_name", memberNameMap.get(id) == null ? KhoDuLieu.chuoi(ci.get("ten_nguoi_dung")) : memberNameMap.get(id));
                                row.put("_phone", ""); row.put("_email", "");
                                allRows.add(row);
                            }
                        }
                        allRows.sort((a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
                        renderRows.run();

                        if (memberNameMap.isEmpty()) {
                            return;
                        }

                        for (Map.Entry<String, String> entry : memberNameMap.entrySet()) {
                            String mUid = entry.getKey();
                            String mName = entry.getValue();

                            List<Map<String, Object>> checkins = memberCheckinMap.get(mUid);
                            if (checkins == null) checkins = new ArrayList<>();

                            Set<String> uniqueDays = new HashSet<>();
                            for (Map<String, Object> ci : checkins) {
                                String dayStr = KhoDuLieu.chuoi(ci.get("ngay"));
                                if (!dayStr.isEmpty()) uniqueDays.add(dayStr);
                            }

                            int daysCount = Math.min(30, uniqueDays.size());

                            LinearLayout card = new LinearLayout(MainActivity.this);
                            card.setOrientation(LinearLayout.VERTICAL);
                            card.setPadding(dp(18), dp(16), dp(18), dp(16));

                            GradientDrawable bg = new GradientDrawable();
                            bg.setColor(0xFF1B1A1D);
                            bg.setCornerRadius(dp(18));
                            bg.setStroke(dp(1), 0xFF51343A);
                            card.setBackground(bg);

                            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                            cp.bottomMargin = dp(14);
                            list.addView(card, cp);

                            TextView tvName = chu("👤 Hội viên: " + mName, 17, TRANG, true);
                            card.addView(tvName);

                            TextView tvStreak = chu("🏃 Chuyên cần tháng này: " + daysCount + "/30 ngày tập", 13, 0xFFFF777D, true);
                            tvStreak.setPadding(0, dp(4), 0, dp(8));
                            card.addView(tvStreak);

                            ProgressBar bar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleHorizontal);
                            bar.setMax(30);
                            bar.setProgress(daysCount);
                            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFE53935));
                            bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF454248));
                            LinearLayout.LayoutParams barP = new LinearLayout.LayoutParams(-1, dp(12));
                            barP.bottomMargin = dp(10);
                            card.addView(bar, barP);

                            final List<Map<String, Object>> finalCheckins = checkins;
                            final String finalMemberName = mName;
                            themNutVaoThe(card, "📋 XEM CHI TIẾT ĐIỂM DANH & BÀI TẬP", true, () -> hienChiTietDiemDanhHoiVien(finalMemberName, finalCheckins));
                        }
                    }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được điểm danh: " + error); }
        });
    }

    private TextView taoTheThongKeDiemDanh(LinearLayout parent, String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER);
        card.setPadding(dp(5), dp(12), dp(5), dp(12));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF292326, 0xFF1B1A1D});
        bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0xFF4D353B); card.setBackground(bg);
        TextView caption = chu(label, 9, 0xFFD0C9CD, true); caption.setGravity(Gravity.CENTER);
        TextView number = chu(value, 20, TRANG, true); number.setGravity(Gravity.CENTER);
        card.addView(caption); LinearLayout.LayoutParams n = new LinearLayout.LayoutParams(-1, -2);
        n.topMargin = dp(5); card.addView(number, n);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(88), 1);
        p.setMargins(0, 0, dp(7), 0); parent.addView(card, p);
        return number;
    }

    private void hienChiTietDiemDanhHoiVien(String memberName, List<Map<String, Object>> checkins) {
        taoTrang("CHI TIẾT ĐIỂM DANH", memberName, this::hienDiemDanh, false);
        if (checkins == null || checkins.isEmpty()) {
            the("Chưa có lượt điểm danh", memberName + " chưa có lịch sử điểm danh nào.");
            return;
        }

        Collections.sort(checkins, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));

        for (Map<String, Object> item : checkins) {
            String day = KhoDuLieu.chuoi(item.get("ngay"));
            String content = KhoDuLieu.chuoi(item.get("noi_dung"));
            if (content.isEmpty()) content = "Đã tập luyện tại phòng Gym";
            long duration = laySo(item, "thoi_luong_phut");
            String diet = KhoDuLieu.chuoi(item.get("huong_dan_an_uong"));

            String details = "📅 Ngày tập: " + day
                    + (duration > 0 ? "\n⏱️ Thời gian tập: " + duration + " phút" : "")
                    + "\n🏋️ Nội dung: " + content
                    + (diet.isEmpty() ? "" : "\n🥗 Chế độ dinh dưỡng: " + diet)
                    + "\n⏰ Điểm danh lúc: " + ngay(laySo(item, "ngay_tao"));

            the("Lượt tập ngày " + day, details);
        }
    }

    private void hienCaiDatGym() {
        if (!laQuanTri()) return;
        kho.layMot("phong_gym", gymId(), new KhoDuLieu.KetQua<Map<String, Object>>() {
            @Override public void thanhCong(Map<String, Object> gym) {
                if (gym == null) { hienTaoThuongHieu(); return; }
                hienFormCaiDatGym(gym);
            }
            @Override public void thatBai(String error) { baoLoi(error); }
        });
    }

    private void hienFormCaiDatGym(Map<String, Object> gym) {
        taoTrang("THÔNG TIN THƯƠNG HIỆU", "Thông tin công khai để hội viên nhận diện Gym", this::hienBangDieuKhien, false);
        thanTrang.setPadding(dp(16), dp(12), dp(16), dp(28));

        // Banner nhận diện thương hiệu theo phong cách đỏ - đen của GymManagement.
        LinearLayout brandBanner = new LinearLayout(this);
        brandBanner.setGravity(Gravity.CENTER_VERTICAL);
        brandBanner.setPadding(dp(16), dp(18), dp(16), dp(18));
        brandBanner.setBackground(taoNenGradient(new int[]{0xFF351014, 0xFF171315, 0xFF101010}, 22, 0xFF8D292F));
        ImageView brandIcon = new ImageView(this);
        brandIcon.setImageResource(R.drawable.ic_store);
        brandIcon.setColorFilter(Color.WHITE);
        brandIcon.setPadding(dp(13), dp(13), dp(13), dp(13));
        brandIcon.setBackground(nenVien(0xFFE52D32, 0xFFFF7777, 16));
        brandBanner.addView(brandIcon, new LinearLayout.LayoutParams(dp(62), dp(62)));
        LinearLayout brandTexts = new LinearLayout(this);
        brandTexts.setOrientation(LinearLayout.VERTICAL);
        brandTexts.setPadding(dp(14), 0, 0, 0);
        TextView brandName = chu(KhoDuLieu.chuoi(gym.get("ten_thuong_hieu")).isEmpty() ? "THƯƠNG HIỆU CỦA BẠN" : KhoDuLieu.chuoi(gym.get("ten_thuong_hieu")).toUpperCase(new Locale("vi", "VN")), 17, TRANG, true);
        TextView brandCaption = chu("THÔNG TIN CÔNG KHAI · GYM MANAGEMENT", 10, 0xFFFFA0A0, true);
        brandCaption.setPadding(0, dp(5), 0, 0);
        brandTexts.addView(brandName);
        brandTexts.addView(brandCaption);
        brandBanner.addView(brandTexts, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, dp(116));
        bannerParams.bottomMargin = dp(22);
        thanTrang.addView(brandBanner, bannerParams);

        LinearLayout sectionTitle = new LinearLayout(this);
        sectionTitle.setGravity(Gravity.CENTER_VERTICAL);
        TextView sectionIcon = chu("ⓘ", 23, 0xFFFF555B, true);
        sectionTitle.addView(sectionIcon, new LinearLayout.LayoutParams(dp(34), -2));
        sectionTitle.addView(chu("Thông tin thương hiệu", 19, TRANG, true));
        thanTrang.addView(sectionTitle);
        View accent = new View(this);
        accent.setBackground(taoNenGradient(new int[]{0xFF68151B, 0xFFFF343B, 0xFF68151B}, 3, 0x00FFFFFF));
        LinearLayout.LayoutParams accentP = new LinearLayout.LayoutParams(-1, dp(2));
        accentP.topMargin = dp(10); accentP.bottomMargin = dp(18);
        thanTrang.addView(accent, accentP);

        EditText name = taoTruongThuongHieu(R.drawable.ic_gym, "Tên thương hiệu Gym", "Nhập tên thương hiệu", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, KhoDuLieu.chuoi(gym.get("ten_thuong_hieu")));
        EditText address = taoTruongThuongHieu(R.drawable.ic_location, "Địa chỉ", "Nhập địa chỉ phòng Gym", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, KhoDuLieu.chuoi(gym.get("dia_chi")));
        EditText phone = taoTruongThuongHieu(R.drawable.ic_phone, "Số điện thoại", "Nhập số điện thoại liên hệ", InputType.TYPE_CLASS_PHONE, KhoDuLieu.chuoi(gym.get("so_dien_thoai")));

        Button save = taoNut("▣   LƯU THÔNG TIN", true, () -> {
            String n = name.getText().toString().trim().replaceAll("\\s+", " ");
            String a = address.getText().toString().trim();
            String p = phone.getText().toString().trim();
            if (n.isEmpty()) { name.setError("Nhập tên thương hiệu"); return; }
            if (a.isEmpty()) { address.setError("Nhập địa chỉ"); return; }
            if (!p.matches("[0-9]{9,11}")) { phone.setError("Số điện thoại gồm 9–11 chữ số"); return; }
            kho.layTatCa("phong_gym", new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                @Override public void thanhCong(List<Map<String, Object>> all) {
                    String norm = KhoDuLieu.chuanHoaTen(n);
                    for (Map<String, Object> other : all) {
                        if (KhoDuLieu.chuoi(other.get("id")).equals(gymId())) continue;
                        String old = KhoDuLieu.chuoi(other.get("ten_thuong_hieu_chuan"));
                        if (old.isEmpty()) old = KhoDuLieu.chuanHoaTen(KhoDuLieu.chuoi(other.get("ten_thuong_hieu")));
                        if (old.equals(norm)) { baoLoi("Tên thương hiệu đã được sử dụng."); return; }
                    }
                    Map<String, Object> patch = new HashMap<>();
                    patch.put("ten_thuong_hieu", n);
                    patch.put("ten_thuong_hieu_chuan", norm);
                    patch.put("ma_gym", n);
                    patch.put("dia_chi", a);
                    patch.put("so_dien_thoai", p);
                    patch.put("ngay_cap_nhat", System.currentTimeMillis());
                    kho.capNhat("phong_gym", gymId(), patch, new KhoDuLieu.KetQua<Boolean>() {
                        @Override public void thanhCong(Boolean ok) {
                            nguoiDung.put("ten_thuong_hieu", n);
                            tenGymHienTai = n;
                            Map<String, Object> userPatch = new HashMap<>();
                            userPatch.put("ten_thuong_hieu", n);
                            kho.capNhat("nguoi_dung", uid, userPatch, new KhoDuLieu.KetQua<Boolean>() {
                                @Override public void thanhCong(Boolean ok2) { thongBao("Đã cập nhật thương hiệu."); hienBangDieuKhien(); }
                                @Override public void thatBai(String error) { thongBao("Đã cập nhật Gym. Hồ sơ sẽ dùng tên mới khi tải lại."); hienBangDieuKhien(); }
                            });
                        }
                        @Override public void thatBai(String error) { baoLoi(error); }
                    });
                }
                @Override public void thatBai(String error) { baoLoi(error); }
            });
        });
        save.setTextSize(16);
        save.setMinHeight(dp(58));
        save.setBackground(taoNenGradient(new int[]{0xFFFF343A, 0xFFB80F1B, 0xFFEA252D}, 30, 0xFFFF7777));
        LinearLayout.LayoutParams saveP = new LinearLayout.LayoutParams(-1, dp(60));
        saveP.topMargin = dp(10);
        saveP.bottomMargin = dp(18);
        thanTrang.addView(save, saveP);
    }

    private EditText taoTruongThuongHieu(int iconRes, String label, String hint, int inputType, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(13), dp(13), dp(13));
        card.setBackground(taoNenGradient(new int[]{0xFF21191C, 0xFF151315}, 19, 0xFF593039));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFFFF7777);
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        icon.setBackground(nenVien(0xFF45191E, 0xFF743039, 15));
        card.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(13), 0, 0, 0);
        fields.addView(chu(label, 13, 0xFFC8BFC2, false));
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(16);
        edit.setTextColor(TRANG);
        edit.setHintTextColor(0xFF81777B);
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setPadding(0, dp(5), 0, 0);
        edit.setBackgroundColor(Color.TRANSPARENT);
        if (!TextUtils.isEmpty(value)) edit.setText(value);
        fields.addView(edit, new LinearLayout.LayoutParams(-1, -2));
        card.addView(fields, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(14);
        thanTrang.addView(card, p);
        return edit;
    }

    private GradientDrawable taoNenGradient(int[] colors, int radius, int stroke) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        bg.setCornerRadius(dp(radius));
        if (stroke != 0) bg.setStroke(dp(1), stroke);
        return bg;
    }

    // ---------------------------------------------------------------------
    // HỒ SƠ NGƯỜI DÙNG
    // ---------------------------------------------------------------------

    private void hienHoSo() {
        FragmentProfileBinding profile = FragmentProfileBinding.inflate(getLayoutInflater());
        manHinhHienTai = getString(R.string.profile_title).toUpperCase(new Locale("vi", "VN"));
        String name = tenNguoiDung();
        String email = KhoDuLieu.chuoi(nguoiDung.get("email"));
        String phone = KhoDuLieu.chuoi(nguoiDung.get("so_dien_thoai"));
        profile.pfName.setText(name);

        String avatarUrl = KhoDuLieu.chuoi(nguoiDung.get("anh_dai_dien"));
        if (!avatarUrl.isEmpty()) {
            profile.pfAvatar.setText("");
            Glide.with(this).load(avatarUrl).circleCrop().into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                @Override public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                    profile.pfAvatar.setBackground(resource);
                }
                @Override public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
            });
        } else {
            profile.pfAvatar.setText(name.substring(0, 1).toUpperCase(new Locale("vi", "VN")));
        }

        profile.pfEmail.setText(email);
        profile.pfEmailValue.setText(email);
        profile.pfPhone.setText(phone.isEmpty() ? getString(R.string.profile_no_phone_value) : phone);
        profile.pfRole.setText(laQuanTri() ? R.string.profile_role_owner : R.string.profile_role_member);
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            profile.pfVersion.setText(getString(R.string.profile_brand_version, version));
        } catch (Exception ignored) {
            profile.pfVersion.setText(getString(R.string.profile_brand_version, "1.0.0"));
        }

        if (laQuanTri()) {
            profile.pfGymLabel.setText(R.string.profile_owner_brand_label);
            profile.pfGymName.setText(TextUtils.isEmpty(tenGymHienTai)
                    ? getString(R.string.profile_no_registered_gym) : tenGymHienTai);
            profile.pfPackageLabel.setVisibility(View.GONE);
            profile.pfPackageName.setVisibility(View.GONE);
            profile.pfMembershipDetails.setVisibility(View.GONE);
            profile.pfMembershipCard.setOnClickListener(v -> hienCaiDatGym());
        } else {
            profile.pfGymName.setText(R.string.profile_no_registered_gym);
            profile.pfPackageName.setText(R.string.profile_no_current_package);
            profile.pfPackageLabel.setVisibility(View.GONE);
            profile.pfMembershipDetails.setVisibility(View.GONE);
            profile.pfMembershipCard.setOnClickListener(v -> hienDanhSachGymDangKyGoi());
            hienGoiTapTrenHoSo(profile);
        }

        profile.pfBack.setOnClickListener(v -> hienBangDieuKhien());
        profile.pfMenu.setOnClickListener(v -> hienMenuHoSo(profile.pfMenu));
        profile.pfEdit.setOnClickListener(v -> hienFormHoSo());
        profile.pfAvatarEdit.setOnClickListener(v -> hienDoiAnhDaiDien());
        profile.pfAvatar.setOnClickListener(v -> hienDoiAnhDaiDien());
        profile.pfPhoneRow.setOnClickListener(v -> hienFormHoSo());
        profile.pfPasswordRow.setOnClickListener(v -> guiEmailDatLaiMatKhauHoSo());
        profile.pfLogoutRow.setOnClickListener(v -> xuLyDangXuat());

        khungChinh.removeAllViews();
        khungChinh.addView(profile.getRoot(), new FrameLayout.LayoutParams(-1, -1));
    }

    private static final int REQUEST_CODE_PICK_AVATAR = 999;

    private void hienDoiAnhDaiDien() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh đại diện từ Thư viện"), REQUEST_CODE_PICK_AVATAR);
        } catch (Exception e) {
            baoLoi("Không mở được thư viện ảnh: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            capNhatAnhDaiDien(selectedImageUri.toString());
        }
    }

    private void capNhatAnhDaiDien(String avatarUrl) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("anh_dai_dien", avatarUrl);
        kho.capNhat("nguoi_dung", uid, patch, new KhoDuLieu.KetQua<Boolean>() {
            @Override public void thanhCong(Boolean ok) {
                if (nguoiDung != null) nguoiDung.put("anh_dai_dien", avatarUrl);
                thongBao("🎉 Đã cập nhật ảnh đại diện thành công!");
                hienHoSo();
            }
            @Override public void thatBai(String error) { baoLoi(error); }
        });
    }

    private void hienBannerHealthNews() {
        themChu("DAILY HEALTH NEWS (TIN TỨC CƠ BẮP & DINH DƯỠNG)", 13, 0xFFE53935, true);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(12));

        row.addView(taoCardNews("🔥 BÍ QUYẾT TẬP CƠ BỤNG 6 MÚI", "Mẹo siết cơ bụng chuẩn Fitness...", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b", 0xFFFF5252, () -> {
            hienChiTietBaiBao("BÍ QUYẾT TẬP CƠ BỤNG 6 MÚI HIỆU QUẢ",
                    "Để sở hữu cơ bụng 6 múi săn chắc, việc tập luyện cơ bụng thôi là chưa đủ. Bạn cần kết hợp chế độ dinh dưỡng thâm hụt calo hợp lý để giảm mỡ bụng dưới 12%.\n\n1. Các bài tập cốt lõi (Core):\n- Plank liên tục 3 hiệp mỗi ngày.\n- Leg raises (nâng chân xà đơn).\n- Bicycle crunches.\n\n2. Dinh dưỡng:\n- Ăn đủ protein (1.6g - 2g/kg thể trọng).\n- Uống đủ 2.5 - 3 lít nước mỗi ngày.\n- Hạn chế đường và tinh bột hấp thu nhanh.");
        }));

        row.addView(taoCardNews("🥗 CHẾ ĐỘ ĂN WHEY & PROTEIN", "Thực đơn dinh dưỡng tăng cơ...", "https://images.unsplash.com/photo-1544025162-d76694265947", 0xFF4CAF50, () -> {
            hienChiTietBaiBao("DINH DƯỠNG TĂNG CƠ TỐI ƯU VỚI WHEY PROTEIN",
                    "Protein là nguyên liệu xây dựng cơ bắp quan trọng nhất. Sau khi tập luyện, cơ bắp cần protein để phục hồi và phát triển.\n\n- Thời điểm vàng uống Whey: Ngay sau buổi tập trong vòng 30 phút.\n- Hàm lượng khuyên dùng: 25-30g protein mỗi lần dùng.\n- Nguồn thực phẩm tự nhiên: Ức gà, trứng, cá hồi, thịt bò nạc và đậu hũ.");
        }));

        scroll.addView(row);
        thanTrang.addView(scroll);
    }

    private View taoCardNews(String title, String subtitle, String imageUrl, int accentColor, Runnable onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setMinimumWidth(dp(240));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFFB2DFDB);
        card.setBackground(bg);

        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ImageLoader.load(img, imageUrl, R.drawable.bg_gym_placeholder);
        LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(-1, dp(110));
        imgP.setMargins(0, 0, 0, dp(8));
        img.setLayoutParams(imgP);
        card.addView(img);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(accentColor);
        tvTitle.setTextSize(12);
        tvTitle.setTypeface(null, Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(0xFF475569);
        tvSub.setTextSize(11);
        tvSub.setPadding(0, dp(2), 0, 0);
        card.addView(tvSub);

        card.setOnClickListener(v -> onClick.run());

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(260), -2);
        p.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(p);
        return card;
    }

    private void hienChiTietBaiBao(String title, String content) {
        taoTrang(title, "Cẩm nang Fitness & Dinh dưỡng", this::hienBangDieuKhien, true);
        the(title, content);
    }

    private void hienQuanLyTheTrangHoiVien() {
        if (!laQuanTri()) { baoLoi("Chức năng này dành cho quản trị Gym."); return; }
        taoTrang("QUẢN LÝ THỂ TRẠNG HỘI VIÊN", tenGymHienTai, this::hienBangDieuKhien, true);
        themChu("Danh sách hội viên và lịch sử chỉ số cơ thể theo ngày tháng", 13, XAM, false);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(list);

        kho.layTatCa("nguoi_dung", new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> users) {
                kho.layTatCa("chi_so_co_the", new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override public void thanhCong(List<Map<String, Object>> measurements) {
                        Map<String, List<Map<String, Object>>> memberMetrics = new HashMap<>();
                        if (measurements != null) {
                            for (Map<String, Object> m : measurements) {
                                String uId = KhoDuLieu.chuoi(m.get("uid_nguoi_dung"));
                                if (!memberMetrics.containsKey(uId)) memberMetrics.put(uId, new ArrayList<>());
                                memberMetrics.get(uId).add(m);
                            }
                        }

                        int count = 0;
                        for (Map<String, Object> user : users) {
                            String role = KhoDuLieu.chuoi(user.get("vai_tro"));
                            String mGym = KhoDuLieu.chuoi(user.get("gym_id"));
                            String mId = KhoDuLieu.chuoi(user.get("id"));
                            if (!"hoi_vien".equals(role)) continue;
                            if (!gymId().equals(mGym) && !memberMetrics.containsKey(mId)) continue;
                            count++;

                            List<Map<String, Object>> history = memberMetrics.get(mId);
                            if (history == null) history = new ArrayList<>();
                            Collections.sort(history, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));

                            String latestInfo = "Chưa có dữ liệu đo chỉ số.";
                            if (!history.isEmpty()) {
                                Map<String, Object> latest = history.get(0);
                                latestInfo = "Cân nặng gần nhất: " + decimal(latest.get("can_nang")) + " kg · BMI: " + decimal(latest.get("bmi")) + " · Ngày đo: " + KhoDuLieu.chuoi(latest.get("ngay_do"));
                            }

                            LinearLayout card = the(KhoDuLieu.chuoi(user.get("ho_ten")), "📧 " + KhoDuLieu.chuoi(user.get("email")) + "\n" + latestInfo);
                            final List<Map<String, Object>> memberHistory = history;
                            final String memberName = KhoDuLieu.chuoi(user.get("ho_ten"));
                            themNutVaoThe(card, "📊 XEM LỊCH SỬ THỂ TRẠNG (" + history.size() + " lần đo)", true, () -> hienLichSuTheTrang(memberName, memberHistory));
                        }

                        if (count == 0) {
                            the("Chưa có hội viên", "Chưa có hội viên nào trong hệ thống.");
                        }
                    }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            }
            @Override public void thatBai(String error) { baoLoi(error); }
        });
    }

    private void hienLichSuTheTrang(String memberName, List<Map<String, Object>> history) {
        taoTrang("LỊCH SỬ THỂ TRẠNG", memberName, this::hienQuanLyTheTrangHoiVien, false);
        if (history.isEmpty()) {
            the("Chưa có dữ liệu", "Hội viên này chưa cập nhật chỉ số cơ thể nào.");
            return;
        }
        for (Map<String, Object> item : history) {
            the("📅 Ngày đo: " + KhoDuLieu.chuoi(item.get("ngay_do")),
                    "⚖️ Cân nặng: " + decimal(item.get("can_nang")) + " kg\n"
                            + "📏 Chiều cao: " + decimal(item.get("chieu_cao")) + " cm\n"
                            + "📊 BMI: " + decimal(item.get("bmi")) + "\n"
                            + (KhoDuLieu.chuoi(item.get("ti_le_mo")).isEmpty() ? "" : "💧 Tỷ lệ mỡ: " + decimal(item.get("ti_le_mo")) + "%\n")
                            + "⏰ Cập nhật lúc: " + ngay(laySo(item, "ngay_tao")));
        }
    }

    private View taoNutLuoiPro(String icon, String title, String subtext, int bgColor, int textColor, Runnable action) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14), dp(16), dp(14), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(18));
        box.setBackground(bg);

        LinearLayout rowHead = new LinearLayout(this);
        rowHead.setOrientation(LinearLayout.HORIZONTAL);
        rowHead.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(18);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(" " + title);
        tvTitle.setTextColor(textColor);
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, Typeface.BOLD);

        rowHead.addView(tvIcon);
        rowHead.addView(tvTitle);
        box.addView(rowHead);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtext);
        tvSub.setTextColor(Color.argb(180, Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
        tvSub.setTextSize(10);
        tvSub.setPadding(0, dp(4), 0, 0);
        box.addView(tvSub);

        box.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
                    break;
            }
            return false;
        });

        if (action != null) {
            box.setOnClickListener(v -> action.run());
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        params.setMargins(dp(5), dp(6), dp(5), dp(6));
        box.setLayoutParams(params);
        return box;
    }

    private void hienGoiTapTrenHoSo(FragmentProfileBinding profile) {
        kho.layTheoTruong("dang_ky_goi", "uid_nguoi_dung", uid,
                new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override public void thanhCong(List<Map<String, Object>> all) {
                        Map<String, Object> current = null;
                        for (Map<String, Object> item : all) {
                            String status = KhoDuLieu.chuoi(item.get("trang_thai"));
                            if (!"dang_hoat_dong".equals(status) && !"cho_xac_nhan".equals(status)) continue;
                            if (current == null || laySo(item, "ngay_dang_ky") > laySo(current, "ngay_dang_ky")) {
                                current = item;
                            }
                        }
                        if (current == null) return;
                        final Map<String, Object> membership = current;
                        profile.pfPackageLabel.setVisibility(View.VISIBLE);
                        profile.pfPackageName.setVisibility(View.VISIBLE);
                        profile.pfMembershipDetails.setVisibility(View.VISIBLE);
                        profile.pfPackageName.setText(KhoDuLieu.chuoi(membership.get("ten_goi")));
                        profile.pfMembershipUntil.setText(getString(R.string.profile_until_date,
                                ngay(laySo(membership, "ngay_ket_thuc"))));
                        boolean active = "dang_hoat_dong".equals(KhoDuLieu.chuoi(membership.get("trang_thai")));
                        int color = getColor(active ? R.color.success : R.color.warning);
                        profile.pfMembershipStatus.setText(active ? R.string.profile_membership_active
                                : R.string.profile_membership_pending);
                        profile.pfMembershipStatus.setTextColor(color);
                        profile.pfStatusDot.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(color));
                        String selectedGymId = KhoDuLieu.chuoi(membership.get("gym_id"));
                        if (!selectedGymId.isEmpty()) {
                            kho.layMot("phong_gym", selectedGymId,
                                    new KhoDuLieu.KetQua<Map<String, Object>>() {
                                        @Override public void thanhCong(Map<String, Object> gym) {
                                            if (gym != null) profile.pfGymName.setText(
                                                    KhoDuLieu.chuoi(gym.get("ten_thuong_hieu")));
                                        }
                                        @Override public void thatBai(String error) { }
                                    });
                        }
                    }
                    @Override public void thatBai(String error) { }
                });
    }

    private void hienMenuHoSo(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Thông báo").setOnMenuItemClickListener(item -> { hienThongBao(); return true; });
        if (laQuanTri()) {
            menu.getMenu().add("Thương hiệu Gym").setOnMenuItemClickListener(item -> { hienCaiDatGym(); return true; });
            menu.getMenu().add("Gói tập").setOnMenuItemClickListener(item -> { hienGoiTap(); return true; });
            menu.getMenu().add("Hội viên").setOnMenuItemClickListener(item -> { hienHoiVien(); return true; });
            menu.getMenu().add("Lịch tập").setOnMenuItemClickListener(item -> { hienLichTap(); return true; });
            menu.getMenu().add("Điểm danh").setOnMenuItemClickListener(item -> { hienDiemDanh(); return true; });
        } else {
            menu.getMenu().add("Đăng ký gói tập").setOnMenuItemClickListener(item -> { hienDanhSachGymDangKyGoi(); return true; });
            menu.getMenu().add("Lịch tập").setOnMenuItemClickListener(item -> { hienLichTap(); return true; });
            menu.getMenu().add("Chỉ số cơ thể").setOnMenuItemClickListener(item -> { hienChiSoCoThe(); return true; });
            menu.getMenu().add("Tiến độ tập luyện").setOnMenuItemClickListener(item -> { hienTienDo(); return true; });
        }
        menu.show();
    }

    private void guiEmailDatLaiMatKhauHoSo() {
        kho.guiEmailDatLaiMatKhau(KhoDuLieu.chuoi(nguoiDung.get("email")),
                new KhoDuLieu.KetQua<Boolean>() {
                    @Override public void thanhCong(Boolean ok) {
                        thongBao("Nếu email tồn tại, Firebase sẽ gửi hướng dẫn đặt lại mật khẩu.");
                    }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
    }

    private void hienFormHoSo() {
        String title = getString(R.string.profile_edit).toUpperCase(new Locale("vi", "VN"));
        taoTrang(title, "Cập nhật tên và số điện thoại", this::hienHoSo, false);
        thanTrang.setPadding(dp(16), dp(10), dp(16), dp(30));

        // Hero profile: tách rõ màn hình chỉnh sửa khỏi màn hình Hồ sơ tổng quan.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(18), dp(20), dp(18), dp(20));
        hero.setBackground(taoNenGradient(new int[]{0xFF401017, 0xFF211014, 0xFF111114}, 26, 0xFF9E303A));
        TextView avatar = chu("T", 32, TRANG, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(nenVien(0xFFF4F7F2, 0xFFFF3945, 50));
        avatar.setTextColor(0xFF17171B);
        LinearLayout.LayoutParams avatarP = new LinearLayout.LayoutParams(dp(82), dp(82));
        hero.addView(avatar, avatarP);
        TextView camera = chu("📷", 14, TRANG, true);
        camera.setGravity(Gravity.CENTER);
        camera.setBackground(nenVien(0xFFE53935, 0xFFFF7777, 30));
        LinearLayout.LayoutParams cameraP = new LinearLayout.LayoutParams(dp(32), dp(32));
        cameraP.topMargin = dp(-18);
        cameraP.leftMargin = dp(48);
        hero.addView(camera, cameraP);
        TextView memberName = chu(tenNguoiDung(), 20, TRANG, true);
        memberName.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams memberP = new LinearLayout.LayoutParams(-1, -2);
        memberP.topMargin = dp(2);
        hero.addView(memberName, memberP);
        TextView role = chu("✦   " + (laQuanTri() ? "CHỦ GYM" : "HỘI VIÊN") + "   ✦", 11, 0xFFFF7777, true);
        role.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams roleP = new LinearLayout.LayoutParams(-1, -2);
        roleP.topMargin = dp(7);
        hero.addView(role, roleP);
        TextView emailHero = chu(KhoDuLieu.chuoi(nguoiDung.get("email")), 12, 0xFFBDB0B2, false);
        emailHero.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emailHeroP = new LinearLayout.LayoutParams(-1, -2);
        emailHeroP.topMargin = dp(3);
        hero.addView(emailHero, emailHeroP);
        LinearLayout.LayoutParams heroP = new LinearLayout.LayoutParams(-1, -2);
        heroP.bottomMargin = dp(16);
        thanTrang.addView(hero, heroP);

        if (laQuanTri()) {
            LinearLayout brand = the("THƯƠNG HIỆU GYM", tenGymHienTai);
            brand.setBackground(taoNenGradient(new int[]{0xFF30151A, 0xFF191519}, 17, 0xFF71343B));
        }

        LinearLayout personal = new LinearLayout(this);
        personal.setOrientation(LinearLayout.VERTICAL);
        personal.setPadding(dp(16), dp(16), dp(16), dp(8));
        personal.setBackground(taoNenGradient(new int[]{0xFF291317, 0xFF171518}, 23, 0xFF71343B));
        TextView personalTitle = chu("👤  THÔNG TIN CÁ NHÂN", 16, TRANG, true);
        personal.addView(personalTitle);
        View accent = new View(this);
        accent.setBackground(taoNenGradient(new int[]{0xFFFF2635, 0xFF6F2028}, 4, 0x00FFFFFF));
        LinearLayout.LayoutParams accentP = new LinearLayout.LayoutParams(dp(72), dp(3));
        accentP.topMargin = dp(10);
        accentP.bottomMargin = dp(14);
        personal.addView(accent, accentP);

        EditText name = themTruongHoSo(personal, "♙", "Họ và tên", "Nhập họ và tên", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, KhoDuLieu.chuoi(nguoiDung.get("ho_ten")), true);
        EditText email = themTruongHoSo(personal, "✉", "Email tài khoản", "Email đăng nhập", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, KhoDuLieu.chuoi(nguoiDung.get("email")), false);
        email.setEnabled(false);
        email.setAlpha(0.72f);
        EditText phone = themTruongHoSo(personal, "☎", "Số điện thoại", "Nhập số điện thoại", InputType.TYPE_CLASS_PHONE, KhoDuLieu.chuoi(nguoiDung.get("so_dien_thoai")), true);
        TextView genderLabel = chu("Giới tính", 11, 0xFFFFA0A5, true);
        LinearLayout.LayoutParams genderLabelP = new LinearLayout.LayoutParams(-1, -2); genderLabelP.topMargin = dp(10); personal.addView(genderLabel, genderLabelP);
        Spinner genderSpinner = new Spinner(this);
        List<String> genderOptions = new ArrayList<>();
        genderOptions.add("Chọn giới tính"); genderOptions.add("Nam"); genderOptions.add("Nữ");
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);
        genderSpinner.setBackground(nenVien(0xFF1C191B, 0xFF754047, 12));
        LinearLayout.LayoutParams genderP = new LinearLayout.LayoutParams(-1, dp(48)); genderP.topMargin = dp(5); genderP.bottomMargin = dp(8); personal.addView(genderSpinner, genderP);
        String savedGender = KhoDuLieu.chuoi(nguoiDung.get("gioi_tinh")).toLowerCase(new Locale("vi", "VN"));
        if (savedGender.isEmpty()) savedGender = KhoDuLieu.chuoi(nguoiDung.get("gender")).toLowerCase(Locale.ROOT);
        genderSpinner.setSelection(savedGender.equals("nữ") || savedGender.equals("nu") || savedGender.equals("female") ? 2 : savedGender.isEmpty() ? 0 : 1);
        LinearLayout.LayoutParams personalP = new LinearLayout.LayoutParams(-1, -2);
        personalP.bottomMargin = dp(16);
        thanTrang.addView(personal, personalP);

        LinearLayout security = new LinearLayout(this);
        security.setOrientation(LinearLayout.VERTICAL);
        security.setPadding(dp(16), dp(16), dp(16), dp(8));
        security.setBackground(taoNenGradient(new int[]{0xFF291317, 0xFF171518}, 23, 0xFF71343B));
        security.addView(chu("🛡  BẢO MẬT TÀI KHOẢN", 16, TRANG, true));
        View securityAccent = new View(this);
        securityAccent.setBackground(taoNenGradient(new int[]{0xFFFF2635, 0xFF6F2028}, 4, 0x00FFFFFF));
        LinearLayout.LayoutParams securityAccentP = new LinearLayout.LayoutParams(dp(72), dp(3));
        securityAccentP.topMargin = dp(10);
        securityAccentP.bottomMargin = dp(12);
        security.addView(securityAccent, securityAccentP);
        Button passwordButton = taoNutNho("🔑   ĐỔI / ĐẶT LẠI MẬT KHẨU     ›", false, this::guiEmailDatLaiMatKhauHoSo);
        passwordButton.setMinHeight(dp(54));
        passwordButton.setTextSize(13);
        passwordButton.setBackground(taoNenGradient(new int[]{0xFF352025, 0xFF1B181B}, 15, 0xFF5B3439));
        security.addView(passwordButton);
        LinearLayout.LayoutParams securityP = new LinearLayout.LayoutParams(-1, -2);
        securityP.bottomMargin = dp(16);
        thanTrang.addView(security, securityP);

        nut("LƯU HỒ SƠ", true, () -> {
            String n = name.getText().toString().trim(), p = phone.getText().toString().trim();
            if (n.isEmpty()) { name.setError("Vui lòng nhập họ tên"); return; }
            if (!p.matches("[0-9]{9,11}")) { phone.setError("Số điện thoại gồm 9–11 chữ số"); return; }
            if (genderSpinner.getSelectedItemPosition() == 0) { baoLoi("Vui lòng chọn giới tính để hiển thị hình thể chính xác."); return; }
            Map<String, Object> patch = new HashMap<>();
            patch.put("ho_ten", n);
            patch.put("so_dien_thoai", p);
            patch.put("gioi_tinh", genderSpinner.getSelectedItemPosition() == 2 ? "nu" : "nam");
            kho.capNhat("nguoi_dung", uid, patch, new KhoDuLieu.KetQua<Boolean>() {
                @Override public void thanhCong(Boolean ok) {
                    nguoiDung.putAll(patch);
                    thongBao(getString(R.string.profile_saved));
                    hienHoSo();
                }
                @Override public void thatBai(String error) { baoLoi(error); }
            });
        });
        Button save = (Button) thanTrang.getChildAt(thanTrang.getChildCount() - 1);
        save.setText("▣    LƯU THAY ĐỔI");
        save.setTextSize(16);
        save.setMinHeight(dp(58));
        save.setBackground(taoNenGradient(new int[]{0xFFFF2635, 0xFFCE0714, 0xFF9E0710}, 32, 0xFFFF5962));
        Button logout = taoNut("⇥   ĐĂNG XUẤT", false, this::xuLyDangXuat);
        logout.setTextColor(0xFFFF7777);
        logout.setBackground(nenVien(0xFF1A1417, 0xFF9E303A, 30));
        logout.setMinHeight(dp(52));
        LinearLayout.LayoutParams logoutP = new LinearLayout.LayoutParams(-1, -2);
        logoutP.topMargin = dp(2);
        thanTrang.addView(logout, logoutP);
        TextView footer = chu("GYMMANAGEMENT  •  THÔNG TIN ĐƯỢC BẢO MẬT", 9, 0xFF796D70, true);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerP = new LinearLayout.LayoutParams(-1, -2);
        footerP.topMargin = dp(18);
        thanTrang.addView(footer, footerP);
    }

    private EditText themTruongHoSo(LinearLayout parent, String icon, String label, String hint,
                                    int inputType, String value, boolean editable) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(13), dp(8), dp(13), dp(8));
        row.setBackground(nenVien(0xFF211A1D, editable ? 0xFFE53946 : 0xFF51484B, 17));
        TextView glyph = chu(icon, 22, editable ? 0xFFFF7777 : 0xFF999397, true);
        glyph.setGravity(Gravity.CENTER);
        row.addView(glyph, new LinearLayout.LayoutParams(dp(34), dp(42)));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(9), dp(2), 0, dp(2));
        TextView caption = chu(label, 11, 0xFFBDB0B2, false);
        column.addView(caption);
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(15);
        edit.setTextColor(TRANG);
        edit.setHintTextColor(0xFF81777A);
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setText(value == null ? "" : value);
        edit.setPadding(0, dp(4), 0, 0);
        edit.setBackgroundColor(Color.TRANSPARENT);
        edit.setEnabled(editable);
        column.addView(edit, new LinearLayout.LayoutParams(-1, -2));
        row.addView(column, new LinearLayout.LayoutParams(0, -2, 1));
        if (editable) row.addView(chu("✎", 19, 0xFFFF7777, true));
        else row.addView(chu("🔒", 15, 0xFF8D8588, false));
        LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(-1, -2);
        rowP.bottomMargin = dp(11);
        parent.addView(row, rowP);
        return edit;
    }
    // ---------------------------------------------------------------------
    // ĐIỀU HƯỚNG BACK
    // ---------------------------------------------------------------------

    @Override
    public void onBackPressed() {
        String editProfileTitle = getString(R.string.profile_edit).toUpperCase(new Locale("vi", "VN"));
        if (editProfileTitle.equals(manHinhHienTai)) {
            hienHoSo();
            return;
        }
        if (TextUtils.isEmpty(uid)) {
            if (!"GYM MANAGEMENT".equals(manHinhHienTai)) hienDangNhap();
            else super.onBackPressed();
        } else {
            if ("Trang chủ".equals(manHinhHienTai) || manHinhHienTai.startsWith("Xin chào")) super.onBackPressed();
            else hienBangDieuKhien();
        }
    }

    private String decimal(Object value) {
        if (value == null) return "—";
        try {
            double d = Double.parseDouble(String.valueOf(value));
            return String.format(Locale.getDefault(), "%.1f", d);
        } catch (Exception e) { return KhoDuLieu.chuoi(value); }
    }

    private double doubleValue(Object value) {
        try { return Double.parseDouble(KhoDuLieu.chuoi(value)); } catch (Exception e) { return 0; }
    }

    private void ghiNhanTienDo(Map<String, Object> item) {
        String tenBuoi = KhoDuLieu.chuoi(item.get("ten_lich"));
        String scheduleId = KhoDuLieu.chuoi(item.get("id"));

        EditText duration = new EditText(this);
        duration.setHint("Thời lượng (phút)");
        duration.setInputType(InputType.TYPE_CLASS_NUMBER);
        duration.setText("45");
        new AlertDialog.Builder(this).setTitle("Ghi nhận buổi tập")
                .setMessage("Tên buổi: " + tenBuoi)
                .setView(duration)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    long minutes;
                    try { minutes = Long.parseLong(duration.getText().toString().trim()); }
                    catch (Exception e) { baoLoi("Thời lượng không hợp lệ."); return; }
                    if (minutes < 1 || minutes > 600) { baoLoi("Thời lượng phải từ 1 đến 600 phút."); return; }

                    if (!scheduleId.isEmpty()) {
                        Map<String, Object> patch = new HashMap<>();
                        patch.put("da_hoan_thanh", true);
                        kho.capNhat("lich_tap", scheduleId, patch, null);
                    }

                    Map<String, Object> d = new HashMap<>();
                    d.put("uid_nguoi_dung", uid);
                    d.put("gym_id", gymId());
                    d.put("ten_buoi_tap", tenBuoi);
                    d.put("thoi_luong_phut", minutes);
                    d.put("ngay", ngayHomNay());
                    d.put("ghi_chu", "Được ghi nhận từ lịch tập.");
                    d.put("ngay_tao", System.currentTimeMillis());
                    kho.them("tien_do", d, new KhoDuLieu.KetQua<String>() {
                        @Override public void thanhCong(String id) {
                            thongBao("🎉 Đã hoàn thành lịch tập và ghi nhận tiến độ!");
                            hienLichTap();
                        }
                        @Override public void thatBai(String error) { baoLoi(error); }
                    });
                }).show();
    }
}
