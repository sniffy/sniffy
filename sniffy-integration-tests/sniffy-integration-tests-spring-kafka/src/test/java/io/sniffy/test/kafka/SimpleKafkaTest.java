package io.sniffy.test.kafka;

import io.sniffy.socket.DisableSockets;
import io.sniffy.test.junit.SniffyRule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@EmbeddedKafka(partitions = 1, topics = { "testTopic" })
public class SimpleKafkaTest {

    private static final String TEST_TOPIC = "testTopic";

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Rule public final SniffyRule sniffy = new SniffyRule();

    @Test
    public void testReceivingKafkaEvents() {
        Consumer<Integer, String> consumer = configureConsumer();
        Producer<Integer, String> producer = configureProducer();

        producer.send(new ProducerRecord<>(TEST_TOPIC, 123, "my-test-value"));

        ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, TEST_TOPIC);
        assertNotNull(singleRecord);
        assertEquals(Integer.valueOf(123), singleRecord.key());
        assertEquals("my-test-value", singleRecord.value());

        consumer.close();
        producer.close();
    }

    @Test
    @DisableSockets
    public void testDisabledKafkaProducer() throws Exception {
        Producer<Integer, String> producer = configureProducer();

        Future<RecordMetadata> recordMetadataFuture = producer.send(new ProducerRecord<>(TEST_TOPIC, 123, "my-test-value"));

        try {
            recordMetadataFuture.get(3000, TimeUnit.MILLISECONDS);
            fail("Should have thrown timeout exception");
        } catch (ExecutionException e) {
            assertNotNull(e);
            assertTrue(e.getCause() instanceof TimeoutException);
        }

        producer.close();
    }

    private Consumer<Integer, String> configureConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<Integer, String> consumer = new DefaultKafkaConsumerFactory<Integer, String>(consumerProps)
                .createConsumer();
        consumer.subscribe(Collections.singleton(TEST_TOPIC));
        return consumer;
    }

    private Producer<Integer, String> configureProducer() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producerProps.put("linger.ms", "50");
        producerProps.put("max.block.ms", "2500");
        producerProps.put("request.timeout.ms", "2000");
        producerProps.put("delivery.timeout.ms", "2250");
        return new DefaultKafkaProducerFactory<Integer, String>(producerProps).createProducer();
    }
}
