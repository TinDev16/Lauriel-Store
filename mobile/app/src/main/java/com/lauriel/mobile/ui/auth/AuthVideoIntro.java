package com.lauriel.mobile.ui.auth;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import com.lauriel.mobile.R;

final class AuthVideoIntro {

    private static final String KEY_DONE = "authIntroDone";
    private static final long INTRO_DURATION_MS = 3000L;

    private final Activity activity;
    private final TextureView video;
    private final View form;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private boolean done;
    private int videoWidth;
    private int videoHeight;

    private final Runnable reveal = new Runnable() {
        @Override
        public void run() {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            done = true;
            form.setVisibility(View.VISIBLE);
            form.setTranslationY(dp(20));
            form.setAlpha(0f);
            form.animate().alpha(1f).translationY(0f).setDuration(650L).start();
        }
    };

    AuthVideoIntro(
        Activity activity,
        TextureView video,
        View form,
        Bundle state
    ) {
        this.activity = activity;
        this.video = video;
        this.form = form;
        done = state != null && state.getBoolean(KEY_DONE, false);
        form.setVisibility(done ? View.VISIBLE : View.INVISIBLE);
        form.setAlpha(done ? 1f : 0f);

        video.setOpaque(true);
        video.setSurfaceTextureListener(
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                    SurfaceTexture texture,
                    int width,
                    int height
                ) {
                    prepare(texture);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                    SurfaceTexture texture,
                    int width,
                    int height
                ) {
                    applyCenterCrop();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(
                    SurfaceTexture texture
                ) {
                    releasePlayer();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture texture) {}
            }
        );
        if (video.isAvailable()) prepare(video.getSurfaceTexture());
    }

    private void prepare(SurfaceTexture texture) {
        releasePlayer();
        if (texture == null) {
            reveal.run();
            return;
        }
        try {
            player = MediaPlayer.create(
                activity,
                R.raw.lauriel_auth_background
            );
            if (player == null) {
                reveal.run();
                return;
            }
            Surface surface = new Surface(texture);
            player.setSurface(surface);
            surface.release();
            player.setVolume(0f, 0f);
            player.setLooping(true);
            videoWidth = player.getVideoWidth();
            videoHeight = player.getVideoHeight();
            video.post(this::applyCenterCrop);
            player.setOnVideoSizeChangedListener((mp, width, height) -> {
                videoWidth = width;
                videoHeight = height;
                applyCenterCrop();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                reveal.run();
                return true;
            });

            if (done) {
                player.setOnSeekCompleteListener(MediaPlayer::start);
                player.seekTo(INTRO_DURATION_MS, MediaPlayer.SEEK_CLOSEST);
            } else {
                player.start();
                handler.removeCallbacks(reveal);
                handler.postDelayed(reveal, INTRO_DURATION_MS);
            }
        } catch (RuntimeException error) {
            releasePlayer();
            reveal.run();
        }
    }

    private void applyCenterCrop() {
        if (
            videoWidth <= 0 ||
            videoHeight <= 0 ||
            video.getWidth() <= 0 ||
            video.getHeight() <= 0
        ) return;
        float scale = Math.max(
            (float) video.getWidth() / videoWidth,
            (float) video.getHeight() / videoHeight
        );
        float scaleX = (scale * videoWidth) / video.getWidth();
        float scaleY = (scale * videoHeight) / video.getHeight();
        Matrix matrix = new Matrix();
        matrix.setScale(
            scaleX,
            scaleY,
            video.getWidth() / 2f,
            video.getHeight() / 2f
        );
        video.setTransform(matrix);
    }

    void save(Bundle out) {
        out.putBoolean(KEY_DONE, done);
    }

    void destroy() {
        handler.removeCallbacksAndMessages(null);
        video.setSurfaceTextureListener(null);
        releasePlayer();
    }

    private void releasePlayer() {
        if (player == null) return;
        try {
            player.setSurface(null);
            player.release();
        } catch (RuntimeException ignored) {}
        player = null;
    }

    private int dp(int value) {
        return Math.round(
            value * activity.getResources().getDisplayMetrics().density
        );
    }
}
