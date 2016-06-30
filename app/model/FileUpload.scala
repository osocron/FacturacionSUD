package model

import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.Future
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import slick.lifted.ProvenShape

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by osocron on 6/21/16.
  */
case class FileUpload(idFile: Int,
                      namePDF: String,
                      nameXML: String,
                      contentPDF: Array[Byte],
                      contentXML: Array[Byte],
                      importe: BigDecimal,
                      mesUpload: String,
                      diaUpload: Int,
                      anioUpload: Int,
                      noGasto: Long)

case class FileUploadFormData(importe: BigDecimal)

object FileUploadForm {
  val form = Form(mapping("Importe" -> bigDecimal)(FileUploadFormData.apply)(FileUploadFormData.unapply))
}

class FileUploadTable(tag: Tag) extends Table[FileUpload](tag, "file") {

  val gastos = TableQuery[GastoTable]

  def idFile = column[Int]("idfile", O.PrimaryKey, O.AutoInc)
  def namePDF = column[String]("namePDF")
  def nameXML = column[String]("nameXML")
  def contentPDF = column[Array[Byte]]("contentPDF")
  def contentXML = column[Array[Byte]]("contentXML")
  def importe = column[BigDecimal]("importe")
  def mes = column[String]("mesUpload")
  def dia = column[Int]("diaUpload")
  def anio = column[Int]("anioUpload")
  def noGasto = column[Long]("noGasto")
  def gasto = foreignKey("fk_file_1", noGasto, gastos)(_.noGasto,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)
  override def * : ProvenShape[FileUpload] = (idFile, namePDF, nameXML, contentPDF, contentXML,
    importe, mes, dia, anio, noGasto)<>(FileUpload.tupled, FileUpload.unapply)

}

class FileUploadDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]{

  val files = TableQuery[FileUploadTable]

  val gastos = TableQuery[GastoTable]

  def add(fileUpload: FileUpload): Future[String] =
    db.run(files += fileUpload).map(res => "Gasto agregado").recover {
      case ex: Exception => ex.getCause.getMessage
    }

  def delete(idFile: Int): Future[Int] = db.run(files.filter(_.idFile === idFile).delete)

  def get(idFile: Int): Future[Option[FileUpload]] =
    db.run(files.filter(_.idFile === idFile).result.headOption)

  def getAll: Future[Seq[FileUpload]] = db.run(files.result)

  def getFilesByNoGasto(noGasto: Long): Future[Seq[FileUpload]] =
    db.run(files.filter(_.noGasto === noGasto).result)

  def updateImporte(idFile: Int, importe: BigDecimal): Future[String] =
    db.run(files.filter(_.idFile === idFile).map(_.importe).update(importe)).map(_ =>
      "Upload actualizado").recover {
      case ex: Exception => ex.getCause.getMessage
    }

}