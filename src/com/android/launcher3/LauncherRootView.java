package com.android.launcher3;

import static com.android.launcher3.config.FeatureFlags.SEPARATE_RECENTS_ACTIVITY;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.AttributeSet;
import android.view.ViewDebug;
import android.view.WindowInsets;

import com.android.launcher3.graphics.SysUiScrim;
import com.android.launcher3.statemanager.StatefulActivity;
import com.hoko.blur.HokoBlur;
import com.patrykmichalik.opto.core.PreferenceExtensionsKt;
import com.android.launcher3.util.window.WindowManagerProxy;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import app.lawnchair.preferences.PreferenceManager;
import app.lawnchair.preferences2.PreferenceManager2;
import app.lawnchair.util.FileAccessManager;
import app.lawnchair.util.FileAccessState;
import app.lawnchair.util.DeviceTier;
import app.lawnchair.util.DeviceTierManager;

public class LauncherRootView extends InsettableFrameLayout {
    private static final String TAG = "OmniWallpaperBlur";
    // Keep one queued wallpaper at most; obsolete jobs are replaced on recreation.
    private static final ThreadPoolExecutor BLUR_EXECUTOR = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1),
            runnable -> {
                Thread thread = new Thread(runnable, TAG);
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy());
    private int mBlurGeneration;

    private final Rect mTempRect = new Rect();

    private final StatefulActivity mActivity;

    @ViewDebug.ExportedProperty(category = "launcher")
    private static final List<Rect> SYSTEM_GESTURE_EXCLUSION_RECT = Collections.singletonList(new Rect());

    private WindowStateListener mWindowStateListener;
    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mDisallowBackGesture;
    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mForceHideBackArrow;

    private final SysUiScrim mSysUiScrim;
    private final boolean mEnableTaskbarOnPhone;

    private final PreferenceManager pref;

    public LauncherRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = StatefulActivity.fromContext(context);
        mSysUiScrim = new SysUiScrim(this);

        pref = PreferenceManager.getInstance(context);
        PreferenceManager2 prefs2 = PreferenceManager2.getInstance(context);
        
        mEnableTaskbarOnPhone = PreferenceExtensionsKt.firstBlocking(prefs2.getEnableTaskbarOnPhone());

        FileAccessManager fileAccessManager = FileAccessManager.getInstance(context);
        FileAccessState wallpaperAccessState = fileAccessManager.getWallpaperAccessState().getValue();
        if (pref.getEnableWallpaperBlur().get()
                && wallpaperAccessState != FileAccessState.Denied.INSTANCE
                && DeviceTierManager.getInstance(context).getTier() != DeviceTier.LOW) {
            setUpBlur(context);
        }
    }

    private void setUpBlur(Context context) {
        // Only read view-bound state on main. Decode, draw and blur outside the first frame.
        var profile = mActivity.getDeviceProfile();
        final int width = Math.max(1, profile.widthPx / 4);
        final int height = Math.max(1, profile.heightPx / 4);
        final int radius = pref.getWallpaperBlur().get();
        final int sampleFactor = Math.max(1, pref.getWallpaperBlurFactorThreshold().get());
        final int generation = ++mBlurGeneration;
        final Context appContext = context.getApplicationContext();
        final WeakReference<LauncherRootView> rootRef = new WeakReference<>(this);

        BLUR_EXECUTOR.execute(() -> {
            Bitmap source = null;
            Bitmap output = null;
            boolean handedOff = false;
            try {
                Drawable wallpaper = WallpaperManager.getInstance(appContext).getDrawable();
                if (wallpaper == null) return;

                source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(source);
                wallpaper.setBounds(0, 0, width, height);
                wallpaper.draw(canvas);
                Paint overlay = new Paint();
                overlay.setColor(Color.WHITE);
                overlay.setAlpha(51);
                canvas.drawRect(0, 0, width, height, overlay);

                output = HokoBlur.with(appContext)
                        .forceCopy(true)
                        .scheme(HokoBlur.SCHEME_OPENGL)
                        .sampleFactor(sampleFactor)
                        .radius(radius)
                        .blur(source);
                if (output == null) return;

                LauncherRootView root = rootRef.get();
                if (root == null) return;
                final Bitmap finished = output;
                handedOff = root.post(() -> {
                    if (root.mBlurGeneration == generation && root.isAttachedToWindow()) {
                        root.setBackground(new BitmapDrawable(root.getResources(), finished));
                    } else {
                        finished.recycle();
                    }
                });
            } catch (Exception | OutOfMemoryError error) {
                // Wallpaper effects must never prevent Launcher3 from rendering home.
                Log.w(TAG, "Wallpaper blur unavailable; using normal background", error);
            } finally {
                if (source != null && (!handedOff || source != output)) source.recycle();
                if (!handedOff && output != null && output != source) output.recycle();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        ++mBlurGeneration;
        super.onDetachedFromWindow();
    }

    private void handleSystemWindowInsets(Rect insets) {
        // Update device profile before notifying the children.
        mActivity.getDeviceProfile().updateInsets(insets);
        boolean resetState = !insets.equals(mInsets);
        setInsets(insets);

        if (resetState) {
            mActivity.getStateManager().reapplyState(true /* cancelCurrentAnimation */);
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        mActivity.handleConfigurationChanged(mActivity.getResources().getConfiguration());

        insets = WindowManagerProxy.INSTANCE.get(getContext())
                .normalizeWindowInsets(getContext(), insets, mTempRect);
        handleSystemWindowInsets(mTempRect);
        return insets;
    }

    @Override
    public void setInsets(Rect insets) {
        // If the insets haven't changed, this is a no-op. Avoid unnecessary layout
        // caused by
        // modifying child layout params.
        if (!insets.equals(mInsets)) {
            super.setInsets(insets);
            mSysUiScrim.onInsetsChanged(insets);
        }
    }

    public void dispatchInsets() {
        mActivity.getDeviceProfile().updateInsets(mInsets);
        super.setInsets(mInsets);
    }

    public void setWindowStateListener(WindowStateListener listener) {
        mWindowStateListener = listener;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (mWindowStateListener != null) {
            mWindowStateListener.onWindowFocusChanged(hasWindowFocus);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (mWindowStateListener != null) {
            mWindowStateListener.onWindowVisibilityChanged(visibility);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mSysUiScrim.draw(canvas);
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mSysUiScrim.setSize(r - l, b - t);
    }

    public void setForceHideBackArrow(boolean forceHideBackArrow) {
        this.mForceHideBackArrow = forceHideBackArrow;
        setDisallowBackGesture(mDisallowBackGesture);
    }

    public void setDisallowBackGesture(boolean disallowBackGesture) {
        if (SEPARATE_RECENTS_ACTIVITY.get()) {
            return;
        }
        mDisallowBackGesture = disallowBackGesture;
        if (Utilities.ATLEAST_Q) {
            setSystemGestureExclusionRects((mForceHideBackArrow || mDisallowBackGesture)
                    ? SYSTEM_GESTURE_EXCLUSION_RECT
                    : Collections.emptyList());
        }
    }

    public SysUiScrim getSysUiScrim() {
        return mSysUiScrim;
    }

    public interface WindowStateListener {

        void onWindowFocusChanged(boolean hasFocus);

        void onWindowVisibilityChanged(int visibility);
    }
}
