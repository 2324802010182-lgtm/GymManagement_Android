package com.example.gymmanagement.ui.components;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.gymmanagement.R;
import com.example.gymmanagement.util.ImageLoader;

/**
 * Trình phát video hướng dẫn (AndroidX Media3 ExoPlayer): Play/Pause, tua, thời lượng, fullscreen,
 * trạng thái tải, lỗi + nút thử lại. Không tự phát; giải phóng player khi dừng/hủy.
 */
@OptIn(markerClass = UnstableApi.class)
public class VideoPlayerView extends FrameLayout {
    private PlayerView playerView;
    private ProgressBar loading;
    private View errorBox;
    private TextView errorText;
    private ImageView poster;
    private View playButton;
    private ExoPlayer player;
    private String url = "";
    private String type = "";
    private long resumePosition;
    private boolean started;
    private Dialog fullscreenDialog;

    public VideoPlayerView(@NonNull Context c) {
        super(c);
        init();
    }

    public VideoPlayerView(@NonNull Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_video_player, this);
        playerView = findViewById(R.id.video_player);
        loading = findViewById(R.id.video_loading);
        errorBox = findViewById(R.id.video_error);
        errorText = findViewById(R.id.video_error_text);
        poster = findViewById(R.id.video_poster);
        playButton = findViewById(R.id.video_play);
        findViewById(R.id.video_retry).setOnClickListener(v -> retry());
        poster.setOnClickListener(v -> playVideo());
        playButton.setOnClickListener(v -> playVideo());
        playerView.setFullscreenButtonClickListener(isFullScreen -> toggleFullscreen());
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(w * 9 / 16, MeasureSpec.EXACTLY));
    }

    public void setSource(String videoUrl, String videoType, String posterUrl) {
        url = videoUrl == null ? "" : videoUrl;
        type = videoType == null ? "" : videoType;
        resumePosition = 0;
        boolean hasPoster = posterUrl != null
                && (posterUrl.startsWith("https://") || posterUrl.startsWith("asset://"));
        poster.setVisibility(hasPoster ? VISIBLE : GONE);
        playButton.setVisibility(hasPoster ? VISIBLE : GONE);
        if (hasPoster) ImageLoader.load(poster, posterUrl, R.drawable.bg_gym_placeholder);
        if (started) {
            release();
            create();
        }
    }

    private void playVideo() {
        if (player == null && started) create();
        poster.setVisibility(GONE);
        playButton.setVisibility(GONE);
        if (player != null) player.play();
    }

    /** Gọi trong onStart của Fragment/Activity. */
    public void start() {
        started = true;
        if (player == null) create();
    }

    /** Gọi trong onStop: lưu vị trí, tạm dừng và giải phóng tài nguyên. */
    public void stop() {
        started = false;
        release();
    }

    private void create() {
        if (!url.startsWith("https://")) return;
        showError(null);
        player = new ExoPlayer.Builder(getContext()).build();
        playerView.setPlayer(player);
        MediaItem.Builder b = new MediaItem.Builder().setUri(url);
        if ("hls".equalsIgnoreCase(type)) b.setMimeType(MimeTypes.APPLICATION_M3U8);
        else if ("dash".equalsIgnoreCase(type)) b.setMimeType(MimeTypes.APPLICATION_MPD);
        else if ("mp4".equalsIgnoreCase(type)) b.setMimeType(MimeTypes.VIDEO_MP4);
        else if ("webm".equalsIgnoreCase(type)) b.setMimeType(MimeTypes.VIDEO_WEBM);
        player.setMediaItem(b.build());
        player.setPlayWhenReady(false); // không tự phát âm thanh
        player.seekTo(resumePosition);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                loading.setVisibility(state == Player.STATE_BUFFERING ? VISIBLE : GONE);
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                loading.setVisibility(GONE);
                errorBox.setVisibility(GONE);
                poster.setVisibility(VISIBLE);
                playButton.setVisibility(GONE);
            }
        });
        player.prepare();
        loading.setVisibility(VISIBLE);
    }

    private void retry() {
        release();
        create();
    }

    private void showError(String msg) {
        if (msg == null) {
            errorBox.setVisibility(GONE);
            return;
        }
        errorText.setText(msg);
        errorBox.setVisibility(VISIBLE);
    }

    private void release() {
        closeFullscreen();
        if (player != null) {
            resumePosition = Math.max(0, player.getCurrentPosition());
            player.release();
            player = null;
        }
        playerView.setPlayer(null);
    }

    private void toggleFullscreen() {
        if (fullscreenDialog != null) {
            closeFullscreen();
            return;
        }
        fullscreenDialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        removeView(playerView);
        fullscreenDialog.addContentView(playerView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fullscreenDialog.setOnDismissListener(d -> restoreFromFullscreen());
        fullscreenDialog.show();
    }

    private void closeFullscreen() {
        if (fullscreenDialog != null) {
            Dialog d = fullscreenDialog;
            d.dismiss();
        }
    }

    private void restoreFromFullscreen() {
        if (fullscreenDialog == null) return;
        fullscreenDialog = null;
        if (playerView.getParent() instanceof ViewGroup) ((ViewGroup) playerView.getParent()).removeView(playerView);
        addView(playerView, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}
