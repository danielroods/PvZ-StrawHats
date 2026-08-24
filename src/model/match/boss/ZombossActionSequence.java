package model.match.boss;

import model.collections.animations.AnimationFactory;

import java.util.ArrayList;
import java.util.List;

public final class ZombossActionSequence {

    public record Step(String clip, double holdSeconds, double clipLength, boolean loop) { }

    private final String name;
    private final String pamPath;
    private final List<Step> steps = new ArrayList<>();

    private int stepIndex;
    private double stepElapsed;
    private boolean finished;

    public ZombossActionSequence(String name, String pamPath) {
        this.name = name;
        this.pamPath = pamPath;
    }

    public ZombossActionSequence then(String clip) {
        double length = clipLength(clip);
        steps.add(new Step(clip, length, length, false));
        return this;
    }

    public ZombossActionSequence then(String clip, double holdSeconds) {
        steps.add(new Step(clip, Math.max(0.05, holdSeconds), clipLength(clip), false));
        return this;
    }

    public ZombossActionSequence loop(String clip, double holdSeconds) {
        steps.add(new Step(clip, Math.max(0.05, holdSeconds), clipLength(clip), true));
        return this;
    }

    private double clipLength(String clip) {
        double length = AnimationFactory.exactClipDurationForPath(pamPath, clip);
        return length > 0 ? length : 1.0;
    }

    public String getName() { return name; }

    public boolean isFinished() { return finished || steps.isEmpty(); }

    public int getStepIndex() { return stepIndex; }

    public int getStepCount() { return steps.size(); }

    public double getStepElapsed() { return stepElapsed; }

    public double getStepProgress() {
        Step step = currentStep();
        if (step == null || step.holdSeconds() <= 0) return 1.0;
        return Math.min(1.0, stepElapsed / step.holdSeconds());
    }

    public Step currentStep() {
        if (finished || steps.isEmpty()) return null;
        return steps.get(Math.min(stepIndex, steps.size() - 1));
    }

    public String getCurrentClip() {
        Step step = currentStep();
        return step == null ? null : step.clip();
    }

    public double getCurrentClipTime() {
        Step step = currentStep();
        if (step == null) return 0.0;
        if (step.loop() && step.clipLength() > 0) return stepElapsed % step.clipLength();
        return Math.min(stepElapsed, step.clipLength());
    }

    public boolean advance(double deltaSeconds) {
        if (finished || steps.isEmpty()) return false;
        stepElapsed += Math.max(0.0, deltaSeconds);
        while (stepIndex < steps.size() && stepElapsed >= steps.get(stepIndex).holdSeconds()) {
            stepElapsed -= steps.get(stepIndex).holdSeconds();
            stepIndex++;
        }
        if (stepIndex >= steps.size()) {
            stepIndex = steps.size() - 1;
            stepElapsed = steps.get(stepIndex).holdSeconds();
            finished = true;
            return true;
        }
        return false;
    }

    public double totalSeconds() {
        double total = 0;
        for (Step step : steps) total += step.holdSeconds();
        return total;
    }
}
