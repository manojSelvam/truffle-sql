package com.fivetran.truffle.parse;

import com.fivetran.truffle.compile.RelLiteral;
import com.fivetran.truffle.compile.RowSource;
import com.fivetran.truffle.compile.ThenRowSink;
import com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.core.Values;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexLiteral;

class PhysicalValues extends Values implements PhysicalRel {
    protected PhysicalValues(RelOptCluster cluster,
                             RelDataType rowType,
                             ImmutableList<ImmutableList<RexLiteral>> tuples,
                             RelTraitSet traits) {
        super(cluster, rowType, tuples, traits);
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
        return super.computeSelfCost(planner, mq);
    }

    @Override
    public RowSource compile(ThenRowSink next) {
        return RelLiteral.compile(this, next);
    }
}
