package org.example.sender;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Knowledge;
import org.example.model.unit.Vector2;
import org.example.receiver.dto.enums.ViewQuality;
import org.example.receiver.dto.enums.ViewWidth;
import org.example.sender.action.*;
import org.example.sender.dto.Foul;

import java.io.IOException;
import java.net.*;

@Slf4j
public class Sender {

    private static final int WAIT_TIME_MS = 150;

    private final DatagramSocket socket;
    private final InetAddress address;
    private final Knowledge knowledge;

    public Sender(DatagramSocket socket, Knowledge knowledge, InetAddress address) throws UnknownHostException {
        this.socket = socket;
        this.address = address;
        this.knowledge = knowledge;
    }

    public void sendInit(String teamName, String version, boolean isGoalie) {
        StringBuilder builder = new StringBuilder();

        builder.append("(init ").append(teamName).append(" (version ").append(version).append(")");
        if (isGoalie) {
            builder.append(" (goalie)");
        }
        builder.append(")");

        sendCommand(builder.toString(), 6000);
    }

    public void sendBye() {
        sendCommand("(bye)");
    }

    public void sendTurn(double moment) {
        sendCommand("(turn " + moment + ")");
    }

    public void sendDash(double power) {
        sendDash(power, null);
    }

    public void sendDash(double power, Double direction) {
        StringBuilder builder = new StringBuilder();

        builder.append("(dash ").append(power);
        if (direction != null) {
            builder.append(" ").append(direction);
        }
        builder.append(")");

        sendCommand(builder.toString());
    }

    public void sendKick(double power, double direction) {
        sendCommand("(kick " + power + " " + direction + ")");
    }

    public void sendTackle(double power, Foul foul) {
        StringBuilder builder = new StringBuilder();

        builder.append("(tackle ").append(power);
        if (foul != null) {
            builder.append(" ").append(foul);
        }
        builder.append(")");

        sendCommand(builder.toString());
    }

    public void sendCatch(double direction) {
        sendCommand("(catch " + direction + ")");
    }

    public void sendMove(Vector2 position) {
        sendCommand("(move " + position.getX() + " " + position.getY() + ")");
    }

    public void sendChangeView(ViewWidth width, ViewQuality quality) {
        StringBuilder builder = new StringBuilder();

        builder.append("(change_view");
        if (width != null) {
            builder.append(" ").append(width);
        }
        builder.append(" ").append(quality).append(")");

        sendCommand(builder.toString());
    }

    public void sendSay(String message) {
        sendCommand("(say \"" + message + "\")");
    }

    public void sendPointTo(double distance, double direction) {
        sendCommand("(move " + distance + " " + direction + ")");
    }

    public void sendPointTo(boolean off) {
        sendCommand("(pointto " + (off ? "of" : "false") + ")");
    }

    /*public void sendAttentionTo(double distance, double direction) {
        sendCommand("(move " + distance + " " + direction + ")");
    }

    public void sendAttentionTo(boolean off) {
        sendCommand("(pointto " + (off ? "of" : "false") + ")");
    }*/

    public void sendDone() {
        sendCommand("(done)");
    }

    // TODO: create dto in receiver!
    public void sendScore() {
        sendCommand("(score)");
    }

    public void sendSyncSee() {
        sendCommand("(synch_see)");
    }

    public void sendCommand(Action action) {
        switch (action.getType()) {
            case TURN -> {
                TurnAction turnAction = (TurnAction) action;
                sendTurn(turnAction.getMoment());
                /*try {
                    log.debug("Sleeping for {} ms to wait for new data resulting from this action.", WAIT_TIME_MS);
                    Thread.sleep(WAIT_TIME_MS);
                } catch (InterruptedException e) {
                    log.error("Sleep operation was interrupted", e);
                }*/
            }
            case DASH -> {
                DashAction dashAction = (DashAction) action;
                sendDash(dashAction.getPower(), dashAction.getDirection());
                /*try {
                    log.debug("Sleeping for {} ms to wait for new data resulting from this action.", WAIT_TIME_MS);
                    Thread.sleep(WAIT_TIME_MS);
                } catch (InterruptedException e) {
                    log.error("Sleep operation was interrupted", e);
                }*/
            }
            case KICK -> {
                KickAction kickAction = (KickAction) action;
                sendKick(kickAction.getPower(), kickAction.getDirection());
                /*try {
                    log.debug("Sleeping for {} ms to wait for new data resulting from this action.", WAIT_TIME_MS);
                    Thread.sleep(WAIT_TIME_MS);
                } catch (InterruptedException e) {
                    log.error("Sleep operation was interrupted", e);
                }*/
            }
            case TACKLE -> {
                TackleAction tackleAction = (TackleAction) action;
                sendTackle(tackleAction.getPower(), tackleAction.getFoul());
                /*try {
                    log.debug("Sleeping for {} ms to wait for new data resulting from this action.", WAIT_TIME_MS);
                    Thread.sleep(WAIT_TIME_MS);
                } catch (InterruptedException e) {
                    log.error("Sleep operation was interrupted", e);
                }*/
            }
            case CATCH -> {
                CatchAction catchAction = (CatchAction) action;
                sendCatch(catchAction.getDirection());
                /*try {
                    log.debug("Sleeping for {} ms to wait for new data resulting from this action.", WAIT_TIME_MS);
                    Thread.sleep(WAIT_TIME_MS);
                } catch (InterruptedException e) {
                    log.error("Sleep operation was interrupted", e);
                }*/
            }
            case MOVE -> {
                MoveAction moveAction = (MoveAction) action;
                sendMove(moveAction.getPosition());
            }
            case CHANGE_VIEW -> {
                ChangeViewAction changeViewAction = (ChangeViewAction) action;
                sendChangeView(changeViewAction.getWidth(), changeViewAction.getQuality());
            }
            case SAY -> {
                SayAction sayAction = (SayAction) action;
                sendSay(sayAction.getMessage());
            }
            case POINT_TO -> {
                PointToAction pointToAction = (PointToAction) action;
                sendPointTo(pointToAction.getDistance(), pointToAction.getDirection());
            }
            case NONE -> {
                /*try {
                    log.debug("Sleeping for {} ms to wait for new data (empty action).", WAIT_TIME_MS);
                    Thread.sleep(WAIT_TIME_MS);
                } catch (InterruptedException e) {
                    log.error("Sleep operation was interrupted", e);
                }*/
            }
        }

    }

    private void sendCommand(String command) {
        sendCommand(command, 6000);
    }

    private void sendCommand(String command, int port) {
        DatagramPacket packet = new DatagramPacket(command.getBytes(), command.getBytes().length, address, port);

        log.debug("Sending message {}", command);

        try {
            socket.send(packet);
        } catch (IOException e) {
            log.error("IOException has been caught while trying to send message {}", command);
        }
    }
}
