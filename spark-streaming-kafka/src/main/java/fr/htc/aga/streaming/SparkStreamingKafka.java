package fr.htc.aga.streaming;

import static fr.htc.aga.common.Constants.APPLICATION_NAME;
import static fr.htc.aga.common.Constants.ENVIRONNEMENT_NAME;
import static fr.htc.aga.common.Constants.KAFKA_BOOTSTRAP;
import static fr.htc.aga.common.Constants.KAFKA_CONSUMER_GROUP_ID;
import static fr.htc.aga.common.Constants.KAFKA_TOPIC_NAME;
import static fr.htc.aga.common.Constants.SPARK_MASTER_NAME;
import static fr.htc.aga.common.Constants.ZK_OFFSET_COMMIT_ROOT_PATH;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.rdd.RDD;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.HasOffsetRanges;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.apache.spark.streaming.kafka010.OffsetRange;
import org.apache.zookeeper.KeeperException;

import fr.htc.aga.common.Constants;

public class SparkStreamingKafka {

	/**
	 * 
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws KeeperException
	 */
	public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
		int duration = 30;
	
		ZKOffSetManager zkOffSetManager = new ZKOffSetManager(Constants.ZK_CONNECTION_STRING, ZK_OFFSET_COMMIT_ROOT_PATH, KAFKA_CONSUMER_GROUP_ID);

		JavaStreamingContext jssc = buildSparkStreamingContext(ENVIRONNEMENT_NAME, APPLICATION_NAME, duration);

		JavaInputDStream<ConsumerRecord<String, String>> stream = buildStreamFromEarliestOffset(jssc, KAFKA_BOOTSTRAP,
				KAFKA_TOPIC_NAME, KAFKA_CONSUMER_GROUP_ID);

		stream.foreachRDD(new VoidFunction<JavaRDD<ConsumerRecord<String, String>>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void call(JavaRDD<ConsumerRecord<String, String>> consumerRecordJavaRDD) throws Exception {
				RDD<ConsumerRecord<String, String>> rdd = consumerRecordJavaRDD.rdd();
				handleRDDStream(rdd);
				// Retrieving the offsets of the RDD
				OffsetRange[] offsetRanges = ((HasOffsetRanges) rdd).offsetRanges();

				// Once RDD handled, offset are commited to ZK
				zkOffSetManager.commitOffset(offsetRanges);
			}
		});

		jssc.start();
		jssc.awaitTermination();
	}
	
	/**
	 * 
	 * @param env
	 * @param appName
	 * @param duration
	 * @return
	 */

	// Build a Spark streaming Context and assign params like : Env name and App name
	
	public static JavaStreamingContext buildSparkStreamingContext(String env, String appName, int duration) {
		SparkConf conf = new SparkConf().setAppName(appName);
		if (env.equals(ENVIRONNEMENT_NAME)) {
			conf = conf.setMaster(SPARK_MASTER_NAME);
		}
		
		// duration's meaning  = duration of the spark's kept data before be cleaned 
		JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(duration));
		return jssc;
	}
	
	/**
	 * Input Streaming reading kafka topic from the earliest offset
	 * @param jssc
	 * @param kafkaBootstrap
	 * @param topic
	 * @param consumerGrp
	 * @return
	 */
	private static JavaInputDStream<ConsumerRecord<String, String>> buildStreamFromEarliestOffset(
			JavaStreamingContext jssc, String kafkaBootstrap, String topic, String consumerGrp) {

		Map<String, Object> kafkaParams = new HashMap<String, Object>();

		kafkaParams.put("bootstrap.servers", kafkaBootstrap); //because a consumer requires bootstrap servers to consume messages from any  requested topic 
		kafkaParams.put("key.deserializer", StringDeserializer.class);// deserializer for Kafka stream (only Key) using Spark structured streaming
		kafkaParams.put("value.deserializer", StringDeserializer.class); // deserializer for Kafka stream (only Value) using Spark structured streaming
		kafkaParams.put("group.id", consumerGrp); // setting consumer's group name (API wich consume data from kafka ex : Spark)
		kafkaParams.put("auto.offset.reset", "earliest"); //consume data from the beginning of the parition ; from partition [0] 
		
		/*Manually Commit offsetRanges to Kafka (setting arg to FALSE  : 
		if the Consumer crashes whilst processing a message it will start consuming from that same Offset, so no messages lost.*/
		
		kafkaParams.put("enable.auto.commit", false); 
		// LocationStrategies.PreferConsistent() : This will distribute partitions evenly across available executors 
		return KafkaUtils.createDirectStream(jssc, LocationStrategies.PreferConsistent(),
				ConsumerStrategies.Subscribe(Arrays.asList(topic), kafkaParams));
	}
	
	/**
	 * 
	 * @param streamRDD
	 */
	public static void handleRDDStream(RDD<ConsumerRecord<String, String>> streamRDD) {
		// TODO Do what ever you want with the RDD
	}

}
