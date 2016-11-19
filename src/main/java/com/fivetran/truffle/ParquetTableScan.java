package com.fivetran.truffle;

import org.apache.calcite.plan.*;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.parquet.hadoop.Footer;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;

import java.net.URI;

/**
 * Scans a parquet-format file.
 */
public class ParquetTableScan extends TableScan implements CompileRowSource {
    /** Calling convention for Parquet files */
    private static final Convention CONVENTION = new Convention.Impl("TRUFFLE", ParquetTableScan.class);

    /**
     * Location of the file. Could be a local file, S3.
     */
    final URI file;

    /**
     * The schema we want to project from the file.
     * Might be changed by rules in RelOptPlanner to push down projections.
     */
    final MessageType schema;

    ParquetTableScan(RelOptCluster cluster,
                     RelTraitSet traitSet,
                     RelOptTable table,
                     URI file,
                     MessageType schema) {
        super(cluster, traitSet.plus(CONVENTION), table);

        this.file = file;
        this.schema = schema;
    }

    @Override
    public RelDataType deriveRowType() {
        RelDataType tableType = table.getRowType();
        RelDataTypeFactory.FieldInfoBuilder acc = getCluster().getTypeFactory().builder();

        for (Type field : schema.getFields()) {
            acc.add(tableType.getField(field.getName(), true, false));
        }

        return acc.build();
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
        double rows = 0;
        double cpu = 0;
        double io = 0;

        for (Footer footer : Parquets.footers(file)) {
            for (BlockMetaData block : Parquets.blockMetaData(footer)) {
                rows += block.getRowCount();

                for (ColumnChunkMetaData column : block.getColumns()) {
                    if (schema.containsPath(column.getPath().toArray())) {
                        io += column.getTotalSize();
                        cpu += column.getTotalUncompressedSize();
                    }
                }
            }
        }

        return planner.getCostFactory().makeCost(rows, cpu, io);
    }

    @Override
    public void register(RelOptPlanner planner) {
        planner.addRule(RuleProjectParquet.INSTANCE);

        // TODO push down filters
    }

    @Override
    public RowSource compile() {
        return new RelParquet(file, schema);
    }

    public ParquetTableScan withProject(MessageType project) {
        // TODO check project <: schema
        ParquetTableScan result = new ParquetTableScan(getCluster(), traitSet, table, file, project);

        result.deriveRowType();

        return result;
    }
}
