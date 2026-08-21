package model.match.waves;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public final class LaneBag {

    private final Random random;
    private final Deque<Integer> bag = new ArrayDeque<>();
    private int lanes;
    private int lastDrawn = -1;

    public LaneBag(int lanes, Random random) {
        this.random = random == null ? new Random() : random;
        reset(lanes);
    }

    public void reset(int laneCount) {
        this.lanes = Math.max(1, laneCount);
        this.lastDrawn = -1;
        bag.clear();
    }

    public int laneCount() {
        return lanes;
    }

    public int draw() {
        if (bag.isEmpty()) refill();
        int lane = bag.poll();
        lastDrawn = lane;
        return lane;
    }

    public List<Integer> preferenceOrder(int lane) {
        List<Integer> order = new ArrayList<>(lanes);
        order.add(Math.floorMod(lane, lanes));
        List<Integer> rest = new ArrayList<>(bag);
        for (Integer candidate : rest) {
            if (!order.contains(candidate)) order.add(candidate);
        }
        for (int i = 0; i < lanes; i++) {
            if (!order.contains(i)) order.add(i);
        }
        return order;
    }

    public void giveBack(int lane) {
        if (lanes <= 1) return;
        int normalized = Math.floorMod(lane, lanes);
        if (!bag.contains(normalized)) bag.addLast(normalized);
    }

    private void refill() {
        List<Integer> order = new ArrayList<>(lanes);
        for (int i = 0; i < lanes; i++) order.add(i);
        Collections.shuffle(order, random);
        if (lanes > 1 && order.get(0) == lastDrawn) {
            Collections.swap(order, 0, 1 + random.nextInt(lanes - 1));
        }
        bag.addAll(order);
    }
}
