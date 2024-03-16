package org.example.controller;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.decision.DecisionDelegate;
import org.example.decision.tree.impl.GoaliePlayerActionTree;
import org.example.decision.tree.impl.PassPlayerActionTree;
import org.example.decision.tree.impl.ScorePlayerActionTree;
import org.example.exception.FailedToCalculateException;
import org.example.model.Knowledge;
import org.example.model.base.GameObject;
import org.example.model.object.Marker;
import org.example.model.unit.Vector2;
import org.example.receiver.PerceptionFormer;
import org.example.receiver.Receiver;
import org.example.model.Perception;
import org.example.model.object.Player;
import org.example.sender.Sender;
import org.example.sender.action.Action;
import org.example.sender.action.EmptyAction;
import org.example.decision.tree.ActionTree;
import org.example.decision.tree.impl.RegularPlayerActionTree;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class PlayerController implements Runnable {

    private final Knowledge knowledge;
    private PerceptionFormer perceptionFormer;
    private final DecisionDelegate decisionDelegate;

    private Receiver receiver;
    private Sender sender;


    @Setter
    private double turnMoment;
    private final Vector2 startPos;
    private boolean isInitialized = false;

    public PlayerController(String teamName, Vector2 startPos) {
        knowledge = new Knowledge(teamName, false, startPos, 0);
        this.startPos = startPos;

        decisionDelegate = new DecisionDelegate(ActionTree.createEmptyActionTree());
    }

    public PlayerController(String teamName, boolean isGoalie, Vector2 startPos, int teammatesAmount, boolean isPassing) {
        knowledge = new Knowledge(teamName, isGoalie, startPos, teammatesAmount);
        this.startPos = startPos;

        ActionTree actionTree = ActionTree.createEmptyActionTree();

        if (isGoalie) {
            actionTree = new GoaliePlayerActionTree(knowledge);
        } else {

            if (isPassing) {
                actionTree = new PassPlayerActionTree(knowledge);
            } else {
                actionTree = new ScorePlayerActionTree(knowledge);
            }

//            actionTree = new RegularPlayerActionTree(knowledge);
        }

        decisionDelegate = new DecisionDelegate(actionTree);
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            perceptionFormer = new PerceptionFormer(knowledge, socket);
            sender = new Sender(socket, knowledge);

            try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
                Future<?> future = executorService.submit(perceptionFormer);

                try {
                    sender.sendInit(knowledge.getTeamName(), "7", knowledge.isGoalie());
                    sender.sendSyncSee(); // Will sync all messages to cycle time by rounding up!!!
                    while (!future.isDone()) {
                        if (knowledge.isServerReady()) {
                            if (!isInitialized) {
                                sender.sendMove(startPos);
                                isInitialized = true;
                            }
                        } else {
                            continue;
                        }
                        executeActions();
                    }
                    sender.sendBye();
                } catch (Exception e) {
                    log.error("Unexpected exception", e);
                }


            } catch (Exception e) {
                log.error("Unexpected exception", e);
                throw e;
            }
        } catch (SocketException e) {
            log.error("Failed to initialise socket connection for player in team {}", e.getMessage());
        } catch (UnknownHostException e) {
            log.error("Failed to get localhost address.");
        } catch (Exception e) {
            log.error("Unexpected exception", e);
            throw e;
        }
    }

    // Main function!
    private void executeActions() {
        Perception perception = perceptionFormer.getLastPerception();

        if (perception == null) {
            log.debug("No enough data for position calculation...");
            return;
        }

        // Don't act until game started
        if (perception.getCycleNumber() == 0) {
            return;
        }

//        calcPosition(perception);

//        Action action = planner.planAction(perception);
        Action action = decisionDelegate.planAction(perception);
        sender.sendCommand(action);
    }

}
