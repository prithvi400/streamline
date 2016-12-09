package org.apache.streamline.streams.runtime.storm.bolt;

import org.apache.storm.tuple.Tuple;
import org.apache.storm.windowing.TimestampExtractor;
import org.apache.streamline.streams.StreamlineEvent;

/**
 * Extract timestamp value from streamline event.
 */
public class StreamlineTimestampExtractor implements TimestampExtractor {
    private final String fieldName;

    public StreamlineTimestampExtractor(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public long extractTimestamp(Tuple tuple) {
        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        Long ts = (Long) event.get(fieldName);
        if (ts == null) {
            throw new IllegalArgumentException("Streamline event does not contain a long value in field: " + fieldName);
        }
        return ts;
    }
}
