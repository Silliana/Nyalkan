package net.nyalkan.render.gui;

import java.util.Arrays;

final class HudLayerCosts {
    private double[] cost = new double[64];

    void record(int layer, long nanos) {
        if (layer < 0)
            return;
        if (layer >= this.cost.length)
            this.cost = Arrays.copyOf(this.cost, Math.max(layer + 1, this.cost.length * 2));
        double old = this.cost[layer];
        this.cost[layer] = old == 0 ? nanos : old * 0.8 + nanos * 0.2;
    }

    int[] passBounds(int layerCount, int passes) {
        int[] bounds = new int[passes + 1];
        double total = 0;
        for (int i = 0; i < layerCount; i++)
            total += Math.max(this.costOf(i), 1);

        double acc = 0;
        int next = 1;
        for (int i = 0; i < layerCount && next < passes; i++) {
            acc += Math.max(this.costOf(i), 1);
            if (acc >= total * next / passes)
                bounds[next++] = i + 1;
        }
        while (next < passes)
            bounds[next++] = layerCount;
        bounds[passes] = Integer.MAX_VALUE;
        return bounds;
    }

    private double costOf(int layer) {
        return layer < this.cost.length ? this.cost[layer] : 0;
    }
}
