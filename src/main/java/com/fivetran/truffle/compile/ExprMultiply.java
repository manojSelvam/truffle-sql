package com.fivetran.truffle.compile;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "*")
abstract class ExprMultiply extends ExprBinary {

    @Specialization
    protected long mul(long left, long right) {
        // Don't use ExactMath because we don't want to throw ArithmeticException
        return left * right;
    }

    @Specialization
    protected double mul(double left, double right) {
        return left * right;
    }

    @Specialization
    protected SqlNull leftNull(SqlNull left, Object right) {
        return SqlNull.INSTANCE;
    }

    @Specialization
    protected SqlNull rightNull(Object left, SqlNull right) {
        return SqlNull.INSTANCE;
    }
}
