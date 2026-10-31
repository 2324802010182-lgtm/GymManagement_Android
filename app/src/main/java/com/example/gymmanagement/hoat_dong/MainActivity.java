package com.example.gymmanagement.hoat_dong;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.example.gymmanagement.util.ImageLoader;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private LinearLayout exerciseFilterHost;
    private LinearLayout exerciseListHost;
    private TextView exerciseCount;
    private VideoPlayerView activeExerciseVideo;

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
        root.setBackgroundColor(0xFFE0F2F1);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        LinearLayout thanhTieuDe = new LinearLayout(this);
        thanhTieuDe.setOrientation(LinearLayout.HORIZONTAL);
        thanhTieuDe.setGravity(Gravity.CENTER_VERTICAL);
        thanhTieuDe.setPadding(dp(16), dp(12), dp(16), dp(12));

        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF334155, 0xFF475569, 0xFF334155}
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
            TextView bell = new TextView(this);
            bell.setText("🔔");
            bell.setTextSize(20);
            bell.setGravity(Gravity.CENTER);
            bell.setPadding(dp(10), dp(6), dp(10), dp(6));
            bell.setOnClickListener(v -> hienThongBao());
            thanhTieuDe.addView(bell);
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
                new int[]{0xFF334155, 0xFF475569, 0xFF334155}
        );
        nav.setBackground(navBg);
        nav.setPadding(dp(5), dp(7), dp(5), dp(7));
        String[] labels = laQuanTri()
                ? new String[]{"Trang chủ", "Gói tập", "Hội viên", "Hồ sơ"}
                : new String[]{"Trang chủ", "Gói tập", "Lịch tập", "Hồ sơ"};
        Runnable[] actions = laQuanTri()
                ? new Runnable[]{this::hienBangDieuKhien, this::hienGoiTap, this::hienHoiVien, this::hienHoSo}
                : new Runnable[]{this::hienBangDieuKhien, this::hienDanhSachGymDangKyGoi, this::hienLichTap, this::hienHoSo};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText(labels[i]);
            b.setTextSize(11);
            boolean isActiveTab = (index == 0 && ("Trang chủ".equals(manHinhHienTai) || manHinhHienTai.startsWith("Xin chào") || manHinhHienTai.equals(tenNguoiDung())))
                    || labels[i].equalsIgnoreCase(manHinhHienTai);
            b.setTextColor(isActiveTab ? 0xFFFF5252 : 0xFFCBD5E1);
            b.setMinHeight(dp(44));
            b.setPadding(dp(2), 0, dp(2), 0);
            b.setBackground(nenVien(isActiveTab ? 0xFF1E293B : 0xFF334155, 0xFF475569, 12));
            b.setOnClickListener(v -> actions[index].run());
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1));
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

        EditText tuKhoa = truong("Tìm kiếm phòng Gym", "Nhập tên thương hiệu",
                InputType.TYPE_CLASS_TEXT, "");
        EditText diaChiLoc = truong("Lọc theo địa chỉ", "Ví dụ: Thủ Dầu Một, Bình Dương",
                InputType.TYPE_CLASS_TEXT, "");
        Button nutTim = nut("TÌM KIẾM / LỌC", true, null);

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
                    tuKhoa.getText().toString(), diaChiLoc.getText().toString(), ketQua);
        });

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
                        tuKhoa.getText().toString(), diaChiLoc.getText().toString(), ketQua);
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
    private void hienThiDanhSachGymDangKyGoi(
            List<Map<String, Object>> dsGym,
            String tuKhoa,
            String diaChiLoc,
            LinearLayout ketQua) {

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

            LinearLayout theGym = theTrong(ketQua, ten,
                    "Địa chỉ: " + (diaChi.isEmpty() ? "Chưa cập nhật" : diaChi)
                            + "\nSố điện thoại: " + (dienThoai.isEmpty() ? "Chưa cập nhật" : dienThoai));
            themNutVaoThe(theGym, "XEM CÁC GÓI TẬP", true,
                    () -> hienGoiTapCuaGym(gym));
            soKetQua++;
        }

        if (soKetQua == 0) {
            ketQua.addView(chu("Không tìm thấy phòng Gym phù hợp. Hãy thử tên hoặc địa chỉ khác.",
                    13, XAM, false));
        } else {
            ketQua.addView(chu("Tìm thấy " + soKetQua + " phòng Gym.", 12, XAM, false));
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

        taoTrang("CÁC GÓI TẬP", tenGym,
                this::hienDanhSachGymDangKyGoi, true);
        the("THÔNG TIN PHÒNG GYM",
                "Tên: " + tenGym
                        + "\nĐịa chỉ: " + (diaChi.isEmpty() ? "Chưa cập nhật" : diaChi)
                        + "\nSố điện thoại: " + (dienThoai.isEmpty() ? "Chưa cập nhật" : dienThoai));

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

                        for (Map<String, Object> pack : goiHoatDong) {
                            String tenGoi = KhoDuLieu.chuoi(pack.get("ten_goi"));
                            String moTa = KhoDuLieu.chuoi(pack.get("mo_ta"));
                            String chiTiet = "Giá: " + tien(laySo(pack, "gia"))
                                    + "\nThời hạn: " + laySo(pack, "so_ngay") + " ngày"
                                    + (moTa.isEmpty() ? "" : "\nMô tả: " + moTa);
                            LinearLayout theGoi = the(tenGoi, chiTiet);
                            themNutVaoThe(theGoi, "ĐĂNG KÝ GÓI NÀY", true,
                                    () -> dangKyGoi(pack));
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
        taoTrang("GÓI TẬP", tenGymHienTai, this::hienBangDieuKhien, true);
        if (laQuanTri()) nut("+ THÊM GÓI TẬP", true, () -> hienFormGoiTap(null));
        else themChu("Các gói tập thuộc thương hiệu " + tenGymHienTai, 13, XAM, false);
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
                    LinearLayout empty = the("Chưa có gói tập", laQuanTri() ? "Hãy tạo gói đầu tiên. Giá và thời hạn do chủ Gym tự thiết lập." : "Gym hiện chưa có gói tập khả dụng.");
                    if (laQuanTri()) themNutVaoThe(empty, "TẠO GÓI TẬP ĐẦU TIÊN", true, () -> hienFormGoiTap(null));
                    return;
                }
                Collections.sort(filtered, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
                for (Map<String, Object> item : filtered) {
                    String name = KhoDuLieu.chuoi(item.get("ten_goi"));
                    String description = KhoDuLieu.chuoi(item.get("mo_ta"));
                    String details = tien(laySo(item, "gia")) + "\nThời hạn: " + laySo(item, "so_ngay") + " ngày\n" + description + "\nTrạng thái: " + (KhoDuLieu.booleanValue(item.get("trang_thai")) ? "Đang hoạt động" : "Đang ẩn");
                    LinearLayout card = the(name, details);
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
                                    thongBao("Đã gửi yêu cầu. Chủ Gym sẽ xác nhận đăng ký của bạn.");
                                    hienDanhSachGymDangKyGoi();
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
        if (laQuanTri()) nut("+ TẠO LỊCH TẬP & PHÂN CA", true, this::hienFormLichTap);
        else themChu("Lịch các buổi tập do Gym cung cấp", 13, XAM, false);
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
                            String itemGym = KhoDuLieu.chuoi(item.get("gym_id"));
                            if (assignedUid.equals(uid) || (assignedUid.isEmpty() && (!gymId().isEmpty() && gymId().equals(itemGym)))) {
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

        Collections.sort(uncompleted, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
        Collections.sort(completed, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));

        if (uncompleted.isEmpty() && completed.isEmpty()) {
            the("Chưa có lịch tập", laQuanTri() ? "Tạo lịch đầu tiên để hội viên xem." : "Bạn chưa có lịch tập nào.");
            return;
        }

        for (Map<String, Object> item : uncompleted) {
            renderScheduleCard(item, false);
        }

        if (!completed.isEmpty()) {
            themChu("✅ CÁC BÀI TẬP ĐÃ HOÀN THÀNH", 14, 0xFFE53935, true);
            for (Map<String, Object> item : completed) {
                renderScheduleCard(item, true);
            }
        }
    }

    private void renderScheduleCard(Map<String, Object> item, boolean daHoanThanh) {
        String assignedMember = KhoDuLieu.chuoi(item.get("ten_hoi_vien"));
        String details = "📅 Ngày: " + KhoDuLieu.chuoi(item.get("ngay")) + "\n⏰ Giờ: " + KhoDuLieu.chuoi(item.get("gio"))
                + (laQuanTri() && !assignedMember.isEmpty() ? "\n👤 Dành cho: " + assignedMember : "")
                + "\n📝 Nội dung:\n" + KhoDuLieu.chuoi(item.get("mo_ta"))
                + (KhoDuLieu.chuoi(item.get("huong_dan_an_uong")).isEmpty() ? "" : "\n🥗 Dinh dưỡng:\n" + KhoDuLieu.chuoi(item.get("huong_dan_an_uong")))
                + (daHoanThanh ? "\n\n✅ TRẠNG THÁI: ĐÃ HOÀN THÀNH LỊCH TẬP NÀY" : "");

        LinearLayout card = the(KhoDuLieu.chuoi(item.get("ten_lich")), details);
        if (laQuanTri()) {
            themNutVaoThe(card, "XÓA LỊCH", false, () -> new AlertDialog.Builder(MainActivity.this).setTitle("Xóa lịch tập?").setNegativeButton("Hủy", null).setPositiveButton("Xóa", (d, w) -> kho.xoa("lich_tap", KhoDuLieu.chuoi(item.get("id")), new KhoDuLieu.KetQua<Boolean>() {
                @Override public void thanhCong(Boolean ok) { hienLichTap(); }
                @Override public void thatBai(String error) { baoLoi(error); }
            })).show());
        } else {
            if (daHoanThanh) {
                Button btnDone = taoNutNho("✅ ĐÃ HOÀN THÀNH LỊCH TẬP NÀY", false, null);
                btnDone.setEnabled(false);
                card.addView(btnDone);
            } else {
                themNutVaoThe(card, "BẮT ĐẦU TẬP LUYỆN", true, () -> hienGiaoDienTapLuyen(item));
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
        EditText name = truong("Tên buổi tập", "Ví dụ: Cardio & Đẩy ngực", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, "");
        EditText date = truong("Ngày (YYYY-MM-DD)", ngayHomNay(), InputType.TYPE_CLASS_TEXT, ngayHomNay());
        EditText time = truong("Giờ bắt đầu", "18:30", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME, "18:30");

        selectedMemberUid = "";
        selectedMemberName = "Tất cả hội viên (Công khai)";
        Button btnMember = taoNut("👤 Hội viên được phân: " + selectedMemberName, false, null);
        btnMember.setOnClickListener(v -> {
            kho.layTheoTruong("nguoi_dung", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                @Override public void thanhCong(List<Map<String, Object>> members) {
                    List<String> names = new ArrayList<>();
                    List<String> uids = new ArrayList<>();
                    names.add("Tất cả hội viên (Công khai)");
                    uids.add("");
                    for (Map<String, Object> m : members) {
                        if ("hoi_vien".equals(KhoDuLieu.chuoi(m.get("vai_tro")))) {
                            names.add(KhoDuLieu.chuoi(m.get("ho_ten")) + " (" + KhoDuLieu.chuoi(m.get("email")) + ")");
                            uids.add(KhoDuLieu.chuoi(m.get("id")));
                        }
                    }
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Chọn hội viên nhận lịch tập")
                            .setItems(names.toArray(new String[0]), (d, which) -> {
                                selectedMemberUid = uids.get(which);
                                selectedMemberName = names.get(which);
                                btnMember.setText("👤 Hội viên được phân: " + selectedMemberName);
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
            String t = time.getText().toString().trim();
            if (n.isEmpty()) { name.setError("Nhập tên buổi tập"); return; }
            if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) { date.setError("Dùng định dạng YYYY-MM-DD"); return; }
            if (t.isEmpty()) { time.setError("Nhập giờ bắt đầu"); return; }
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

            String scheduleId = KhoDuLieu.chuoi(safeItem.get("id"));
            if (!scheduleId.isEmpty()) {
                Map<String, Object> patch = new HashMap<>();
                patch.put("da_hoan_thanh", true);
                kho.capNhat("lich_tap", scheduleId, patch, null);
            }

            final long finalActiveSecs = totalActiveWorkoutSeconds;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                hienManHinhTongKet(safeItem, finalActiveSecs);
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

    private void hienManHinhTongKet(Map<String, Object> item, long activeSecs) {
        taoTrang("CHÚC MỪNG HOÀN THÀNH!", KhoDuLieu.chuoi(item.get("ten_lich")), this::hienLichTap, false);

        long minutes = Math.max(1, activeSecs / 60);
        LinearLayout card = the("🎉 BẠN ĐÃ HOÀN THÀNH XUẤT SẮC BUỔI TẬP!",
                "⏱️ Thời gian tập luyện thực tế: " + minutes + " phút (" + activeSecs + " giây)\n\n"
                + "🥗 LỜI KHUYÊN DINH DƯỠNG HÔM NAY:\n" + (KhoDuLieu.chuoi(item.get("huong_dan_an_uong")).isEmpty() ? "Bổ sung đủ nước, ăn nhiều protein và nghỉ ngơi điều độ." : KhoDuLieu.chuoi(item.get("huong_dan_an_uong"))));

        nut("HOÀN THÀNH & LƯU ĐIỂM DANH", true, () -> {
            String scheduleId = KhoDuLieu.chuoi(item.get("id"));
            String tenLich = KhoDuLieu.chuoi(item.get("ten_lich"));

            Map<String, Object> patch = new HashMap<>();
            patch.put("da_hoan_thanh", true);
            if (!scheduleId.isEmpty()) {
                kho.capNhat("lich_tap", scheduleId, patch, null);
            }

            Map<String, Object> checkin = new HashMap<>();
            checkin.put("gym_id", gymId());
            checkin.put("uid_nguoi_dung", uid);
            checkin.put("ten_nguoi_dung", tenNguoiDung());
            checkin.put("ngay", ngayHomNay());
            checkin.put("noi_dung", "Hoàn thành lịch: " + tenLich);
            checkin.put("ngay_tao", System.currentTimeMillis());
            kho.them("diem_danh", checkin, new KhoDuLieu.KetQua<String>() {
                @Override public void thanhCong(String s) {
                    thongBao("✅ Đã lưu điểm danh và hoàn thành lịch tập!");
                    hienLichTap();
                }
                @Override public void thatBai(String error) {
                    thongBao("Đã hoàn thành, nhưng không ghi nhận được điểm danh: " + error);
                    hienLichTap();
                }
            });
        });
    }

    // ---------------------------------------------------------------------
    // F05 - THƯ VIỆN BÀI TẬP
    // ---------------------------------------------------------------------

    private void hienThuVienBaiTap() {
        taoTrang("THƯ VIỆN BÀI TẬP", "Dành cho hội viên và chủ Gym", this::hienBangDieuKhien, true);
        exerciseCategory = "";
        exerciseDifficulty = "";
        exerciseQuery = "";
        exerciseCatalog.clear();
        try {
            exerciseCatalog.addAll(ExerciseCatalog.load(this));
        } catch (Exception e) {
            themChu("Không tải được danh mục bài tập trong ứng dụng.", 14, XAM, false);
            return;
        }

        EditText search = oNhap("Tìm tên bài tập, nhóm cơ hoặc dụng cụ", InputType.TYPE_CLASS_TEXT, "");
        Button searchButton = nut("TÌM KIẾM", false, null);
        exerciseFilterHost = new LinearLayout(this);
        exerciseFilterHost.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(exerciseFilterHost);
        exerciseCount = themChu("", 12, XAM, false);
        exerciseListHost = new LinearLayout(this);
        exerciseListHost.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(exerciseListHost);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                exerciseQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                renderExerciseList();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        searchButton.setOnClickListener(v -> {
            exerciseQuery = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            renderExerciseList();
        });
        renderExerciseFilters();
        renderExerciseList();
    }

    private void renderExerciseFilters() {
        if (exerciseFilterHost == null) return;
        exerciseFilterHost.removeAllViews();
        String[] categories = new String[Exercise.CATEGORIES.length + 1];
        categories[0] = "Tất cả";
        System.arraycopy(Exercise.CATEGORIES, 0, categories, 1, Exercise.CATEGORIES.length);
        String[] difficulties = new String[Exercise.DIFFICULTIES.length + 1];
        difficulties[0] = "Tất cả";
        System.arraycopy(Exercise.DIFFICULTIES, 0, difficulties, 1, Exercise.DIFFICULTIES.length);
        addExerciseChipRow(categories, true);
        addExerciseChipRow(difficulties, false);
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
            if (!exerciseCategory.isEmpty() && !exerciseCategory.equals(exercise.category)) continue;
            if (!exerciseDifficulty.isEmpty() && !exerciseDifficulty.equals(exercise.difficulty)) continue;
            if (!exerciseQuery.isEmpty() && !exercise.searchText().contains(exerciseQuery)) continue;
            filtered.add(exercise);
        }
        exerciseCount.setText(filtered.size() + " bài tập");
        if (filtered.isEmpty()) {
            exerciseListHost.addView(chu("Không tìm thấy bài tập phù hợp. Hãy đổi từ khóa hoặc bộ lọc.", 13, XAM, false));
            return;
        }
        for (Exercise exercise : filtered) addExerciseCard(exercise);
    }

    private void addExerciseCard(Exercise exercise) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(11), dp(11), dp(11), dp(11));
        card.setBackground(nenVien(DEN_THE, Color.rgb(48, 48, 48), 15));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(11);
        exerciseListHost.addView(card, cardParams);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(nenVien(DEN_PHU, Color.rgb(55, 55, 55), 11));
        image.setClipToOutline(true);
        ImageLoader.load(image, exercise.imageUrl, R.drawable.bg_gym_placeholder);
        card.addView(image, new LinearLayout.LayoutParams(dp(88), dp(88)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(11), 0, 0, 0);
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        TextView name = chu(exercise.name, 15, TRANG, true);
        name.setMaxLines(2);
        details.addView(name);
        TextView category = chu(exercise.category + " · " + exercise.difficulty, 12, XAM, false);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(-1, -2);
        categoryParams.topMargin = dp(4);
        details.addView(category, categoryParams);
        TextView equipment = chu("Dụng cụ: " + exercise.equipment, 12, XAM, false);
        equipment.setMaxLines(1);
        equipment.setEllipsize(TextUtils.TruncateAt.END);
        details.addView(equipment);
        TextView dosage = chu(exercise.recommendedSets + " hiệp · " + exercise.recommendedReps + " lần", 11, XAM, false);
        details.addView(dosage);
        if (exercise.hasVideo()) {
            TextView video = chu("▶ Có video hướng dẫn", 11, DO, true);
            details.addView(video);
        }
        if (!exercise.imageAttribution.isEmpty() || !exercise.videoAttribution.isEmpty()) {
            String author = !exercise.imageAttribution.isEmpty()
                    ? exercise.imageAttribution : exercise.videoAttribution;
            String license = !exercise.imageLicense.isEmpty()
                    ? exercise.imageLicense : exercise.videoLicense;
            TextView credit = chu("Media: " + author + " · " + license, 9, XAM, false);
            credit.setMaxLines(1);
            credit.setEllipsize(TextUtils.TruncateAt.END);
            details.addView(credit);
        }
        card.setOnClickListener(v -> xemChiTietBaiTap(exercise));
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
        taoTrang("CHỈ SỐ CƠ THỂ", "Theo dõi thay đổi qua từng lần đo", this::hienBangDieuKhien, true);
        nut("+ CẬP NHẬT CHỈ SỐ", true, this::hienFormChiSo);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(list);
        kho.layTheoTruong("chi_so_co_the", "uid_nguoi_dung", uid, new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> items) {
                Collections.sort(items, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
                if (items.isEmpty()) { the("Chưa có dữ liệu đo", "Nhập cân nặng, chiều cao và các chỉ số có sẵn để bắt đầu theo dõi."); return; }
                Map<String, Object> latest = items.get(0);
                the("CHỈ SỐ GẦN NHẤT", "Cân nặng: " + decimal(latest.get("can_nang")) + " kg\nChiều cao: " + decimal(latest.get("chieu_cao")) + " cm\nBMI: " + decimal(latest.get("bmi")) + "\nTỷ lệ mỡ: " + (KhoDuLieu.chuoi(latest.get("ti_le_mo")).isEmpty() ? "Chưa nhập" : decimal(latest.get("ti_le_mo")) + "%") + "\nNgày đo: " + KhoDuLieu.chuoi(latest.get("ngay_do")));
                themChu("LỊCH SỬ ĐO", 14, XAM, true);
                for (Map<String, Object> item : items) {
                    the(KhoDuLieu.chuoi(item.get("ngay_do")), "Cân nặng " + decimal(item.get("can_nang")) + " kg · BMI " + decimal(item.get("bmi")) + (KhoDuLieu.chuoi(item.get("ti_le_mo")).isEmpty() ? "" : " · Mỡ " + decimal(item.get("ti_le_mo")) + "%"));
                }
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được chỉ số: " + error); }
        });
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
        taoTrang("TIẾN ĐỘ TẬP LUYỆN", "Thống kê dựa trên các bản ghi đã lưu", this::hienBangDieuKhien, true);
        nut("+ GHI NHẬN BUỔI TẬP", true, this::hienFormTienDo);
        kho.layTheoTruong("tien_do", "uid_nguoi_dung", uid, new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> sessions) {
                long totalMinutes = 0;
                for (Map<String, Object> s : sessions) totalMinutes += laySo(s, "thoi_luong_phut");
                the("TỔNG QUAN", "Số buổi đã ghi nhận: " + sessions.size() + "\nTổng thời gian: " + totalMinutes + " phút (" + (totalMinutes / 60) + " giờ " + (totalMinutes % 60) + " phút)");
                kho.layTheoTruong("chi_so_co_the", "uid_nguoi_dung", uid, new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override public void thanhCong(List<Map<String, Object>> metrics) {
                        Collections.sort(metrics, (a, b) -> Long.compare(laySo(a, "ngay_tao"), laySo(b, "ngay_tao")));
                        if (metrics.size() >= 2) {
                            Map<String, Object> first = metrics.get(0), last = metrics.get(metrics.size() - 1);
                            double delta = doubleValue(last.get("can_nang")) - doubleValue(first.get("can_nang"));
                            the("THAY ĐỔI CÂN NẶNG", String.format(Locale.getDefault(), "Ban đầu: %.1f kg\nGần nhất: %.1f kg\nThay đổi: %+.1f kg", doubleValue(first.get("can_nang")), doubleValue(last.get("can_nang")), delta));
                            themChu("LỊCH SỬ CÂN NẶNG", 14, XAM, true);
                            for (Map<String, Object> m : metrics) the(KhoDuLieu.chuoi(m.get("ngay_do")), decimal(m.get("can_nang")) + " kg · BMI " + decimal(m.get("bmi")));
                        } else if (metrics.size() == 1) {
                            the("CHỈ SỐ HIỆN TẠI", "Cân nặng " + decimal(metrics.get(0).get("can_nang")) + " kg · BMI " + decimal(metrics.get(0).get("bmi")) + ". Hãy cập nhật thêm chỉ số theo thời gian để so sánh tiến độ.");
                        } else {
                            the("CHƯA CÓ CHỈ SỐ", "Bạn có thể ghi nhận chỉ số cơ thể để theo dõi thay đổi.");
                        }
                    }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
                themChu("LỊCH SỬ BUỔI TẬP", 14, XAM, true);
                if (sessions.isEmpty()) the("Chưa có buổi tập", "Ghi nhận buổi tập sau khi hoàn thành để thống kê thời gian.");
                Collections.sort(sessions, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
                for (Map<String, Object> s : sessions) the(KhoDuLieu.chuoi(s.get("ten_buoi_tap")), "Ngày: " + KhoDuLieu.chuoi(s.get("ngay")) + "\nThời lượng: " + laySo(s, "thoi_luong_phut") + " phút\n" + KhoDuLieu.chuoi(s.get("ghi_chu")));
            }
            @Override public void thatBai(String error) { baoLoi(error); }
        });
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
        taoTrang("THÔNG BÁO", tenGymHienTai, this::hienBangDieuKhien, true);
        if (laQuanTri()) nut("+ GỬI THÔNG BÁO ĐẾN HỘI VIÊN", true, this::hienFormThongBao);
        kho.layThongBaoHienThi(gymId(), uid, laQuanTri(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> all) {
                if (all == null) all = new ArrayList<>();
                Collections.sort(all, (a, b) -> Long.compare(laySo(b, "ngay_tao"), laySo(a, "ngay_tao")));
                int count = 0;
                for (Map<String, Object> item : all) {
                    String receiver = KhoDuLieu.chuoi(item.get("uid_nhan"));
                    if (!receiver.isEmpty() && !receiver.equals(uid)) continue;
                    count++;
                    boolean read = daDoc(item);
                    LinearLayout card = the(KhoDuLieu.chuoi(item.get("tieu_de")), KhoDuLieu.chuoi(item.get("noi_dung")) + "\n" + ngay(laySo(item, "ngay_tao")) + (read ? "\nĐã đọc" : "\n● Chưa đọc"));
                    if (!read) themNutVaoThe(card, "ĐÁNH DẤU ĐÃ ĐỌC", true, () -> danhDauDaDoc(item));
                    themNutVaoThe(card, "XEM CHI TIẾT", false, () -> hienChiTietThongBao(item));
                }
                if (count == 0) the("Bạn chưa có thông báo", "Thông báo mới từ thương hiệu Gym sẽ hiển thị tại đây.");
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được thông báo: " + error); }
        });
    }

    private void hienChiTietThongBao(Map<String, Object> item) {
        String tieuDe = KhoDuLieu.chuoi(item.get("tieu_de"));
        String noiDung = KhoDuLieu.chuoi(item.get("noi_dung"));
        long thoiGian = laySo(item, "ngay_tao");

        taoTrang("CHI TIẾT THÔNG BÁO", tenGymHienTai, this::hienThongBao, false);

        LinearLayout detailsCard = the(tieuDe, "🕒 Thời gian gửi: " + ngay(thoiGian) + "\n\n📋 NỘI DUNG CHI TIẾT:\n" + noiDung);
        detailsCard.setPadding(dp(20), dp(20), dp(20), dp(20));

        if (!daDoc(item)) {
            nut("ĐÁNH DẤU ĐÃ ĐỌC", true, () -> danhDauDaDoc(item));
        }
        nut("QUAY LẠI", false, this::hienThongBao);
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
        taoTrang("GỬI THÔNG BÁO", "Thông báo sẽ chỉ gửi đến thành viên thuộc " + tenGymHienTai, this::hienThongBao, false);
        EditText title = truong("Tiêu đề", "Ví dụ: Lịch nghỉ lễ", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, "");
        EditText content = truong("Nội dung", "Nhập nội dung thông báo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, "");
        content.setSingleLine(false);
        content.setMinHeight(dp(120));
        nut("GỬI THÔNG BÁO", true, () -> {
            String t = title.getText().toString().trim(), c = content.getText().toString().trim();
            if (t.isEmpty()) { title.setError("Nhập tiêu đề"); return; }
            if (c.isEmpty()) { content.setError("Nhập nội dung"); return; }
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
        });
    }

    // ---------------------------------------------------------------------
    // F09 - QUẢN TRỊ GYM: HỘI VIÊN, YÊU CẦU GÓI, ĐIỂM DANH, THƯƠNG HIỆU
    // ---------------------------------------------------------------------

    private void hienHoiVien() {
        if (!laQuanTri()) { baoLoi("Chức năng này dành cho quản trị Gym."); return; }
        taoTrang("QUẢN LÝ HỘI VIÊN", tenGymHienTai, this::hienBangDieuKhien, true);
        nut("XEM YÊU CẦU ĐĂNG KÝ GÓI", true, this::hienYeuCauGoi);
        nut("ĐIỂM DANH HỘI VIÊN", false, this::hienDiemDanh);

        kho.layTheoTruong("nguoi_dung", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> gymUsers) {
                kho.layTheoTruong("dang_ky_goi", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
                    @Override public void thanhCong(List<Map<String, Object>> registrations) {
                        Map<String, String> memberPackageMap = new HashMap<>();
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
                                if ("dang_hoat_dong".equals(st)) {
                                    memberUids.add(mUid);
                                    memberPackageMap.put(mUid, KhoDuLieu.chuoi(reg.get("ten_goi")));
                                }
                            }
                        }

                        themChu("Tổng hội viên thuộc Gym: " + memberUids.size(), 14, XAM, true);
                        if (memberUids.isEmpty()) {
                            the("Chưa có hội viên", "Khi hội viên đăng ký gói hoặc chọn phòng Gym này, danh sách sẽ xuất hiện ở đây.");
                            return;
                        }

                        for (String mId : memberUids) {
                            kho.layMot("nguoi_dung", mId, new KhoDuLieu.KetQua<Map<String, Object>>() {
                                @Override public void thanhCong(Map<String, Object> member) {
                                    if (member == null) return;
                                    String status = Boolean.FALSE.equals(member.get("trang_thai")) ? "Đã khóa" : "Đang hoạt động";
                                    String packageName = memberPackageMap.containsKey(mId) ? memberPackageMap.get(mId) : "Chưa có gói hoạt động";

                                    LinearLayout card = the(KhoDuLieu.chuoi(member.get("ho_ten")),
                                            "📧 Email: " + KhoDuLieu.chuoi(member.get("email"))
                                                    + "\n📞 SĐT: " + KhoDuLieu.chuoi(member.get("so_dien_thoai"))
                                                    + "\n💳 Gói tập: " + packageName
                                                    + "\n🔒 Trạng thái: " + status);

                                    themNutVaoThe(card, "📅 PHÂN LỊCH TẬP CHO HỘI VIÊN NÀY", true, () -> hienFormLichTapChoThanhVien(mId, KhoDuLieu.chuoi(member.get("ho_ten"))));
                                    themNutVaoThe(card, Boolean.FALSE.equals(member.get("trang_thai")) ? "MỞ KHÓA TÀI KHOẢN" : "KHÓA TÀI KHOẢN", false, () -> {
                                        Map<String, Object> patch = new HashMap<>();
                                        patch.put("trang_thai", !Boolean.FALSE.equals(member.get("trang_thai")));
                                        kho.capNhat("nguoi_dung", mId, patch, new KhoDuLieu.KetQua<Boolean>() {
                                            @Override public void thanhCong(Boolean ok) { hienHoiVien(); }
                                            @Override public void thatBai(String error) { baoLoi(error); }
                                        });
                                    });
                                }
                                @Override public void thatBai(String error) {}
                            });
                        }
                    }
                    @Override public void thatBai(String error) { baoLoi(error); }
                });
            }
            @Override public void thatBai(String error) { baoLoi("Không tải được hội viên: " + error); }
        });
    }

    private void hienFormLichTapChoThanhVien(String memberUid, String memberName) {
        selectedMemberUid = memberUid;
        selectedMemberName = memberName + " (Hội viên)";
        hienFormLichTap();
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
        taoTrang("ĐIỂM DANH & CHUYÊN CẦN", tenGymHienTai, this::hienBangDieuKhien, true);
        themChu("DANH SÁCH HỘI VIÊN VÀ CHỈ SỐ ĐIỂM DANH (1-30 NGÀY/THÁNG)", 13, 0xFFE53935, true);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        thanTrang.addView(list);

        kho.layTheoTruong("diem_danh", "gym_id", gymId(), new KhoDuLieu.KetQua<List<Map<String, Object>>>() {
            @Override public void thanhCong(List<Map<String, Object>> allCheckins) {
                Map<String, List<Map<String, Object>>> memberCheckinMap = new HashMap<>();
                if (allCheckins != null) {
                    for (Map<String, Object> ci : allCheckins) {
                        String mUid = KhoDuLieu.chuoi(ci.get("uid_nguoi_dung"));
                        if (mUid.isEmpty()) mUid = KhoDuLieu.chuoi(ci.get("uid_hoi_vien"));
                        if (!mUid.isEmpty()) {
                            if (!memberCheckinMap.containsKey(mUid)) memberCheckinMap.put(mUid, new ArrayList<>());
                            memberCheckinMap.get(mUid).add(ci);
                        }
                    }
                }

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

                        if (memberNameMap.isEmpty()) {
                            the("Chưa có lượt điểm danh", "Chưa có lượt điểm danh nào từ hội viên.");
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
                            bg.setColor(0xFFFFFFFF);
                            bg.setCornerRadius(dp(18));
                            bg.setStroke(dp(1), 0xFFB2DFDB);
                            card.setBackground(bg);

                            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                            cp.bottomMargin = dp(14);
                            list.addView(card, cp);

                            TextView tvName = chu("👤 Hội viên: " + mName, 17, 0xFF0F172A, true);
                            card.addView(tvName);

                            TextView tvStreak = chu("🏃 Chuyên cần tháng này: " + daysCount + "/30 ngày tập", 13, 0xFFE53935, true);
                            tvStreak.setPadding(0, dp(4), 0, dp(8));
                            card.addView(tvStreak);

                            ProgressBar bar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleHorizontal);
                            bar.setMax(30);
                            bar.setProgress(daysCount);
                            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFE53935));
                            bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
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
        EditText name = truong("Tên thương hiệu Gym", "Tên thương hiệu", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, KhoDuLieu.chuoi(gym.get("ten_thuong_hieu")));
        EditText address = truong("Địa chỉ", "Địa chỉ", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, KhoDuLieu.chuoi(gym.get("dia_chi")));
        EditText phone = truong("Số điện thoại", "0123456789", InputType.TYPE_CLASS_PHONE, KhoDuLieu.chuoi(gym.get("so_dien_thoai")));
        nut("LƯU THÔNG TIN", true, () -> {
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
        the("VAI TRÒ", laQuanTri() ? "Quản trị Gym" : "Hội viên");
        if (laQuanTri()) the("THƯƠNG HIỆU GYM", tenGymHienTai);
        EditText name = truong("Họ và tên", "Họ tên", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, KhoDuLieu.chuoi(nguoiDung.get("ho_ten")));
        EditText email = truong("Email tài khoản", "Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, KhoDuLieu.chuoi(nguoiDung.get("email")));
        email.setEnabled(false);
        email.setAlpha(0.7f);
        EditText phone = truong("Số điện thoại", "0123456789", InputType.TYPE_CLASS_PHONE, KhoDuLieu.chuoi(nguoiDung.get("so_dien_thoai")));
        nut("LƯU HỒ SƠ", true, () -> {
            String n = name.getText().toString().trim(), p = phone.getText().toString().trim();
            if (n.isEmpty()) { name.setError("Vui lòng nhập họ tên"); return; }
            if (!p.matches("[0-9]{9,11}")) { phone.setError("Số điện thoại gồm 9–11 chữ số"); return; }
            Map<String, Object> patch = new HashMap<>();
            patch.put("ho_ten", n);
            patch.put("so_dien_thoai", p);
            kho.capNhat("nguoi_dung", uid, patch, new KhoDuLieu.KetQua<Boolean>() {
                @Override public void thanhCong(Boolean ok) {
                    nguoiDung.putAll(patch);
                    thongBao(getString(R.string.profile_saved));
                    hienHoSo();
                }
                @Override public void thatBai(String error) { baoLoi(error); }
            });
        });
        nut("ĐỔI / ĐẶT LẠI MẬT KHẨU", false, this::guiEmailDatLaiMatKhauHoSo);
        nut("ĐĂNG XUẤT", false, this::xuLyDangXuat);
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
