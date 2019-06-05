package com.cmartin

import java.util.{ Properties, UUID }

import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{ ConsumerConfig, KafkaConsumer }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerConfig }
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }

package object learn {

  trait Greeting {
    lazy val greeting: String = "simple-application-hello"
  }

  trait KafkaConf {
    // config object
    val conf = ConfigFactory.load("kafka.conf")

    // kafka config
    lazy val kafkaHost = conf.getString("kafka.host")
    lazy val kafkaPort = conf.getInt("kafka.port")
    lazy val kafkaTopic = conf.getString("kafka.topic")
  }

  trait SimpleProducer extends KafkaConf {
    lazy val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$kafkaHost:$kafkaPort")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getCanonicalName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getCanonicalName)
    //props.put(ProducerConfig.RETRIES_CONFIG, "5")

    lazy val producer = new KafkaProducer[String, String](props)
  }

  trait SimpleConsumer extends KafkaConf {
    lazy val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$kafkaHost:$kafkaPort")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getCanonicalName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getCanonicalName)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")

    props.put("group.id", UUID.randomUUID().toString)

    lazy val consumer = new KafkaConsumer[String, String](props)
  }

}
