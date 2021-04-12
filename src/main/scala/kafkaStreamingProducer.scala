package main


import org.apache.spark.{SparkContext,SparkConf}
import org.apache.spark.SparkContext._
import org.apache.spark.storage.StorageLevel
import org.apache.spark._
import org.apache.spark.streaming._

import org.apache.spark.sql.{Dataset, DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.functions.{col, from_json,to_json,struct}

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}


import org.apache.spark.sql.streaming.Trigger

import java.nio.file.{Files, Paths}



class sparkStreamng{
  def streamingFunction(batchDf: DataFrame, batchId: Long): Unit = {
        println("\n\n\t\tBATCH "+batchId+"\n\n")
        batchDf.show(false)
  }
  def kafkaConsume(kafkaTopicName: String = "test", kafkaServer: String = "localhost:9092"): Unit = {
    val conf = new SparkConf().setAppName("KAFKA").setMaster("local");
    val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")
    val spark = SparkSession
    .builder()
    .master("local[4]")
    .appName("Spark Kafka Producer")
    .config(conf)
    .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")
    import spark.implicits._


    System.setProperty("HADOOP_USER_NAME","hadoop")

    val trainsCsv = "/home/xs107-bairoy/xenonstack/l2/module4/spark_streaming_producer/files/trains/cars1.csv"
    
    val trains = spark.read
            .option("header", "true")
            .format("csv")
            .load(trainsCsv)
            .toDF()

    //val path = "/home/xs107-bairoy/xenonstack/l2/module4/spark_streaming_producer/files/data1.json"
    val transactionDF = spark.readStream
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaServer)
                .option("subscribe", kafkaTopicName)
                .option("startingOffsets", "earliest")
                .format("csv")
              .load(trainsCsv)

    println("Printing Schema of transactionDF: ")
    transactionDF.printSchema()

    val schema = new StructType()
      .add("id",IntegerType)
      .add("train_id",IntegerType)
      .add("position",IntegerType)
      .add("shape",StringType)
      .add("len",StringType)
      .add("sides",StringType)
      .add("roof",StringType)
      .add("wheels",IntegerType)
      .add("load_shape",StringType)
      .add("load_num",IntegerType)
      
    
    transactionDF
                .selectExpr("CAST(topic AS STRING)", "CAST(value AS STRING)", "CAST(timestamp AS STRING)")
                .writeStream
                .format("kafka")
                .option("kafka.bootstrap.servers", kafkaServer)
                .option("topic", kafkaTopicName)
                .trigger(Trigger.ProcessingTime("1 seconds"))
                .outputMode("append")
                .foreachBatch(streamingFunction _)
                .option("checkpointLocation","/tmp/spark/kafkaStreamingConsumer")
                .start()
                .awaitTermination()

/*
    val schema = new StructType()
      .add("id",IntegerType)
      .add("firstname",StringType)
      .add("middlename",StringType)
      .add("lastname",StringType)
      .add("dob_year",IntegerType)
      .add("dob_month",IntegerType)
      .add("gender",StringType)
      .add("salary",IntegerType)

    val personDF = df.selectExpr("CAST(value AS STRING)")
      .select(from_json(col("value"), schema).as("data"))
    personDF.printSchema()
      
    personDF.select(to_json(struct("data.*")) as "value")
      .writeStream
      .format("kafka")
      .outputMode("update")
      .option("kafka.bootstrap.servers", kafkaServer)
      .option("topic", kafkaTopicName)
      .option("checkpointLocation","/tmp/spark/kafkaStreamingProducer")
      .start()
      .awaitTermination()*/
      

      spark.close()
    
  }
  def temp(kafkaTopicName: String = "test", kafkaServer: String = "localhost:9092"): Unit = {

    val conf = new SparkConf().setAppName("KAFKA").setMaster("local");
    val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")
    val spark = SparkSession
    .builder()
    .master("local[4]")
    .appName("Spark Kafka Producer")
    .config(conf)
    .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")
    import spark.implicits._


    System.setProperty("HADOOP_USER_NAME","hadoop")


    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaServer)
      .option("subscribe", kafkaTopicName)
      .option("startingOffsets", "earliest")
      .load()    
    df.printSchema()


    val schema = new StructType()
      .add("id",IntegerType)
      .add("firstname",StringType)
      .add("middlename",StringType)
      .add("lastname",StringType)
      .add("dob_year",IntegerType)
      .add("dob_month",IntegerType)
      .add("gender",StringType)
      .add("salary",IntegerType)

    val personDF = df.selectExpr("CAST(value AS STRING)")
      .select(from_json(col("value"), schema).as("data"))
    personDF.printSchema()
      
    personDF.select(to_json(struct("data.*")) as "value")
      .writeStream
      .format("kafka")
      .outputMode("update")
      .option("kafka.bootstrap.servers", kafkaServer)
      .option("topic", kafkaTopicName)
      .option("checkpointLocation","/tmp/spark/kafkaStreamingProducer")
      .start()

      spark.close()
  }
  
}
object kafkaStreamingProducer {  
  def main(args: Array[String]): Unit = {
    println("\n\n\t\tKafka Producer Application Started ...\n\n")
    val sS = new sparkStreamng
    sS.kafkaConsume()
    //sS.temp()
    println("\n\n\t\tKafka Producer Application Completed ...\n\n")
  }

  
}


