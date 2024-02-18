package org.example.receiver;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.model.object.Ball;
import org.example.model.object.Marker;
import org.example.model.object.Player;
import org.example.model.unit.Side;
import org.example.receiver.dto.*;
import org.example.receiver.dto.enums.MessageType;
import org.example.receiver.dto.object.ObjectInfo;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PerceptionFormer implements Runnable {

    private final Knowledge knowledge;

    private Receiver receiver;
    private Perception formingPerception;
    private final LinkedBlockingDeque<Perception> perceptionLinkedBlockingDeque;

    private final int initMessageCount = 3;
    private int messageCounterForInit = 0;

    public PerceptionFormer(Knowledge knowledge, DatagramSocket socket) {
        this.knowledge = knowledge;
        this.receiver = new Receiver(socket, knowledge);
        perceptionLinkedBlockingDeque = new LinkedBlockingDeque<>(5);
    }

    @Override
    public void run() {
        try {

            MessageDTO dto;

            //noinspection InfiniteLoopStatement
            while (true) {
                if (knowledge.getPlayerTypes().isEmpty()) {
                    // Wait till hear anything or universe die...
                    dto = receiver.receiveMessage();
                } else {
                    // Wait up to 300 ms
                    dto = receiver.receiveMessage(3000);
                }

                formPerception(dto);
            }

        } catch (IllegalStateException e) {
            log.error("Starting to get perception data, but initial data hasn't been received yet!");
        } catch (SocketTimeoutException ignored) {
            log.debug("Server stopped sending messages (waited for 300 ms).");
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error", e);
            throw e;
        }

        log.debug("Perception Former is done!");
    }

    private void formPerception(MessageDTO dto) throws IllegalStateException {
        if (formingPerception == null &&
                dto.getMessageType() != MessageType.INIT && dto.getMessageType() != MessageType.SERVER_PARAM &&
                dto.getMessageType() != MessageType.PLAYER_PARAM && dto.getMessageType() != MessageType.PLAYER_TYPE &&
                (knowledge.getUniformNumber() == null || knowledge.getServerParams().isEmpty() ||
                knowledge.getPlayerParams().isEmpty() || knowledge.getPlayerTypes().isEmpty())
        ) {
            throw new IllegalStateException();
        }

        if (dto.getMessageType() == MessageType.INIT || dto.getMessageType() == MessageType.SERVER_PARAM ||
                dto.getMessageType() == MessageType.PLAYER_PARAM || dto.getMessageType() == MessageType.PLAYER_TYPE) {

            switch (dto.getMessageType()) {
                case INIT -> {
                    InitDTO initDTO = (InitDTO) dto;

                    knowledge.setTeamSide(initDTO.getSide());
                    knowledge.setUniformNumber(initDTO.getUniformNumber());
                    knowledge.setCurrentPlayMode(initDTO.getPlayMode());
                }
                case SERVER_PARAM -> knowledge.setServerParams(((ServerParamDTO) dto).getParams());
                case PLAYER_PARAM -> knowledge.setPlayerParams(((PlayerParamDTO) dto).getParams());
                case PLAYER_TYPE -> knowledge.addPlayerType((PlayerTypeDTO) dto);
            }

            return;
        }

        if (!knowledge.isServerReady()) {
            if (messageCounterForInit >= initMessageCount) {
                knowledge.setServerReady(true);
            } else {
                messageCounterForInit++;
            }
        }

        if (formingPerception == null) {
            formingPerception = new Perception(dto.getCycleNumber());
        } else if (formingPerception.getCycleNumber() != dto.getCycleNumber()) {
            addPerception(formingPerception);
            formingPerception = new Perception(dto.getCycleNumber());
        }

        switch (dto.getMessageType()) {
            case SENSE_BODY -> formingPerception.setSensed((SenseBodyDTO) dto);
            case SEE -> {

                SeeDTO seeDTO = (SeeDTO) dto;

                List<Player> teammates = new ArrayList<>();
                List<Player> opponents = new ArrayList<>();
                List<Player> unknown = new ArrayList<>();

                List<Marker> markers = new ArrayList<>();

                for (ObjectInfo objectInfo: seeDTO.getObjects()) {
                    switch (objectInfo.getObjectName().charAt(0)) {
                        case 'p' -> {
                            // Player

                            Player player = new Player();

                            String[] nameParts = objectInfo.getObjectName().split(" ");

                            if (nameParts.length >= 2) {
                                if (nameParts[1].equals(knowledge.getTeamName())) {
                                    player.setSide(knowledge.getTeamSide());
                                } else {
                                    player.setSide(Side.getOpposite(knowledge.getTeamSide()));
                                    if (knowledge.getOppositeTeamName() == null) {
                                        knowledge.setOppositeTeamName(nameParts[1]);
                                    }
                                }
                            }

                            if (nameParts.length >= 3) {
                                player.setUniformNumber(Integer.parseInt(nameParts[2]));
                            }

                            player.setGoalie(nameParts.length == 4);

                            player.setDistance(objectInfo.getDistance());
                            player.setDirection(objectInfo.getDirection());
                            player.setDistanceChange(objectInfo.getDistanceChange());
                            player.setDirectionChange(objectInfo.getDirectionChange());
                            player.setBodyDirection(objectInfo.getBodyFacingDirection());
                            player.setHeadDirection(objectInfo.getHeadFacingDirection());

                            if (player.getSide() == null) {
                                unknown.add(player);
                            } else {
                                if (player.getSide() == knowledge.getTeamSide()) {
                                    teammates.add(player);
                                } else {
                                    opponents.add(player);
                                }
                            }

                        }
                        case 'b' -> {
                            // Ball
                            Ball ball = new Ball();
                            ball.setDistance(objectInfo.getDistance());
                            ball.setDirection(objectInfo.getDirection());
                            ball.setDistanceChange(objectInfo.getDistanceChange());
                            ball.setDirectionChange(objectInfo.getDirectionChange());

                            formingPerception.setBallSaw(ball);
                        }
                        case 'f', 'g' -> {
                            // Marker
                            Marker marker = new Marker(objectInfo.getObjectName());
                            if (objectInfo.getObjectName().charAt(0) != 'l') {
                                marker.setPosition(knowledge.getMarkersPositions().get(objectInfo.getObjectName()));
                            }

                            marker.setDistance(objectInfo.getDistance());
                            marker.setDirection(objectInfo.getDirection());

                            markers.add(marker);
                        }
                        case 'l', 'P', 'B', 'G', 'F' -> {
                            log.debug("Received {} from see message. It's not used => ignoring it", objectInfo.getObjectName());
                        }
                        default -> {
                            log.warn("Unexpected object name from see message: {}. Ignoring it", objectInfo.getObjectName());
                        }
                    }
                }

                formingPerception.setTeammatesSaw(teammates);
                formingPerception.setOpponentsSaw(opponents);
                formingPerception.setUnknownPlayersSaw(unknown);

                formingPerception.setMarkersSaw(markers);
            }
            case HEAR -> formingPerception.setHeardMessage((HearDTO) dto);
        }

    }

    private void addPerception(Perception perception) {
        try {
            perceptionLinkedBlockingDeque.add(perception);
        } catch (IllegalStateException e) {
            perceptionLinkedBlockingDeque.poll();
            perceptionLinkedBlockingDeque.add(perception);
        }
    }

    public Perception getLastPerception() {

        try {
            return perceptionLinkedBlockingDeque.pollLast(600, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for perception to form.");
            return null;
        }
    }


}