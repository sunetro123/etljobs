package etljobs.etlsteps

import etljobs.utils.{IOType, ORC, PARQUET}
import org.apache.spark.sql.SparkSession
import com.google.cloud.bigquery.{BigQuery, FormatOptions, JobInfo}
import etljobs.bigquery.LoadApi
import scala.util.{Failure, Success, Try}

class BQLoadStep(
            val name: String
            ,source_path: String = ""
            ,source_dirs: => Seq[String] = Seq()
            ,source_format: IOType
            ,destination_dataset: String
            ,destination_table: String
            ,write_disposition: JobInfo.WriteDisposition = JobInfo.WriteDisposition.WRITE_TRUNCATE
            ,create_disposition: JobInfo.CreateDisposition = JobInfo.CreateDisposition.CREATE_NEVER
            )(bq : => BigQuery, etl_metadata : Map[String, String])
  extends EtlStep[Unit,Unit]
{
  def process(input_state : Unit): Try[Unit] = {
    Try{
      etl_logger.info("#################################################################################################")
      etl_logger.info(s"Starting BQ Data Load Step : $name")

      LoadApi.loadIntoBQFromGCS(
        bq,source_path,source_dirs,source_format
        ,destination_dataset,destination_table,write_disposition,create_disposition
      )
    }
  }

  def map(func: Unit => Unit): Unit = {
    etl_logger.info("Executing inside map")
    val output = Try(process()) match {
      case Success(value) => value
      case Failure(exception) => throw exception
    }
    func(output)
  }

  def flatMap(func: Unit => Unit): Unit = {
    etl_logger.info("Executing inside flatMap")
    val output = Try(process()) match {
      case Success(value) => value
      case Failure(exception) =>
        etl_logger.info(s"Got Error: $exception")
        throw exception
    }
    func(output)
  }

  override def getExecutionMetrics : Map[String, Map[String,String]] = {
    Map()
    //bq_logger.info("Loaded rows: " + destinationTable.getNumRows)
    //bq_logger.info(s"Loaded rows size: ${destinationTable.getNumBytes / 1000000.0} MB")
    //bq_logger.info("#################################################################################################")
  }

  override def getStepProperties : Map[String,String] = {
    Map(
      "source_dirs" -> source_dirs.mkString(",")
      ,"source_path" -> source_path
      ,"destination_dataset" -> destination_dataset
      ,"destination_table" -> destination_table
      ,"write_disposition" -> write_disposition.toString
      ,"create_disposition" -> create_disposition.toString
    )
  }
}

object BQLoadStep {
  def apply( name : String
           ,source_path: String = ""
           ,source_dirs: => Seq[String] = Seq()
           ,source_format: IOType
           ,destination_dataset: String
           ,destination_table: String
           ,write_disposition: JobInfo.WriteDisposition = JobInfo.WriteDisposition.WRITE_TRUNCATE
           ,create_disposition: JobInfo.CreateDisposition = JobInfo.CreateDisposition.CREATE_NEVER
           )(bq: => BigQuery,etl_metadata : Map[String, String]): BQLoadStep = {
    new BQLoadStep(name, source_path, source_dirs, source_format, destination_dataset, destination_table, write_disposition, create_disposition)(bq,etl_metadata)
  }
}