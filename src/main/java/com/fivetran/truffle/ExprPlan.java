package com.fivetran.truffle;

import com.oracle.truffle.api.nodes.Node;
import org.apache.calcite.rel.RelRoot;

/**
 * By the time we send our query to Truffle, it has already been parsed, validated, and planned.
 * This pseudo-node simply holds the query plan so we can pass it as the "context" parameter to {@link TruffleSqlLanguage#parse}
 */
class ExprPlan extends Node {
    public final RelRoot plan;
    public final LazyRowSink then;

    public ExprPlan(RelRoot plan, LazyRowSink then) {
        this.plan = plan;
        this.then = then;
    }
}
