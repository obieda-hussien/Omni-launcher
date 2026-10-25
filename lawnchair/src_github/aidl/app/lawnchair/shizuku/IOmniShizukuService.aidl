package app.lawnchair.shizuku;

interface IOmniShizukuService {
    void destroy() = 16777114;
    String getStatus() = 1;
    boolean applyAnimationPreset(int preset) = 2;
}
