package org.example.sender.action;

import lombok.Getter;
import org.example.receiver.dto.enums.ViewQuality;
import org.example.receiver.dto.enums.ViewWidth;

@Getter
public class ChangeViewAction extends Action {

    private final ViewWidth width;
    private final ViewQuality quality;

    public ChangeViewAction(ViewWidth width, ViewQuality quality) {
        this(width, quality, false);
    }

    public ChangeViewAction(ViewWidth width, ViewQuality quality, boolean repeatable) {
        super(ActionType.CHANGE_VIEW, repeatable);
        this.width = width;
        this.quality = quality;
    }
}
