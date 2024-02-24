package org.example.receiver;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Knowledge;
import org.example.model.unit.PlayMode;
import org.example.model.unit.Side;
import org.example.receiver.dto.*;
import org.example.receiver.dto.enums.*;
import org.example.receiver.dto.object.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

@Slf4j
public class Receiver {

    private static final int MAXIMUM_SIZE_MESSAGE = 5000;

    private final DatagramSocket socket;
    private final Knowledge knowledge;

    public Receiver(DatagramSocket socket, Knowledge knowledge) {
        this.socket = socket;
        this.knowledge = knowledge;
    }

    public MessageDTO receiveMessage() throws IOException {
        return receiveMessage(0);
    }

    public MessageDTO receiveMessage(int timeout) throws IOException {
        byte[] data = new byte[MAXIMUM_SIZE_MESSAGE];

        DatagramPacket packet = new DatagramPacket(data, data.length);
        socket.setSoTimeout(timeout);
        socket.receive(packet);
        String message = new String(data);
        message = message.substring(0, message.lastIndexOf(")") + 1);

        log.debug("Received message: {}", message);

        if (knowledge.getPort() == null) {
            knowledge.setPort(packet.getPort());
        }

//        return null;
        return parseMessage(message);
    }

    private MessageDTO parseMessage(String message) {
        String messageName = message.substring(1, message.lastIndexOf(")")).split(" ")[0];
        String messageContent = message.substring(1 + messageName.length() + 1, message.lastIndexOf(")"));

        return switch (messageName) {
            case "init" -> parseInit(messageContent);
            case "server_param" -> parseServerParam(messageContent);
            case "player_param" -> parsePlayerParam(messageContent);
            case "player_type" -> parsePlayerType(messageContent);
            case "sense_body" -> parseSenseBody(messageContent);
            case "see" -> parseSee(messageContent);
            case "hear" -> parseHear(messageContent);
            default -> {
                log.warn("Failed to parse {} message type.", messageName);
                yield new MessageDTO(-1, MessageType.NOT_IMPLEMENTED);
            }
        };

    }

    private InitDTO parseInit(String messageContent) {
        log.debug("Type: INIT");

        String[] messageParts = messageContent.split(" ");

        return InitDTO.builder()
                .side(Side.parseString(messageParts[0]))
                .uniformNumber(Integer.parseInt(messageParts[1]))
                .playMode(PlayMode.parsePlayMode(messageParts[2]))
                .build();
    }

    /*private Map<String, String> extractParams(String messageContent) {
        Map<String, String> params = new HashMap<>();

        String[] data = messageContent.substring(1, messageContent.lastIndexOf(")")).split("\\)\\(");

        for (String datum : data) {
            String[] param = datum.split(" ");

            params.put(param[0], param[1]);
        }

        log.debug("Finished. Next!");

        return params;
    }*/

    private List<String> extractParams(String messageContent) {
        return Arrays.stream(messageContent.split(" ")).toList();
    }

    private ServerParamDTO parseServerParam(String messageContent) {
        log.debug("Type: SERVER PARAM");
        return new ServerParamDTO(extractParams(messageContent));
    }

    private PlayerParamDTO parsePlayerParam(String messageContent) {
        log.debug("Type: PLAYER PARAM");
        return new PlayerParamDTO(extractParams(messageContent));
    }

    private PlayerTypeDTO parsePlayerType(String messageContent) {
        log.debug("Type: PLAYER TYPE");

        PlayerTypeDTO dto = new PlayerTypeDTO();

        /*String[] data = messageContent.substring(1, messageContent.lastIndexOf(")")).split("\\)\\(");

        for (String datum : data) {
            String[] param = datum.split(" ");

            switch (param[0]) {
                case "id" -> dto.setId(Integer.parseInt(param[1]));
                case "player_speed_max" -> dto.setPlayerSpeedMax(Double.parseDouble(param[1]));
                case "stamina_inc_max" -> dto.setStaminaIncMax(Double.parseDouble(param[1]));
                case "player_decay" -> dto.setPlayerDecay(Double.parseDouble(param[1]));
                case "inertia_moment" -> dto.setInertiaMoment(Double.parseDouble(param[1]));
                case "dash_power_rate" -> dto.setDashPowerRate(Double.parseDouble(param[1]));
                case "player_size" -> dto.setPlayerSize(Double.parseDouble(param[1]));
                case "kickable_margin" -> dto.setKickableMargin(Double.parseDouble(param[1]));
                case "kick_rand" -> dto.setKickRand(Double.parseDouble(param[1]));
                case "extra_stamina" -> dto.setExtraStamina(Double.parseDouble(param[1]));
                case "effort_max" -> dto.setEffortMax(Double.parseDouble(param[1]));
                case "effort_min" -> dto.setEffortMin(Double.parseDouble(param[1]));
                default -> log.warn("Unknown param in PlayerType: {}", param[0]);
            }
        }*/
        String[] data = messageContent.split(" ");

        dto.setId(Integer.parseInt(data[0]));
        dto.setPlayerSpeedMax(Double.parseDouble(data[1]));
        dto.setStaminaIncMax(Double.parseDouble(data[2]));
        dto.setPlayerDecay(Double.parseDouble(data[3]));
        dto.setInertiaMoment(Double.parseDouble(data[4]));
        dto.setDashPowerRate(Double.parseDouble(data[5]));
        dto.setPlayerSize(Double.parseDouble(data[6]));
        dto.setKickableMargin(Double.parseDouble(data[7]));
        dto.setKickRand(Double.parseDouble(data[8]));
        dto.setExtraStamina(Double.parseDouble(data[9]));
        dto.setEffortMax(Double.parseDouble(data[10]));
        dto.setEffortMin(Double.parseDouble(data[11]));

        return dto;
    }

    private SenseBodyDTO parseSenseBody(String messageContent) {
        log.debug("Type: SENSE BODY");
        SenseBodyDTO dto = new SenseBodyDTO(Integer.parseInt(messageContent.substring(
                0, messageContent.indexOf("(") - 1
        )));

        String[] messageParts = messageContent.substring(messageContent.indexOf("(") + 1, messageContent.lastIndexOf(")")).split("\\) \\(");

        for (int i = 0; i < messageParts.length; i++) {
            String[] data = messageParts[i].split(" ");

            switch (data[0]) {
                case "view_mode" -> dto.setViewMode(
                        ViewMode.builder()
                                .viewQuality(ViewQuality.parseString(data[1]))
                                .viewModeHorizontal(ViewWidth.parseString(data[2]))
                                .build()
                );
                case "stamina" -> dto.setStamina(
                        Stamina.builder()
                                .stamina(Double.parseDouble(data[1]))
                                .effort(Double.parseDouble(data[2]))
//                                .capacity(Double.parseDouble(data[3]))
                                .build()
                );
                case "speed" -> dto.setSpeed(
                        Speed.builder()
                                .amount(Double.parseDouble(data[1]))
                                .direction(Math.toRadians(Double.parseDouble(data[2])))
                                .build()
                );
                case "head_angle" -> dto.setHeadAngle(Math.toRadians(Double.parseDouble(data[1])));
                case "kick" -> dto.setKick(Integer.parseInt(data[1]));
                case "dash" -> dto.setDash(Integer.parseInt(data[1]));
                case "turn" -> dto.setTurn(Integer.parseInt(data[1]));
                case "say" -> dto.setSay(Integer.parseInt(data[1]));
                case "turn_neck" -> dto.setTurnNeck(Integer.parseInt(data[1]));
                case "catch" -> dto.setCatchCount(Integer.parseInt(data[1]));
                case "move" -> dto.setMove(Integer.parseInt(data[1]));
                case "change_view" -> dto.setChangeView(Integer.parseInt(data[1]));
                case "change_focus" -> dto.setChangeFocus(Integer.parseInt(data[1]));
                /*case "arm" -> {
                    Arm arm = new Arm();
                    String armPart = messageParts[i] + ") (" + messageParts[i+1] + ") (" + messageParts[i+2] + ") (" + messageParts[i+3];
                    String[] armData = armPart.substring(armPart.indexOf("(") + 1, armPart.lastIndexOf(")")).split("\\) \\(");
                    for (int j = 0; j < armData.length; j++) {
                        String[] dataIns = armData[j].split(" ");
                        switch (dataIns[0]) {
                            case "movable" -> arm.setMovableCycles(Integer.parseInt(dataIns[1]));
                            case "expires" -> arm.setExpiresCycles(Integer.parseInt(dataIns[1]));
                            case "count" -> arm.setCount(Integer.parseInt(dataIns[1]));
                            default -> log.warn("Unknown param in Arm: {}", dataIns[0]);
                        }
                    }
                    dto.setArm(arm);
                    i += 3;
                }
                case "focus" -> {
                    Focus focus = new Focus();
                    String armPart = messageParts[i] + ") (" + messageParts[i+1];
                    String[] armData = armPart.substring(armPart.indexOf("(") + 1, armPart.lastIndexOf(")")).split("\\) \\(");
                    for (int j = 0; j < armData.length; j++) {
                        String[] dataIns = armData[j].split(" ");
                        switch (dataIns[0]) {
                            case "target" -> {
                                if (dataIns[1].equals("none")) {
                                    focus.setTarget(null);
                                } else {
                                    focus.setTarget(Target.builder()
                                            .side(Side.parseString(dataIns[1]))
                                            .uniformNumber(Integer.parseInt(dataIns[2]))
                                            .build());
                                }
                            }
                            case "count" -> focus.setCount(Integer.parseInt(dataIns[1]));
                            default -> log.warn("Unknown param in Focus: {}", dataIns[0]);
                        }
                    }
                    dto.setFocus(focus);

                    i += 1;
                }
                case "tackle" -> {
                    Tackle tackle = new Tackle();
                    String armPart = messageParts[i] + ") (" + messageParts[i+1];
                    String[] armData = armPart.substring(armPart.indexOf("(") + 1, armPart.lastIndexOf(")")).split("\\) \\(");
                    for (int j = 0; j < armData.length; j++) {
                        String[] dataIns = armData[j].split(" ");
                        switch (dataIns[0]) {
                            case "expires" -> tackle.setExpiresCycles(Integer.parseInt(dataIns[1]));
                            case "count" -> tackle.setCount(Integer.parseInt(dataIns[1]));
                            default -> log.warn("Unknown param in Tackle: {}", dataIns[0]);
                        }
                    }
                    dto.setTackle(tackle);
                    i += 1;
                }
                case "collision" -> {
                    List<CollisionType> collisionTypeList = new ArrayList<>();

                    if (!data[1].equals("none")) {
                        for (int j = 1; j < data.length; j++) {
                            collisionTypeList.add(CollisionType.parseString(data[j]));
                        }
                    }

                    dto.setCollision(Collections.unmodifiableList(collisionTypeList));
                }
                case "foul" -> {
                    Foul foul = new Foul();
                    String armPart = messageParts[i] + ") (" + messageParts[i+1];
                    String[] armData = armPart.substring(armPart.indexOf("(") + 1, armPart.lastIndexOf(")")).split("\\) \\(");
                    for (int j = 0; j < armData.length; j++) {
                        String[] dataIns = armData[j].split(" ");
                        switch (dataIns[0]) {
                            case "charged" -> foul.setChargedCycles(Integer.parseInt(dataIns[1]));
                            case "card" -> {
                                if (dataIns[1].equals("none")) {
                                    foul.setCardType(null);
                                } else {
                                    foul.setCardType(CardType.parseString(dataIns[1]));
                                }
                            }
                            default -> log.warn("Unknown param in Tackle: {}", dataIns[0]);
                        }
                    }
                    dto.setFoul(foul);
                    i += 1;
                }*/
                default -> log.warn("Unknown param in SenseBody: {}", data[0]);
            }
        }

        return dto;
    }

    private SeeDTO parseSee(String messageContent) {
        log.debug("Type: SEE");

        if (!messageContent.contains("(")) {
            return new SeeDTO(Integer.parseInt(messageContent));
        }

        SeeDTO dto = new SeeDTO(Integer.parseInt(messageContent.substring(
                0, messageContent.indexOf("(") - 1
        )));

        List<ObjectInfo> objectInfos = new ArrayList<>();

        String[] messageParts = messageContent.substring(messageContent.indexOf("(") + 1, messageContent.lastIndexOf(")")).split("\\) \\(");

        for (int i = 0; i < messageParts.length; i++) {
            ObjectInfo objectInfo = new ObjectInfo();
            objectInfo.setObjectName(messageParts[i].substring(1, messageParts[i].lastIndexOf(")")));

            String[] data = messageParts[i].substring(messageParts[i].lastIndexOf(")") + 2).split(" ");

            if (data.length >= 1) {
                objectInfo.setDistance(Double.parseDouble(data[0]));
            }
            if (data.length >= 2) {
                objectInfo.setDirection(Math.toRadians(Double.parseDouble(data[1])));
            }
            if (data.length >= 4 && !data[2].equals("t") && !data[2].equals("k")) {
                objectInfo.setDistanceChange(Double.parseDouble(data[3]));
                objectInfo.setDirectionChange(Math.toRadians(Double.parseDouble(data[3])));
            }
            if (data.length >= 6 && !data[5].equals("t") && !data[5].equals("k")) {
                objectInfo.setBodyFacingDirection(Math.toRadians(Double.parseDouble(data[4])));
                objectInfo.setHeadFacingDirection(Math.toRadians(Double.parseDouble(data[5])));
            }

            objectInfos.add(objectInfo);
        }

        dto.setObjects(Collections.unmodifiableList(objectInfos));

        return dto;
    }

    private HearDTO parseHear(String messageContent) {
        log.debug("Type: HEAR");

        String[] messageParts = messageContent.split(" ");

        Double direction = null;
        try {
            direction = Math.toRadians(Double.parseDouble(messageParts[1]));
        } catch (NumberFormatException ignored) {}

        String message = messageParts[2];

        if (message.charAt(0) == '"') {
            message = message.substring(1, message.lastIndexOf(""));
        }

        return HearDTO.builder()
                .cycleNumber(Integer.parseInt(messageParts[0]))
                .senderType((direction != null) ? SenderType.DIRECTION : SenderType.parseString(messageParts[1]))
                .message(message)
                .build();
    }

}
