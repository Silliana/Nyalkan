package net.nyalkan.render.profiling;

import net.nyalkan.render.Nanos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

// rolling frame time stats
public final class FpsOverlay {
    private static final long SECOND = Nanos.SECOND;
    private static final long WINDOW_US = 10_000_000L;
    private static final long RECENT_US = 500_000L;
    private static final long REFRESH = SECOND / 2;
    private static final int SIZE = 8192;
    private static final int COLOR = 0xE0E0E0;
    private static final int BACKGROUND = 0x90505050;

    private static final int[] times = new int[SIZE];
    private static final int[] worst = new int[SIZE / 100 + 1];
    private static int head, count;
    private static long last, refreshAt;
    private static String[] lines = new String[0];
    private static int width;

    private FpsOverlay() {}

    public static void render(GuiGraphics graphics) {
        long now = System.nanoTime();
        long dt = now - last;
        if (last == 0 || dt > SECOND) {
            count = 0;
        } else {
            times[head] = (int) (dt / 1000);
            head = (head + 1) & (SIZE - 1);
            if (count < SIZE)
                count++;
        }
        last = now;

        Minecraft minecraft = Minecraft.getInstance();
        if (now >= refreshAt) {
            refreshAt = now + REFRESH;
            update(minecraft.font);
        }
        if (minecraft.options.hideGui || lines.length == 0)
            return;

        int lineHeight = minecraft.font.lineHeight;
        graphics.fill(1, 1, width + 5, lines.length * lineHeight + 3, BACKGROUND);
        for (int i = 0; i < lines.length; i++)
            graphics.drawString(minecraft.font, lines[i], 3, 3 + i * lineHeight, COLOR, false);
    }

    private static void update(Font font) {
        long total = 0, recent = 0;
        int n = 0, recentFrames = 0, min = Integer.MAX_VALUE, max = 0;
        for (int i = 0; i < count && total < WINDOW_US; i++, n++) {
            int t = times[(head - 1 - i) & (SIZE - 1)];
            total += t;
            if (total <= RECENT_US) {
                recent += t;
                recentFrames++;
            }
            if (t < min)
                min = t;
            if (t > max)
                max = t;
        }
        if (n == 0 || total == 0) {
            lines = new String[0];
            return;
        }

        int k = Math.max(1, n / 100);
        long worstSum = slowest(n, k);
        double frame = recentFrames > 0 ? recent / 1000.0 / recentFrames : total / 1000.0 / n;

        lines = new String[]{
                "FPS " + Math.round(n * 1e6 / total),
                "Min " + Math.round(1e6 / max) + "  Max " + Math.round(1e6 / Math.max(1, min)),
                "1% low " + Math.round(1e6 * k / worstSum),
                "Frame " + Math.round(frame * 100) / 100.0 + " ms"
        };
        width = 0;
        for (String line : lines)
            width = Math.max(width, font.width(line));
    }

    // sum of k largest
    private static long slowest(int n, int k) {
        int size = 0;
        for (int i = 0; i < n; i++) {
            int t = times[(head - 1 - i) & (SIZE - 1)];
            if (size < k) {
                int c = size++;
                while (c > 0 && worst[(c - 1) >> 1] > t) {
                    worst[c] = worst[(c - 1) >> 1];
                    c = (c - 1) >> 1;
                }
                worst[c] = t;
            } else if (t > worst[0]) {
                int c = 0;
                while (true) {
                    int child = 2 * c + 1;
                    if (child >= k)
                        break;
                    if (child + 1 < k && worst[child + 1] < worst[child])
                        child++;
                    if (worst[child] >= t)
                        break;
                    worst[c] = worst[child];
                    c = child;
                }
                worst[c] = t;
            }
        }
        long sum = 0;
        for (int i = 0; i < size; i++)
            sum += worst[i];
        return Math.max(1, sum);
    }
}
