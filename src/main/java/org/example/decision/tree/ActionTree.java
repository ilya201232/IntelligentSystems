package org.example.decision.tree;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;

@RequiredArgsConstructor
public abstract class ActionTree {

    protected final Knowledge knowledgeGlobal;
    protected TreeNode treeRoot = TreeNode.getEmptyTreeNode();

    public Action decideAction(Perception perception) {
        return treeRoot.getResultAction(perception, knowledgeGlobal);
    }
}
