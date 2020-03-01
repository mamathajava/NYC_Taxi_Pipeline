package DataTransform

import org.apache.spark.sql.DataFrame
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StructType, StructField, StringType, IntegerType,LongType,FloatType,DoubleType, TimestampType}
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import java.util.Calendar
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.sql._



//import org.apache.hadoop.conf.Configuration
//import org.apache.hadoop.fs.{ FileSystem, Path }


object TransformYelloTaxiData { 

    def main(args: Array[String]){ 

        // Use new SparkSession interface in Spark 2.0

        val sc = new SparkContext("local[*]", "TransformYelloTaxiData")   
        val sqlContext = new org.apache.spark.sql.SQLContext(sc)
        val spark = SparkSession
          .builder
          .appName("TransformYelloTaxiData")
          .master("local[*]")
          .config("spark.sql.warehouse.dir", "/temp") // Necessary to work around a Windows bug in Spark 2.0.0; omit if you're not on Windows.
          .getOrCreate()
        
        // Convert our csv file to a DataSet, using our Person case
        // class to infer the schema.
        import spark.implicits._

        //Destination directory
        val destDataDirRoot =  "data/output/transactions/yellow-taxi" 
        val srcDataFile = "data/processed"

        val vendor_lookup = spark.read
                                 .option("header","true")
                                 .option("delimiter", ",")
                                 .csv(srcDataFile + "/reference/vendor/" + "*.csv")

        val trip_type = spark.read
                             .option("header","true")
                             .option("delimiter", ",")
                             .csv(srcDataFile + "/reference/trip-type/" + "*.csv")

        val trip_month = spark.read
                             .option("header","true")
                             .option("delimiter", ",")
                             .csv(srcDataFile + "/reference/trip-month/" + "*.csv")


        val trip_zone = spark.read
                             .option("header","true")
                             .option("delimiter", ",")
                             .csv(srcDataFile + "/reference/taxi-zone/" + "*.csv")


        val rate_code = spark.read
                             .option("header","true")
                             .option("delimiter", ",")
                             .csv(srcDataFile + "/reference/rate-code/" + "*.csv")


        val payment_type = spark.read
                             .option("header","true")
                             .option("delimiter", ",")
                             .csv(srcDataFile + "/reference/payment-type/" + "*.csv")

        val yellow_taxi = spark.read
                             .option("header","true")
                             .option("delimiter", ",")
                             .csv(srcDataFile + "/yellow-taxi/*/*/" + "*.csv")

        val green_taxi = spark.read
                             .option("header","true")
                             .option("delimiter", ",")
                             .csv(srcDataFile + "/green-taxi/*/*/" + "*.csv")

        val curatedDF = spark.sql("""
        select distinct t.taxi_type,
            t.vendor_id as vendor_id,
            t.pickup_datetime,
            t.dropoff_datetime,
            t.store_and_fwd_flag,
            t.rate_code_id,
            t.pickup_location_id,
            t.dropoff_location_id,
            t.pickup_longitude,
            t.pickup_latitude,
            t.dropoff_longitude,
            t.dropoff_latitude,
            t.passenger_count,
            t.trip_distance,
            t.fare_amount,
            t.extra,
            t.mta_tax,
            t.tip_amount,
            t.tolls_amount,
            t.improvement_surcharge,
            t.total_amount,
            t.payment_type,
            t.trip_year,
            t.trip_month,
            v.abbreviation as vendor_abbreviation,
            v.description as vendor_description,
            tm.month_name_short,
            tm.month_name_full,
            pt.description as payment_type_description,
            rc.description as rate_code_description,
            tzpu.borough as pickup_borough,
            tzpu.zone as pickup_zone,
            tzpu.service_zone as pickup_service_zone,
            tzdo.borough as dropoff_borough,
            tzdo.zone as dropoff_zone,
            tzdo.service_zone as dropoff_service_zone,
            year(t.pickup_datetime) as pickup_year,
            month(t.pickup_datetime) as pickup_month,
            day(t.pickup_datetime) as pickup_day,
            hour(t.pickup_datetime) as pickup_hour,
            minute(t.pickup_datetime) as pickup_minute,
            second(t.pickup_datetime) as pickup_second,
            date(t.pickup_datetime) as pickup_date,
            year(t.dropoff_datetime) as dropoff_year,
            month(t.dropoff_datetime) as dropoff_month,
            day(t.dropoff_datetime) as dropoff_day,
            hour(t.dropoff_datetime) as dropoff_hour,
            minute(t.dropoff_datetime) as dropoff_minute,
            second(t.dropoff_datetime) as dropoff_second,
            date(t.dropoff_datetime) as dropoff_date
        from 
          taxi_db.yellow_taxi_trips_raw t
          left outer join taxi_db.vendor_lookup v 
            on (t.vendor_id = case when t.trip_year < "2015" then v.abbreviation else v.vendor_id end)
          left outer join taxi_db.trip_month_lookup tm 
            on (t.trip_month = tm.trip_month)
          left outer join taxi_db.payment_type_lookup pt 
            on (t.payment_type = case when t.trip_year < "2015" then pt.abbreviation else pt.payment_type end)
          left outer join taxi_db.rate_code_lookup rc 
            on (t.rate_code_id = rc.rate_code_id)
          left outer join taxi_db.taxi_zone_lookup tzpu 
            on (t.pickup_location_id = tzpu.location_id)
          left outer join taxi_db.taxi_zone_lookup tzdo 
            on (t.dropoff_location_id = tzdo.location_id)
        """)

      val curatedDFConformed = curatedDF.withColumn("temp_vendor_id",col("vendor_id").cast(IntegerType)).drop("vendor_id")
                                .withColumnRenamed("temp_vendor_id", "vendor_id")
                                .withColumn("temp_payment_type", col("payment_type").cast(IntegerType)).drop("payment_type")
                                .withColumnRenamed("temp_payment_type", "payment_type")

      // COMMAND ----------

      //Destination directory

      //Delete any residual data from prior executions for an idempotent run
      //dbutils.fs.rm(destDataDirRoot,recurse=true)

      // COMMAND ----------

      //Save as Delta, partition by year and month
      curatedDFConformed
          .coalesce(10)
          .write
          .format("delta")
          .mode("append")
          .partitionBy("trip_year","trip_month")
          .save(destDataDirRoot)   

      // COMMAND ----------

      //Cluster conf: 3 autoscale to 6 workers - DS4v2 (with DS13vs driver) - 8 cores, 28 GB of RAM/worker | Yellow + green running together with 128 MB raw delta files | Coalesce 15 | 1 hr 45 mins
      //Cluster conf: 3 autoscale to 6 workers - DS4v2 (with DS13vs driver) - 8 cores, 28 GB of RAM/worker | Yellow + green running together with 128 MB raw delta files | Coalesce 25 | 1 hr 45 mins
      //2016-2017 data, coalesce 10: 

      // COMMAND ----------
    }
}