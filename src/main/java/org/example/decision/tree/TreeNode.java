package org.example.decision.tree;

import org.example.model.Knowledge;
import org.example.model.Perception;
import org.example.sender.action.Action;
import org.example.sender.action.EmptyAction;

@FunctionalInterface
public interface TreeNode {

    Action getResultAction(Perception perception, Knowledge knowledge, Object... args);

    static TreeNode getEmptyTreeNode() {
        return (perception, knowledge, args) -> EmptyAction.instance;
    }

}
