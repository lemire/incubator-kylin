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

package org.apache.kylin.engine.mr.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.kylin.common.hll.HyperLogLogPlusCounter;
import org.apache.kylin.common.util.Bytes;
import org.apache.kylin.cube.kv.RowConstants;

public class CuboidStatsUtil {

   public static void writeCuboidStatistics(Configuration conf, Path outputPath, Map<Long, HyperLogLogPlusCounter> cuboidHLLMap, int samplingPercentage) throws IOException {
        Path seqFilePath = new Path(outputPath, BatchConstants.CFG_STATISTICS_CUBOID_ESTIMATION);
        SequenceFile.Writer writer = SequenceFile.createWriter(conf, SequenceFile.Writer.file(seqFilePath), SequenceFile.Writer.keyClass(LongWritable.class), SequenceFile.Writer.valueClass(BytesWritable.class));

        List<Long> allCuboids = new ArrayList<Long>();
        allCuboids.addAll(cuboidHLLMap.keySet());
        Collections.sort(allCuboids);

        // persist the sample percentage with key 0
        writer.append(new LongWritable(0l), new BytesWritable(Bytes.toBytes(samplingPercentage)));
        ByteBuffer valueBuf = ByteBuffer.allocate(RowConstants.ROWVALUE_BUFFER_SIZE);
        try {
            for (long i : allCuboids) {
                valueBuf.clear();
                cuboidHLLMap.get(i).writeRegisters(valueBuf);
                valueBuf.flip();
                writer.append(new LongWritable(i), new BytesWritable(valueBuf.array(), valueBuf.limit()));
            }
        } finally {
            writer.close();
        }
    }
}
