package etljobs.spark

import etljobs.utils.GlobalProperties
import org.apache.log4j.Logger
import org.apache.spark.sql.{Column, DataFrame, SparkSession}

import scala.util.Try

trait SparkManager {
  private val ic_logger = Logger.getLogger(getClass.getName)
  val global_properties: Option[GlobalProperties]
  ic_logger.info(f"======> Loaded SparkManager(${getClass.getName})")

  implicit class DataFrameHelper(df : DataFrame) {
    var column_count = 0
    def hasColumn(columnName : String) : Boolean = Try(df(columnName)).isSuccess
    def withColumnExtended(columnName : String, expression : Column) : DataFrame = {
      ic_logger.info(s"Column added $columnName with expression ${expression.expr}")
      df.withColumn(columnName, expression)
    }
    def columnCount: Int = column_count
  }

  lazy val spark: SparkSession = createSparkSession(global_properties)

  def createSparkSession(gp: Option[GlobalProperties]): SparkSession = {
    gp match {
      case Some(ss) =>
        if (ss.running_environment =="gcp") {
        if (SparkSession.getActiveSession.isDefined) {
          val spark = SparkSession.getActiveSession.get
          ic_logger.info("################# Using Already Created Spark Session ####################")
          spark
        }
        else {
          val spark = SparkSession.builder()
            .config("spark.default.parallelism", ss.spark_concurrent_threads)
            .config("spark.sql.shuffle.partitions", ss.spark_shuffle_partitions)
            .config("spark.sql.sources.partitionOverwriteMode", ss.spark_output_partition_overwrite_mode)
            .enableHiveSupport()
            .getOrCreate()

          ic_logger.info("##################### Created SparkSession ##########################")
          ic_logger.info("spark.sparkContext.uiWebUrl = " + spark.sparkContext.uiWebUrl)
          ic_logger.info("spark.sparkContext.applicationId = " + spark.sparkContext.applicationId)
          ic_logger.info("spark.sparkContext.sparkUser = " + spark.sparkContext.sparkUser)
          ic_logger.info("spark.eventLog.dir = " + spark.conf.getOption("spark.eventLog.dir"))
          ic_logger.info("spark.eventLog.enabled = " + spark.conf.getOption("spark.eventLog.enabled"))
          spark.conf.getAll.filter(m1 => m1._1.contains("yarn")).foreach(kv => ic_logger.info(kv._1 + " = " + kv._2))
          ic_logger.info("#####################################################################")

          spark
        }

      }
        else if (ss.running_environment =="aws") {
          if (SparkSession.getActiveSession.isDefined) {
            SparkSession.getActiveSession.get
          }
          else {
            val spark = SparkSession.builder()
              .config("spark.default.parallelism", ss.spark_concurrent_threads)
              .config("spark.sql.shuffle.partitions", ss.spark_shuffle_partitions)
              .config("spark.sql.sources.partitionOverwriteMode", ss.spark_output_partition_overwrite_mode)
              .config("fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
              .config("fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
              .config("fs.gs.project.id", ss.gcp_project)
              .config("fs.gs.auth.service.account.enable", "true")
              .config("google.cloud.auth.service.account.json.keyfile",ss.gcp_project_key_name)
              .enableHiveSupport()
              .getOrCreate()

            spark
          }
        }
        else if (ss.running_environment == "local") {
          if (SparkSession.getActiveSession.isDefined) {
            SparkSession.getActiveSession.get
          }
          else {
            val spark = SparkSession.builder().master("local[*]")
              .config("fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
              .config("fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
              .config("fs.gs.project.id", ss.gcp_project)
              .config("fs.gs.auth.service.account.enable", "true")
              .config("google.cloud.auth.service.account.json.keyfile",ss.gcp_project_key_name)
              .getOrCreate()

            ic_logger.info("##################### Created SparkSession ##########################")
            ic_logger.info("spark.sparkContext.uiWebUrl = " + spark.sparkContext.uiWebUrl)
            ic_logger.info("spark.sparkContext.applicationId = " + spark.sparkContext.applicationId)
            ic_logger.info("spark.sparkContext.sparkUser = " + spark.sparkContext.sparkUser)
            ic_logger.info("spark.eventLog.dir = " + spark.conf.getOption("spark.eventLog.dir"))
            ic_logger.info("spark.eventLog.enabled = " + spark.conf.getOption("spark.eventLog.enabled"))
            spark.conf.getAll.filter(m1 => m1._1.contains("yarn")).foreach(kv => ic_logger.info(kv._1 + " = " + kv._2))
            ic_logger.info("#####################################################################")

            spark
          }
        }
        else {
          throw new Exception("Exception occurred! Please provide correct value of property running_environment in loaddata.properties. Expected values are gcp or aws or local")
        }
      case None => SparkSession.builder().master("local[*]")
        .config("fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
        .config("fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
        .getOrCreate()
    }
  }
}
