package com.hanbang.videoplay.view;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.hanbang.videoplay.JCBuriedPointStandard;
import com.hanbang.videoplay.JCUtils;
import com.hanbang.videoplay.JCVideoPlayer;
import com.hanbang.videoplay.R;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Nathen
 * On 2016/04/18 16:15
 */
public class JCVideoPlayerStandard extends JCVideoPlayer {

    protected static Timer DISSMISS_CONTROL_VIEW_TIMER;

    public ImageView backButton;
    public ProgressBar bottomProgressBar;
    public CircleProgressBar loadingProgressBar;
    public TextView titleTextView;
    public ImageView thumbImageView;
    public ImageView coverImageView;
    public ImageView tinyBackImageView;

    protected DismissControlViewTimerTask mDismissControlViewTimerTask;


    public JCVideoPlayerStandard(Context context) {
        super(context);
    }

    public JCVideoPlayerStandard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        bottomProgressBar = (ProgressBar) findViewById(R.id.bottom_progressbar);
        titleTextView = (TextView) findViewById(R.id.title);
        backButton = (ImageView) findViewById(R.id.back);
        thumbImageView = (ImageView) findViewById(R.id.thumb);
        coverImageView = (ImageView) findViewById(R.id.cover);
        loadingProgressBar = (CircleProgressBar) findViewById(R.id.loading);
        tinyBackImageView = (ImageView) findViewById(R.id.back_tiny);

        thumbImageView.setOnClickListener(this);
        backButton.setOnClickListener(this);
        tinyBackImageView.setOnClickListener(this);

    }

    @Override
    public boolean setUp(String url, int screen, Object... objects) {
        if (objects.length == 0) return false;
        if (super.setUp(url, screen, objects)) {
            titleTextView.setText(objects[0].toString());
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                fullscreenButton.setImageResource(R.drawable.material_exit_full_screen);
                backButton.setVisibility(View.VISIBLE);
                tinyBackImageView.setVisibility(View.GONE);
            } else if (currentScreen == SCREEN_LAYOUT_LIST) {
                fullscreenButton.setImageResource(R.drawable.material_full_screen);
                backButton.setVisibility(View.GONE);
                tinyBackImageView.setVisibility(View.GONE);
            } else if (currentScreen == SCREEN_WINDOW_TINY) {
                tinyBackImageView.setVisibility(View.VISIBLE);
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.GONE, View.GONE, View.GONE, View.GONE);
            }
            return true;
        }
        return false;
    }

    private void setViewStatus() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.video_play_layout_standard;
    }

    @Override
    public void setUiWitStateAndScreen(int state) {
        super.setUiWitStateAndScreen(state);
        switch (currentState) {
            case CURRENT_STATE_NORMAL:
                changeUiToNormal();
                break;
            case CURRENT_STATE_PREPAREING:
                changeUiToPrepareingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING:
                changeUiToPlayingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PAUSE:
                changeUiToPauseShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_ERROR:
                changeUiToError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                changeUiToCompleteShow();
                cancelDismissControlViewTimer();
                bottomProgressBar.setProgress(100);
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                changeUiToPlayingBufferingShow();
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    if (mChangePosition) {
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        bottomProgressBar.setProgress(progress);
                    }
                    if (!mChangePosition && !mChangeVolume) {
                        onEvent(JCBuriedPointStandard.ON_CLICK_BLANK);
                        onClickUiToggle();
                    }
                    break;
            }
        } else if (id == R.id.progress) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelDismissControlViewTimer();
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    break;
            }
        }
        return super.onTouch(v, event);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int i = v.getId();
        if (i == R.id.thumb) {
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(getContext(), "视频地址错误！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL) {
                if (!url.startsWith("file") && !JCUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                startPlayLocic();
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onClickUiToggle();
            }
        } else if (i == R.id.surface_container) {
            startDismissControlViewTimer();
        } else if (i == R.id.back) {
            backPress();
        } else if (i == R.id.back_tiny) {
            backPress();
        }
    }

    @Override
    public void showWifiDialog() {
        super.showWifiDialog();
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .title("友情提示")
                .content("当前为非WIFI网络，是否继续观看")
                .positiveText("继续观看")
                .positiveColor(0xfffcb326)
                .negativeText("残忍拒绝")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        startPlayLocic();
                        WIFI_TIP_DIALOG_SHOWED = true;
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();


        dialog.show();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        cancelDismissControlViewTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        startDismissControlViewTimer();
    }

    public void startPlayLocic() {
        prepareVideo();
        startDismissControlViewTimer();
        onEvent(JCBuriedPointStandard.ON_CLICK_START_THUMB);
    }

    public void onClickUiToggle() {
        if (currentState == CURRENT_STATE_PREPAREING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPrepareingClear();
            } else {
                changeUiToPrepareingShow();
            }
        } else if (currentState == CURRENT_STATE_PLAYING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingClear();
            } else {
                changeUiToPlayingShow();
            }
        } else if (currentState == CURRENT_STATE_PAUSE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPauseClear();
            } else {
                changeUiToPauseShow();
            }
        } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToCompleteClear();
            } else {
                changeUiToCompleteShow();
            }
        } else if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingBufferingClear();
            } else {
                changeUiToPlayingBufferingShow();
            }
        }
    }

    @Override
    public void setProgressAndTime(int progress, int secProgress, int currentTime, int totalTime) {
        super.setProgressAndTime(progress, secProgress, currentTime, totalTime);
        if (progress != 0) bottomProgressBar.setProgress(progress);
        if (secProgress != 0) bottomProgressBar.setSecondaryProgress(secProgress);
    }

    @Override
    public void resetProgressAndTime() {
        super.resetProgressAndTime();
        bottomProgressBar.setProgress(0);
        bottomProgressBar.setSecondaryProgress(0);
    }

    //Unified management Ui
    public void changeUiToNormal() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.GONE, View.VISIBLE,
                        View.GONE, View.VISIBLE, View.VISIBLE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.GONE, View.VISIBLE,
                        View.GONE, View.VISIBLE, View.VISIBLE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    public void changeUiToPrepareingShow() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.GONE,
                        View.VISIBLE, View.GONE, View.VISIBLE, View.GONE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.GONE,
                        View.VISIBLE, View.GONE, View.VISIBLE, View.GONE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPrepareingClear() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.VISIBLE, View.GONE, View.VISIBLE, View.GONE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.VISIBLE, View.GONE, View.VISIBLE, View.GONE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPlayingShow() {

        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.GONE, View.GONE, View.GONE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.GONE, View.GONE, View.GONE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPlayingClear() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.GONE, View.GONE, View.GONE, View.VISIBLE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.GONE, View.GONE, View.GONE, View.VISIBLE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPauseShow() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.GONE, View.GONE, View.GONE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.GONE, View.GONE, View.GONE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPauseClear() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.GONE, View.GONE, View.GONE, View.GONE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPlayingBufferingShow() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.GONE,
                        View.VISIBLE, View.GONE, View.GONE, View.GONE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.GONE,
                        View.VISIBLE, View.GONE, View.GONE, View.GONE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPlayingBufferingClear() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.VISIBLE, View.GONE, View.GONE, View.VISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.GONE, View.GONE, View.GONE,
                        View.VISIBLE, View.GONE, View.GONE, View.VISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToCompleteShow() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.GONE, View.VISIBLE, View.GONE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.GONE, View.VISIBLE, View.GONE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToCompleteClear() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.GONE, View.GONE, View.VISIBLE,
                        View.GONE, View.VISIBLE, View.GONE, View.VISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.GONE, View.GONE, View.VISIBLE,
                        View.GONE, View.VISIBLE, View.GONE, View.VISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToError() {
        switch (currentScreen) {
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.GONE, View.GONE, View.VISIBLE,
                        View.GONE, View.GONE, View.VISIBLE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.GONE, View.GONE, View.VISIBLE,
                        View.GONE, View.GONE, View.VISIBLE, View.GONE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void setAllControlsVisible(int topCon, int bottomCon, int startBtn, int loadingPro,
                                      int thumbImg, int coverImg, int bottomPro) {
        if (topContainer.getVisibility() != GONE && bottomContainer.getVisibility() != GONE && topCon == GONE && bottomCon == GONE) {
            hintContainer();
        } else if (topContainer.getVisibility() != VISIBLE && bottomContainer.getVisibility() != VISIBLE && topCon == VISIBLE && bottomCon == VISIBLE) {
            showContainer();
        }

        startButton.setVisibility(startBtn);
        if (loadingProgressBar.getVisibility() != loadingPro) {
            loadingProgressBar.setVisibility(loadingPro);
        }
        thumbImageView.setVisibility(thumbImg);
        coverImageView.setVisibility(coverImg);
        bottomProgressBar.setVisibility(bottomPro);
    }

    private void hintContainer() {
        animatorShow.cancel();
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(topContainer, "translationY", topContainer.getTranslationY(), -topContainer.getHeight());
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(bottomContainer, "translationY", bottomContainer.getTranslationY(), bottomContainer.getHeight());
        animatorHint.play(animator1).with(animator2);
        animatorHint.start();
    }

    private void showContainer() {
        animatorHint.cancel();
        topContainer.setVisibility(VISIBLE);
        bottomContainer.setVisibility(VISIBLE);
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(topContainer, "translationY", topContainer.getTranslationY(), 0);
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(bottomContainer, "translationY", bottomContainer.getTranslationY(), 0);
        animatorShow.play(animator1).with(animator2);
        animatorShow.start();
    }

    private void updateStartImage() {
        if (currentState == CURRENT_STATE_PLAYING) {
            startButton.setImageResource(R.drawable.material_video_play_pause);
        } else if (currentState == CURRENT_STATE_ERROR) {
            startButton.setImageResource(R.drawable.material_video_play_warning);
        } else {
            startButton.setImageResource(R.drawable.material_video_play_play);
        }
    }

    protected Dialog mProgressDialog;
    protected ProgressBar mDialogProgressBar;
    protected TextView mDialogSeekTime;
    protected TextView mDialogTotalTime;
    protected ImageView mDialogIcon;

    @Override
    public void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        super.showProgressDialog(deltaX, seekTime, seekTimePosition, totalTime, totalTimeDuration);
        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.video_play_progress_dialog, null);
            View content = localView.findViewById(R.id.content);
            content.setRotation(90);
            mDialogProgressBar = ((ProgressBar) localView.findViewById(R.id.duration_progressbar));
            mDialogSeekTime = ((TextView) localView.findViewById(R.id.tv_current));
            mDialogTotalTime = ((TextView) localView.findViewById(R.id.tv_duration));
            mDialogIcon = ((ImageView) localView.findViewById(R.id.duration_image_tip));
            mProgressDialog = new Dialog(getContext(), R.style.video_play_style_dialog_progress);
            mProgressDialog.setContentView(localView);
            mProgressDialog.getWindow().addFlags(Window.FEATURE_ACTION_BAR);
            mProgressDialog.getWindow().addFlags(32);
            mProgressDialog.getWindow().addFlags(16);
            mProgressDialog.getWindow().setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = mProgressDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
            localLayoutParams.x = getResources().getDimensionPixelOffset(R.dimen.jc_progress_dialog_margin_top) / 2;
            mProgressDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        mDialogSeekTime.setText(seekTime);
        mDialogTotalTime.setText(" / " + totalTime);
        mDialogProgressBar.setProgress(totalTimeDuration <= 0 ? 0 : (seekTimePosition * 100 / totalTimeDuration));
        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.material_video_qianjing);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.material_video_back);
        }

    }

    @Override
    public void dismissProgressDialog() {
        super.dismissProgressDialog();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }


    protected Dialog mVolumeDialog;
    protected ProgressBar mDialogVolumeProgressBar;

    @Override
    public void showVolumDialog(float deltaY, int volumePercent) {
        super.showVolumDialog(deltaY, volumePercent);
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.video_play_volume_dialog, null);
            View content = localView.findViewById(R.id.content);
            content.setRotation(90);
            mDialogVolumeProgressBar = ((ProgressBar) localView.findViewById(R.id.volume_progressbar));
            mVolumeDialog = new Dialog(getContext(), R.style.video_play_style_dialog_progress);
            mVolumeDialog.setContentView(localView);
            mVolumeDialog.getWindow().addFlags(8);
            mVolumeDialog.getWindow().addFlags(32);
            mVolumeDialog.getWindow().addFlags(16);
            mVolumeDialog.getWindow().setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = mVolumeDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
//            localLayoutParams.y = getContext().getResources().getDimensionPixelOffset(R.dimen.jc_volume_dialog_margin_left);
            mVolumeDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }

        mDialogVolumeProgressBar.setProgress(volumePercent);
    }

    @Override
    public void dismissVolumDialog() {
        super.dismissVolumDialog();
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
        }
    }

    public void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        DISSMISS_CONTROL_VIEW_TIMER = new Timer();
        mDismissControlViewTimerTask = new DismissControlViewTimerTask();
        DISSMISS_CONTROL_VIEW_TIMER.schedule(mDismissControlViewTimerTask, 2500);
    }

    public void cancelDismissControlViewTimer() {
        if (DISSMISS_CONTROL_VIEW_TIMER != null) {
            DISSMISS_CONTROL_VIEW_TIMER.cancel();
        }
        if (mDismissControlViewTimerTask != null) {
            mDismissControlViewTimerTask.cancel();
        }

    }

    public class DismissControlViewTimerTask extends TimerTask {

        @Override
        public void run() {
            if (currentState != CURRENT_STATE_NORMAL
                    && currentState != CURRENT_STATE_ERROR
                    && currentState != CURRENT_STATE_AUTO_COMPLETE) {
                if (getContext() != null && getContext() instanceof Activity) {
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hintContainer();
                            startButton.setVisibility(View.GONE);
                            if (currentScreen != SCREEN_WINDOW_TINY) {
                                bottomProgressBar.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }
        }
    }
}
