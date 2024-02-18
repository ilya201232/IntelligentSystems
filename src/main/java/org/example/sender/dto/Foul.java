package org.example.sender.dto;

public enum Foul {
    TRUE, FALSE, ON, OFF;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
