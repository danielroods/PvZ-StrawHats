package model.collections.zombie;

import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;

public final class ZombieSequence {

    public interface StepAction {
        void run(Zombie zombie, GameSession session);
    }

    private record Step(String clip, double duration, boolean loop, StepAction onEnter) { }

    private final List<Step> steps = new ArrayList<>();
    private Runnable onComplete;

    private int index = -1;
    private double elapsed;

    public ZombieSequence add(String clip, double duration) {
        return add(clip, duration, false, null);
    }

    public ZombieSequence add(String clip, double duration, boolean loop) {
        return add(clip, duration, loop, null);
    }

    public ZombieSequence add(String clip, double duration, boolean loop, StepAction onEnter) {
        if (clip != null) steps.add(new Step(clip, Math.max(0.0, duration), loop, onEnter));
        return this;
    }

    public ZombieSequence onComplete(Runnable action) {
        this.onComplete = action;
        return this;
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    public boolean isFinished() {
        return index >= steps.size();
    }

    public boolean tick(Zombie zombie, GameSession session, double deltaSeconds) {
        if (steps.isEmpty() || isFinished()) return false;

        if (index < 0) {
            index = 0;
            elapsed = 0;
            enterCurrent(zombie, session);
            return true;
        }

        elapsed += deltaSeconds;
        while (index < steps.size() && elapsed >= steps.get(index).duration()) {
            elapsed -= steps.get(index).duration();
            index++;
            if (index < steps.size()) enterCurrent(zombie, session);
        }

        if (index >= steps.size()) {
            zombie.clearActionAnimationState();
            if (onComplete != null) {
                Runnable finish = onComplete;
                onComplete = null;
                finish.run();
            }
            return false;
        }
        return true;
    }

    private void enterCurrent(Zombie zombie, GameSession session) {
        Step step = steps.get(index);

        zombie.setActionAnimationState(step.clip(), 0, step.loop());
        if (step.onEnter() != null) step.onEnter().run(zombie, session);
    }
}
