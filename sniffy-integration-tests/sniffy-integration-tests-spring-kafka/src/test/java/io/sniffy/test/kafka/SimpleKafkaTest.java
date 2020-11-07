package io.sniffy.test.kafka;

import io.sniffy.socket.DisableSockets;
import io.sniffy.test.junit.SniffyRule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@EmbeddedKafka(partitions = 1/*, topics = { "testTopic" }*/)
public class SimpleKafkaTest {

    private static final String TEST_TOPIC = "testTopic";

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Rule
    public final SniffyRule sniffy = new SniffyRule();

    @Test
    public void testReceivingKafkaEvents() {
        Consumer<Integer, String> consumer = configureConsumer();
        Producer<Integer, String> producer = configureProducer();

        producer.send(new ProducerRecord<>(TEST_TOPIC, 123, "my-test-value"));

        ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, TEST_TOPIC);
        assertNotNull(singleRecord);
        assertEquals(new Integer(123), singleRecord.key());
        assertEquals("my-test-value", singleRecord.value());

        consumer.close();
        producer.close();
    }

    @Test
    //@DisableSockets
    public void testDisabledKafkaProducer() {
        Consumer<Integer, String> consumer = configureConsumer();
        Producer<Integer, String> producer = configureProducer();

        producer.send(new ProducerRecord<>(TEST_TOPIC, 123, "my-test-value"));

        ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, TEST_TOPIC);
        assertNotNull(singleRecord);
        assertEquals(new Integer(123), singleRecord.key());
        assertEquals("my-test-value", singleRecord.value());

        consumer.close();
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
        return new DefaultKafkaProducerFactory<Integer, String>(producerProps).createProducer();
    }
}
