package org.example.receiver.dto.object;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.receiver.dto.enums.ViewWidth;
import org.example.receiver.dto.enums.ViewQuality;

@Getter
@Setter
@Builder
public class ViewMode {
    private ViewQuality viewQuality;
    private ViewWidth viewModeHorizontal;
}
