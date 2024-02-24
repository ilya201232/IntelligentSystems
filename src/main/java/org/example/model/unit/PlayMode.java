package org.example.model.unit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
public class PlayMode {

    private PlayModeType playModeType;
    private Side side;

    public static PlayMode parsePlayMode(String playMode) {
        String[] split = playMode.split("_");

        Side side = null;
        if (split[split.length - 1].length() == 1) {
            side = Side.parseString(split[split.length - 1]);
            playMode = playMode.substring(0, playMode.length() - 2);
        }

        PlayModeType type = PlayModeType.parsePlayModeType(playMode);

        return new PlayMode(type, side);
    }
}
