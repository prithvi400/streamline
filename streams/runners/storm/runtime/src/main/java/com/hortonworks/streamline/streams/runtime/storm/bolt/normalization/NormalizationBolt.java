/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/

package com.hortonworks.streamline.streams.runtime.storm.bolt.normalization;

import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.impl.normalization.NormalizationProcessor;
import com.hortonworks.streamline.streams.runtime.normalization.NormalizationProcessorRuntime;
import com.hortonworks.streamline.streams.runtime.storm.bolt.AbstractProcessorBolt;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class NormalizationBolt extends AbstractProcessorBolt {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizationBolt.class);
    private final NormalizationProcessor normalizationProcessor;

    private NormalizationProcessorRuntime normalizationProcessorRuntime;

    public NormalizationBolt(NormalizationProcessor normalizationProcessor) {
        this.normalizationProcessor = normalizationProcessor;
    }

    public NormalizationBolt(String normalizationProcessorJson) {
        this(Utils.createObjectFromJson(normalizationProcessorJson, NormalizationProcessor.class));
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        if (normalizationProcessor == null) {
            throw new RuntimeException("normalizationProcessor cannot be null");
        }
        normalizationProcessorRuntime = new NormalizationProcessorRuntime(normalizationProcessor);
        normalizationProcessorRuntime.initialize(Collections.<String, Object>emptyMap());
    }

    public void process(Tuple inputTuple, StreamlineEvent event) throws Exception {
        LOG.debug("Normalizing received StreamlineEvent: [{}] with tuple: [{}]", event, inputTuple);
        //todo this bolt will be replaced with custom baseprocessor bolt.
        StreamlineEventImpl eventWithStream = StreamlineEventImpl.builder()
                .from(event)
                .sourceStream(inputTuple.getSourceStreamId())
                .build();
        List<Result> outputEvents = normalizationProcessorRuntime.process(eventWithStream);
        LOG.debug("Emitting events to collector: [{}]", outputEvents);
        for (Result outputEvent : outputEvents) {
            for (StreamlineEvent e : outputEvent.events) {
                collector.emit(outputEvent.stream, inputTuple, new Values(e));
            }
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        if (normalizationProcessor == null) {
            throw new RuntimeException("normalizationProcessor cannot be null");
        }
        for (Stream stream : normalizationProcessor.getOutputStreams()) {
            declarer.declareStream(stream.getId(), new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
    }
}
