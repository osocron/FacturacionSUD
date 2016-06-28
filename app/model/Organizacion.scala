package model

import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

import scala.concurrent.Future
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by osocron on 6/15/16.
  */
case class Organizacion(id: Int, user: String, password: String)

case class OrganizacionFormData(user: String, password: String)

object OrganizacionForm{

  val form = Form(
    mapping(
      "user" -> nonEmptyText,
      "password" -> nonEmptyText
    )(OrganizacionFormData.apply)(OrganizacionFormData.unapply)
  )

  val deleteForm = Form(single("id" -> number))

}

class OrganizacionTable(tag: Tag) extends Table[Organizacion](tag, "organizacion") {

  def id = column[Int]("idorganizacion", O.PrimaryKey, O.AutoInc)
  def user = column[String]("user")
  def password = column[String]("password")

  override def * = (id, user, password)<>(Organizacion.tupled, Organizacion.unapply)
}

class OrganizacionesDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  val organizaciones = TableQuery[OrganizacionTable]

  def add(organizacion: Organizacion): Future[String] =
    db.run(organizaciones += organizacion).map(res => "Organizacion agregada").recover{
      case ex: Exception => ex.getCause.getMessage
    }

  def delete(id: Int): Future[Int] =
    db.run(organizaciones.filter(_.id === id).delete)

  def get(id: Int): Future[Option[Organizacion]] =
    db.run(organizaciones.filter(_.id === id).result.headOption)

  def getAll: Future[Seq[Organizacion]] = db.run(organizaciones.result)

  def update(idOrg: Int, organizacion: Organizacion): Future[String] = {
    db.run(organizaciones.filter(_.id === idOrg).update(organizacion)).map(_ =>
      "Organizacion updated").recover{
      case ex: Exception => ex.getCause.getMessage
    }
  }

}
