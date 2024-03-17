package org.example.decision.tree;

import org.example.model.Knowledge;

import java.util.*;

public abstract class TimedActionTree extends ActionTree {

    private final Map<UUID, Timer> timers;

    public TimedActionTree(Knowledge knowledgeGlobal) {
        super(knowledgeGlobal);

        timers = new HashMap<>();
    }

    @Override
    public void alwaysAction() {
        increaseTimers();
    }

    protected Optional<Integer> getTimerValue(UUID uuid) {
        return Optional.ofNullable(timers.get(uuid)).map(Timer::getValue);
    }

    protected void increaseTimers() {
        timers.values().forEach(Timer::increase);
    }

    protected void resetTimer(UUID uuid) {
        Timer timer = timers.get(uuid);

        if (timer != null) {
            timer.resetTimer();
        }
    }

    protected void resetAllTimers() {
        timers.values().forEach(Timer::resetTimer);
    }

    protected UUID createTimer() {
        return createTimer(0);
    }

    protected UUID createTimer(int defaultValue) {
        UUID uuid = UUID.randomUUID();

        timers.put(uuid, new Timer(defaultValue));

        return uuid;
    }

}
