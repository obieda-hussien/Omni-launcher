package app.lawnchair.shizuku;

interface IOmniShizukuService {
    void destroy() = 16777114;
    String getStatus() = 1;
    String getFrameStats() = 2;
    boolean resetFrameStats() = 3;
}
