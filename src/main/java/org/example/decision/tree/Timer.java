package org.example.decision.tree;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Timer {

    private final int defaultValue;
    private int value;

    public Timer(int defaultValue) {
        this.defaultValue = defaultValue;
        value = defaultValue;
    }

    public void resetTimer() {
        value = defaultValue;
    }
}
