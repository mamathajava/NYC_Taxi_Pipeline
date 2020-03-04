package SaveToDB

import org.apache.spark.sql.DataFrame
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StructType, StructField, StringType, IntegerType,LongType,FloatType,DoubleType, TimestampType}
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import java.util.Calendar

import org.apache.hadoop.fs.{ FileSystem, Path }
import org.apache.hadoop.conf.Configuration


object JDBCDataToDB { 

  def main(args: Array[String]){ 

      val sc = new SparkContext("local[*]", "JDBCDataToDB")   
      val sqlContext = new org.apache.spark.sql.SQLContext(sc)
      val spark = SparkSession
        .builder
        .appName("JDBCDataToDB")
        .master("local[*]")
        .config("spark.sql.warehouse.dir", "/temp") // Necessary to work around a Windows bug in Spark 2.0.0; omit if you're not on Windows.
        .config("spark.network.timeout", "6000s") // https://stackoverflow.com/questions/48219169/3600-seconds-timeout-that-spark-worker-communicating-with-spark-driver-in-heartb
        .config("spark.executor.heartbeatInterval", "10000s")
        .config("spark.executor.memory", "10g")
        .getOrCreate()

      import spark.implicits._ 

      // https://spark.apache.org/docs/2.2.0/sql-programming-guide.html#hive-tables
      


      // Note: JDBC loading and saving can be achieved via either the load/save or jdbc methods
      // Loading data from a JDBC source
      // val jdbcDF = spark.read
      //   .format("jdbc")
      //   .option("url", "jdbc:postgresql:dbserver")
      //   .option("dbtable", "schema.tablename")
      //   .option("user", "username")
      //   .option("password", "password")
      //   .load()

      // val connectionProperties = new Properties()
      // connectionProperties.put("user", "username")
      // connectionProperties.put("password", "password")
      // val jdbcDF2 = spark.read
      //   .jdbc("jdbc:postgresql:dbserver", "schema.tablename", connectionProperties)

      // // Saving data to a JDBC source
      // jdbcDF.write
      //   .format("jdbc")
      //   .option("url", "jdbc:postgresql:dbserver")
      //   .option("dbtable", "schema.tablename")
      //   .option("user", "username")
      //   .option("password", "password")
      //   .save()

      // jdbcDF2.write
      //   .jdbc("jdbc:postgresql:dbserver", "schema.tablename", connectionProperties)

      // // Specifying create table column data types on write
      // jdbcDF.write
      //   .option("createTableColumnTypes", "name CHAR(64), comments VARCHAR(1024)")
      //   .jdbc("jdbc:postgresql:dbserver", "schema.tablename", connectionProperties)



  }

}