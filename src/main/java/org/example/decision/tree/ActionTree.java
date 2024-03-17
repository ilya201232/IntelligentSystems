package org.example.decision.tree;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.decision.tree.impl.DoNothingActionTree;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;

import java.util.HashMap;

public abstract class ActionTree {

    protected final Knowledge knowledgeGlobal;
    protected TreeNode treeRoot = TreeNode.getEmptyTreeNode();
    protected final HashMap<String, TreeNode> createdTreeNodes = new HashMap<>();

    @Setter
    protected Perception lastPerception = null;

    public ActionTree(Knowledge knowledgeGlobal) {
        this.knowledgeGlobal = knowledgeGlobal;

        treeRoot = createTreeRoot();
    }

    public Action decideAction(Perception perception) {
        return treeRoot.getResultAction(perception, knowledgeGlobal);
    }

    protected abstract TreeNode createTreeRoot();

    public abstract boolean checkMinimumConditionForPassingPerception(Perception perception);

    public static ActionTree createEmptyActionTree() {
        return new DoNothingActionTree();
    }

    public void alwaysAction() {

    }
}
