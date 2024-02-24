package org.example.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.model.object.Ball;
import org.example.model.object.Line;
import org.example.model.object.Marker;
import org.example.model.object.Player;
import org.example.receiver.dto.HearDTO;
import org.example.receiver.dto.SenseBodyDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Perception {

    private boolean hasGotSenseBodyInfo;
    private boolean hasGotSeeInfo;
    private boolean hasGotHearInfo;

    private LocalDateTime creationDatetime;

    private int cycleNumber;

    // From hear message
    private HearDTO heardMessage;

    // From see message
    private List<Marker> markersSaw;
//    private final List<Line> linesSaw;
    private Ball ballSaw;

    private List<Player> teammatesSaw;
    private List<Player> opponentsSaw;
    private List<Player> unknownPlayersSaw;

    // From sense_body message
    private SenseBodyDTO sensed;

    public Perception(int cycleNumber) {
        hasGotSenseBodyInfo = false;
        hasGotSeeInfo = false;
        hasGotHearInfo = false;

        this.cycleNumber = cycleNumber;
        markersSaw = new ArrayList<>();
//        linesSaw = new ArrayList<>();
        teammatesSaw = new ArrayList<>();
        opponentsSaw = new ArrayList<>();
        unknownPlayersSaw = new ArrayList<>();
        creationDatetime = LocalDateTime.now();
    }

//    public void addLine(Line line) {
//        linesSaw.add(line);
//    }
}
