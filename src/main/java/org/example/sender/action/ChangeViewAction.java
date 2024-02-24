package org.example.sender.action;

import lombok.Getter;
import org.example.receiver.dto.enums.ViewQuality;
import org.example.receiver.dto.enums.ViewWidth;

@Getter
public class ChangeViewAction extends Action {

    private final ViewWidth width;
    private final ViewQuality quality;

    public ChangeViewAction(ViewWidth width, ViewQuality quality) {
        super(ActionType.CHANGE_VIEW);
        this.width = width;
        this.quality = quality;
    }
}
