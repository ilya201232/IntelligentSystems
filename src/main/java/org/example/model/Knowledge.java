package org.example.model;

import lombok.Getter;
import lombok.Setter;
import org.example.model.object.Player;
import org.example.model.unit.PlayMode;
import org.example.model.unit.Side;
import org.example.model.unit.Vector2;
import org.example.receiver.dto.PlayerTypeDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Knowledge {

    @Setter
    private boolean isServerReady = false;

    @Setter
//    private Map<String, String> serverParams;
    private List<String> serverParams;
    @Setter
//    private Map<String, String> playerParams;
    private List<String> playerParams;
    private final List<PlayerTypeDTO> playerTypes;

//    private final Map<String, Line> lines;
    private final Map<String, Vector2> markersPositions;

//    private final Ball ball;

//    private final Map<Integer, Player> teammates;
//    private final Map<Integer, Player> opponents;

    // TODO: player types?
    private Integer uniformNumber;
    private final boolean isGoalie;

    private final Player thisPlayer;

    private Side teamSide;
    private final String teamName;
    @Setter
    private String oppositeTeamName;

    @Setter
    private PlayMode currentPlayMode;

    @Setter
    private Integer port;

    public Knowledge(String teamName, boolean isGoalie) {

        serverParams = new ArrayList<>();
        playerParams = new ArrayList<>();
        playerTypes = new ArrayList<>();

        this.teamName = teamName;
        this.isGoalie = isGoalie;

        thisPlayer = new Player(teamName, isGoalie);

//        lines = new HashMap<>();
//
//        lines.put("l l", new Line("l l"));
//        lines.put("l r", new Line("l r"));
//        lines.put("l t", new Line("l t"));
//        lines.put("l b", new Line("l b"));


        markersPositions = new HashMap<>();

        markersPositions.put("f t l 50", new Vector2(-50, 39));
        markersPositions.put("f t l 40", new Vector2(-40, 39));
        markersPositions.put("f t l 30", new Vector2(-30, 39));
        markersPositions.put("f t l 20", new Vector2(-20, 39));
        markersPositions.put("f t l 10", new Vector2(-10, 39));
        markersPositions.put("f t 0", new Vector2(0, 39));
        markersPositions.put("f t r 10", new Vector2(10, 39));
        markersPositions.put("f t r 20", new Vector2(20, 39));
        markersPositions.put("f t r 30", new Vector2(30, 39));
        markersPositions.put("f t r 40", new Vector2(40, 39));
        markersPositions.put("f t r 50", new Vector2(50, 39));

        markersPositions.put("f b l 50", new Vector2(-50, -39));
        markersPositions.put("f b l 40", new Vector2(-40, -39));
        markersPositions.put("f b l 30", new Vector2(-30, -39));
        markersPositions.put("f b l 20", new Vector2(-20, -39));
        markersPositions.put("f b l 10", new Vector2(-10, -39));
        markersPositions.put("f b 0", new Vector2(0, -39));
        markersPositions.put("f b r 10", new Vector2(10, -39));
        markersPositions.put("f b r 20", new Vector2(20, -39));
        markersPositions.put("f b r 30", new Vector2(30, -39));
        markersPositions.put("f b r 40", new Vector2(40, -39));
        markersPositions.put("f b r 50", new Vector2(50, -39));

        markersPositions.put("f l t 30", new Vector2(-57.5, 30));
        markersPositions.put("f l t 20", new Vector2(-57.5, 20));
        markersPositions.put("f l t 10", new Vector2(-57.5, 10));
        markersPositions.put("f l 0", new Vector2(-57.5, 0));
        markersPositions.put("f l b 10", new Vector2(-57.5, -10));
        markersPositions.put("f l b 20", new Vector2(-57.5, -20));
        markersPositions.put("f l b 30", new Vector2(-57.5, -30));

        markersPositions.put("f r t 30", new Vector2(57.5, 30));
        markersPositions.put("f r t 20", new Vector2(57.5, 20));
        markersPositions.put("f r t 10", new Vector2(57.5, 10));
        markersPositions.put("f r 0", new Vector2(57.5, 0));
        markersPositions.put("f r b 10", new Vector2(57.5, -10));
        markersPositions.put("f r b 20", new Vector2(57.5, -20));
        markersPositions.put("f r b 30", new Vector2(57.5, -30));

        markersPositions.put("f l t", new Vector2(-52.5, 34));
        markersPositions.put("f c t", new Vector2(0, 34));
        markersPositions.put("f r t", new Vector2(52.5, 34));
        markersPositions.put("f l b", new Vector2(-52.5, -34));
        markersPositions.put("f c b", new Vector2(0, -34));
        markersPositions.put("f r b", new Vector2(52.5, -34));

        markersPositions.put("f g l t", new Vector2(-52.5, 7.01));
        markersPositions.put("g l", new Vector2(-52.5, 0));
        markersPositions.put("f g l b", new Vector2(-52.5, -7.01));

        markersPositions.put("f g r t", new Vector2(52.5, 7.01));
        markersPositions.put("g r", new Vector2(52.5, 0));
        markersPositions.put("f g r b", new Vector2(52.5, -7.01));

        markersPositions.put("f p l t", new Vector2(-36, 20.15));
        markersPositions.put("f p l c", new Vector2(-36, 0));
        markersPositions.put("f p l b", new Vector2(-36, -20.15));

        markersPositions.put("f p r t", new Vector2(36, 20.15));
        markersPositions.put("f p r c", new Vector2(36, 0));
        markersPositions.put("f p r b", new Vector2(36, -20.15));

        markersPositions.put("f c", new Vector2(0, 0));


//        ball = new Ball();


        // player::allow_mult_default_type = true to allow multiple default player types (since)
//        teammates = new HashMap<>();
//        opponents = new HashMap<>();
//
//        Side oppositeSide = Side.getOpposite(player.getSide());
//
//        for (int i = 1; i < 12; i++) {
//            if (player.getUniformNumber() != i) {
//                teammates.put(i, new Player(player.getTeamName(), player.getSide(), i));
//            }
//            opponents.put(i, new Player("", oppositeSide, i));
//        }


    }

    public void addPlayerType(PlayerTypeDTO dto) {
        playerTypes.add(dto);
    }

    public void setUniformNumber(Integer uniformNumber) {
        this.uniformNumber = uniformNumber;
        thisPlayer.setUniformNumber(uniformNumber);
    }

    public void setTeamSide(Side teamSide) {
        this.teamSide = teamSide;
        thisPlayer.setSide(teamSide);
    }
}
