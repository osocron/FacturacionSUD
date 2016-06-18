package model

import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.Future
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


case class Gasto(noGasto: Long,
                 concepto: String,
                 importe: BigDecimal,
                 mes: String,
                 dia: Int,
                 anio: Int,
                 comprobado: String,
                 idOrganizacion: Int)

case class GastoFormData(noGasto: Long,
                         concepto: String,
                         importe: BigDecimal,
                         mes: String,
                         dia: Int,
                         anio: Int,
                         comprobado: String)

object GastoForm {

  val form = Form(
    mapping(
      "No. Gasto" -> longNumber,
      "Concepto" -> text,
      "Importe" -> bigDecimal,
      "Mes" -> text,
      "Dia" -> number,
      "Anio" -> number,
      "Comprobado" -> text
    )(GastoFormData.apply)(GastoFormData.unapply)
  )

}

class GastoTable(tag: Tag) extends Table[Gasto](tag, "gasto") {

  val organizaciones = TableQuery[OrganizacionTable]

  def noGasto = column[Long]("noGasto", O.PrimaryKey)
  def concepto = column[String]("concepto")
  def importe = column[BigDecimal]("importe")
  def mes = column[String]("mes")
  def dia = column[Int]("dia")
  def anio = column[Int]("anio")
  def comprobado = column[String]("comprobado")
  def idOrganizacion = column[Int]("idorganizacion")
  def organizacion = foreignKey("fk_gasto_1", idOrganizacion, organizaciones)(_.id,
    onUpdate = ForeignKeyAction.Cascade,
    onDelete = ForeignKeyAction.Cascade)
  override def * = (noGasto, concepto, importe, mes, dia, anio, comprobado, idOrganizacion)<>(Gasto.tupled, Gasto.unapply)

}

class GastoDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val gastos = TableQuery[GastoTable]

  def add(gasto: Gasto): Future[String] =
    db.run(gastos += gasto).map(res => "Gasto agregado").recover {
      case ex: Exception => ex.getCause.getMessage
    }

  def delete(noGasto: Long): Future[Int] =
    db.run(gastos.filter(_.noGasto === noGasto).delete)

  def get(noGasto: Long): Future[Option[Gasto]] =
    db.run(gastos.filter(_.noGasto === noGasto).result.headOption)

  def getAll: Future[Seq[Gasto]] = db.run(gastos.result)

  def getGastoByOrg(id: Int): Future[Seq[Gasto]] = db.run(gastos.filter(_.idOrganizacion === id).result)

}