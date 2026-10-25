package app.lawnchair.shizuku;

interface IOmniShizukuService {
    void destroy() = 16777114;
    String getStatus();
    boolean applyAnimationPreset(int preset);
}
