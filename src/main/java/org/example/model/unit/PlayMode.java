package org.example.model.unit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
public class PlayMode {

    @Setter
    private int createdAt;
    private PlayModeType playModeType;
    private Side side;

    public static PlayMode parsePlayMode(String playMode, int cycleNumber) {
        String[] split = playMode.split("_");

        Side side = null;
        if (split[split.length - 1].length() == 1) {
            side = Side.parseString(split[split.length - 1]);
            playMode = playMode.substring(0, playMode.length() - 2);
        }

        PlayModeType type = PlayModeType.parsePlayModeType(playMode);

        return new PlayMode(cycleNumber, type, side);
    }
}
