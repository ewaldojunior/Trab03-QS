/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.api.common.io;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.common.operators.base.InnerJoinOperatorBase;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;

import java.io.IOException;

/**
 * A ReplicatingInputFormat replicates any {@link InputFormat} to all parallel instances of a
 * DataSource, i.e., the full input of the replicated InputFormat is completely processed by each
 * parallel instance of the DataSource. This is done by assigning all {@link
 * org.apache.flink.core.io.InputSplit}s generated by the replicated InputFormat to each parallel
 * instance.
 *
 * <p>Replicated data can only be used as input for a {@link InnerJoinOperatorBase} or {@link
 * org.apache.flink.api.common.operators.base.CrossOperatorBase} with the same parallelism as the
 * DataSource. Before being used as an input to a Join or Cross operator, replicated data might be
 * processed in local pipelines by Map-based operators with the same parallelism as the source.
 * Map-based operators are {@link org.apache.flink.api.common.operators.base.MapOperatorBase},
 * {@link org.apache.flink.api.common.operators.base.FlatMapOperatorBase}, {@link
 * org.apache.flink.api.common.operators.base.FilterOperatorBase}, and {@link
 * org.apache.flink.api.common.operators.base.MapPartitionOperatorBase}.
 *
 * <p>Replicated DataSources can be used for local join processing (no data shipping) if one input
 * is accessible on all parallel instance of a join and the other input is (randomly) partitioned
 * across all parallel instances.
 *
 * <p>However, a replicated DataSource is a plan hint that can invalidate a Flink program if not
 * used correctly (see usage instructions above). In such situations, the optimizer is not able to
 * generate a valid execution plan and the program execution will fail.
 *
 * @param <OT> The output type of the wrapped InputFormat.
 * @param <S> The InputSplit type of the wrapped InputFormat.
 * @see org.apache.flink.api.common.io.InputFormat
 * @see org.apache.flink.api.common.io.RichInputFormat
 * @see InnerJoinOperatorBase
 * @see org.apache.flink.api.common.operators.base.CrossOperatorBase
 * @see org.apache.flink.api.common.operators.base.MapOperatorBase
 * @see org.apache.flink.api.common.operators.base.FlatMapOperatorBase
 * @see org.apache.flink.api.common.operators.base.FilterOperatorBase
 * @see org.apache.flink.api.common.operators.base.MapPartitionOperatorBase
 */
@PublicEvolving
public final class ReplicatingInputFormat<OT, S extends InputSplit> extends RichInputFormat<OT, S> {

    private static final long serialVersionUID = 1L;

    private InputFormat<OT, S> replicatedIF;

    public ReplicatingInputFormat(InputFormat<OT, S> wrappedIF) {
        this.replicatedIF = wrappedIF;
    }

    public InputFormat<OT, S> getReplicatedInputFormat() {
        return this.replicatedIF;
    }

    @Override
    public void configure(Configuration parameters) {
        this.replicatedIF.configure(parameters);
    }

    @Override
    public BaseStatistics getStatistics(BaseStatistics cachedStatistics) throws IOException {
        return this.replicatedIF.getStatistics(cachedStatistics);
    }

    @Override
    public S[] createInputSplits(int minNumSplits) throws IOException {
        return this.replicatedIF.createInputSplits(minNumSplits);
    }

    @Override
    public InputSplitAssigner getInputSplitAssigner(S[] inputSplits) {
        return new ReplicatingInputSplitAssigner(inputSplits);
    }

    @Override
    public void open(S split) throws IOException {
        this.replicatedIF.open(split);
    }

    @Override
    public boolean reachedEnd() throws IOException {
        return this.replicatedIF.reachedEnd();
    }

    @Override
    public OT nextRecord(OT reuse) throws IOException {
        return this.replicatedIF.nextRecord(reuse);
    }

    @Override
    public void close() throws IOException {
        this.replicatedIF.close();
    }

    @Override
    public void setRuntimeContext(RuntimeContext context) {
        if (this.replicatedIF instanceof RichInputFormat) {
            ((RichInputFormat) this.replicatedIF).setRuntimeContext(context);
        }
    }

    @Override
    public RuntimeContext getRuntimeContext() {
        if (this.replicatedIF instanceof RichInputFormat) {
            return ((RichInputFormat) this.replicatedIF).getRuntimeContext();
        } else {
            throw new RuntimeException(
                    "The underlying input format to this ReplicatingInputFormat isn't context aware");
        }
    }

    @Override
    public void openInputFormat() throws IOException {
        if (this.replicatedIF instanceof RichInputFormat) {
            ((RichInputFormat) this.replicatedIF).openInputFormat();
        }
    }

    @Override
    public void closeInputFormat() throws IOException {
        if (this.replicatedIF instanceof RichInputFormat) {
            ((RichInputFormat) this.replicatedIF).closeInputFormat();
        }
    }
}
