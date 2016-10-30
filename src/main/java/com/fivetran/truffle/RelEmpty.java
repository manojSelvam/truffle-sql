package com.fivetran.truffle;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

class RelEmpty extends RowSource {

    private final FrameDescriptor resultType;

    public RelEmpty(SourceSection source, RootNode then) {
        super(source, then);

        this.resultType = new FrameDescriptor();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        then.execute(Truffle.getRuntime().createVirtualFrame(new Object[] { }, resultType));

        return QueryReturn.INSTANCE;
    }
}
