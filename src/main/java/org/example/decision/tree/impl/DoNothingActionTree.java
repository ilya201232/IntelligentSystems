package org.example.decision.tree.impl;

import org.example.decision.tree.ActionTree;
import org.example.decision.tree.TreeNode;
import org.example.model.Knowledge;
import org.example.model.Perception;

public class DoNothingActionTree extends ActionTree {

    public DoNothingActionTree() {
        super(null);
    }

    @Override
    protected TreeNode createTreeRoot() {
        return TreeNode.getEmptyTreeNode();
    }

    @Override
    public boolean checkMinimumConditionForPassingPerception(Perception perception) {
        return true;
    }
}
