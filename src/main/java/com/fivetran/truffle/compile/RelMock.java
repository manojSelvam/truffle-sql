package com.fivetran.truffle.compile;

import com.fivetran.truffle.Types;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.Shape;
import org.apache.calcite.rel.type.RelDataType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Mock table used in testing expressions like SELECT * FROM TABLE(mock())
 */
public class RelMock extends RowSourceSimple {
    private final RelDataType relType;
    private final Class<?> type;
    private final Object[] rows;

    public static RelMock compile(RelDataType relType, Class<?> type, Object[] rows, ThenRowSink then) {
        FrameDescriptorPart frame = FrameDescriptorPart.root(relType.getFieldCount());
        RowSink sink = then.apply(frame);

        return new RelMock(relType, frame, type, rows, sink);
    }

    private RelMock(RelDataType relType, FrameDescriptorPart frame, Class<?> type, Object[] rows, RowSink then) {
        super(frame, then);

        assert relType.getFieldCount() == frame.size();

        this.relType = relType;
        this.type = type;
        this.rows = rows;
    }

    @Override
    protected void executeVoid() {
        try {
            VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(new Object[]{}, sourceFrame.frame());
            Field[] fields = type.getFields();

            for (Field each : fields)
                each.setAccessible(true);

            for (Object row : rows) {
                for (int column = 0; column < fields.length; column++) {
                    FrameSlot slot = sourceFrame.findFrameSlot(column);
                    Object rawValue = fields[column].get(row);
                    RelDataType type = relType.getFieldList().get(column).getType();
                    Object truffleValue = coerce(rawValue, type);

                    setSlot(frame, slot, truffleValue);
                }

                then.executeVoid(frame);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static void setSlot(VirtualFrame frame, FrameSlot slot, Object truffleValue) {
        Objects.requireNonNull(truffleValue);

        if (truffleValue == SqlNull.INSTANCE) {
            slot.setKind(FrameSlotKind.Object);
            frame.setObject(slot, SqlNull.INSTANCE);
        }

        switch (slot.getKind()) {
            case Long:
            case Int:
                slot.setKind(FrameSlotKind.Long);
                frame.setLong(slot, (long) truffleValue);

                break;
            case Float:
            case Double:
                slot.setKind(FrameSlotKind.Double);
                frame.setDouble(slot, (double) truffleValue);

                break;
            case Boolean:
                slot.setKind(FrameSlotKind.Boolean);
                frame.setBoolean(slot, (boolean) truffleValue);

                break;
            default:
                slot.setKind(FrameSlotKind.Object);
                frame.setObject(slot, truffleValue);
        }
    }

    private static Object coerce(Object value, RelDataType type) {
        if (value == null)
            return SqlNull.INSTANCE;

        switch (type.getSqlTypeName()) {
            case ROW:
                return truffleObject(value, type);
            default:
                return Types.coerceAny(value, type);
        }
    }

    /**
     * Convert a Java object to a TruffleObject using reflection.
     * This is very slow! This should only be used for mocks.
     */
    private static TruffleObject truffleObject(Object value, RelDataType type) {
        Shape shape = TruffleSqlContext.LAYOUT.createShape(SqlObjectType.INSTANCE);
        Class<?> clazz = value.getClass();
        Field[] fields = clazz.getFields();
        Object[] fieldValues = new Object[fields.length];

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            RelDataType fieldType = type.getFieldList().get(i).getType();

            assert !Modifier.isStatic(field.getModifiers()) : "Mock records are not allowed to have static fields";

            try {
                Object rawValue = field.get(value);
                Object niceValue = coerce(rawValue, fieldType);

                shape = shape.defineProperty(field.getName(), niceValue, 0);
                fieldValues[i] = niceValue;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return shape.createFactory().newInstance(fieldValues);
    }
}
