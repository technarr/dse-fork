package tools.aqua.dse.tree;

import gov.nasa.jpf.constraints.api.Valuation;

class LeafBuggy extends LeafWithValuation {

    private final String cause;

    LeafBuggy(DecisionNode parent, int pos, Valuation val, String cause) {
        super(parent, NodeType.ERROR, pos, val);
        this.cause = cause;
    }

}