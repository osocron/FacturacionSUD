package controllers

import javax.inject.Inject

import model._
import play.api.mvc._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.duration._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.{File, FileInputStream, FileOutputStream}
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.util.{Failure, Success, Try}

class Administracion @Inject()(organizacionesDAO: OrganizacionesDAO,
                               gastoDAO: GastoDAO,
                               fileUploadDAO: FileUploadDAO) extends Controller {

  def dashboard = Action.async { request =>
    request.session.get("connected").map { user =>
      organizacionesDAO.getAll.map(res => Ok(views.html.administrador.dashboard(res, OrganizacionForm.form)))
    }.getOrElse {
      Future.successful(Unauthorized("No ha iniciado sesión"))
    }
  }

  def addOrg() = Action.async { implicit request =>
    OrganizacionForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Redirect(routes.Administracion.dashboard())),
      data => {
        val newOrga = Organizacion(0, data.user, data.password)
        organizacionesDAO.add(newOrga).map(res =>
          Redirect(routes.Administracion.dashboard())
        )
      }
    )
  }

  def organizacion(idOrganizacion: Int) = Action.async { request =>
    request.session.get("connected").map { user =>
      gastoDAO.getGastoByOrg(idOrganizacion).map(gastos => {
        if (user.equals("administrador")) {
          Ok(views.html.administrador.organizacion(idOrganizacion, gastos, GastoForm.form))
        } else {
          Ok(views.html.barrio.barrio(idOrganizacion,gastos))
        }
      })
    }.getOrElse {
      Future.successful(Unauthorized("No ha iniciado sesión"))
    }
  }

  def addGasto(idOrganizacion: Int) = Action.async { implicit request =>
    GastoForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Redirect(routes.Administracion.organizacion(idOrganizacion))),
      data => {
        val newGasto =
          Gasto(data.noGasto, data.concepto, data.importe, data.mes, data.dia, data.anio, data.comprobado, idOrganizacion)
        gastoDAO.add(newGasto).map(res =>
          Redirect(routes.Administracion.organizacion(idOrganizacion))
        )
      }
    )
  }

  def gastos(idOrg: Int, idGasto: Long) = Action.async { implicit request =>
    request.session.get("connected").map { user =>
      val qResult = Await.ready(gastoDAO.get(idGasto), Duration.Inf).value.get
      val gasto: Gasto = qResult match {
        case Success(option) => option match {
          case Some(g) => g
          case None => new Gasto(0L, "", BigDecimal(1), "", 0, 0, "", 0)
        }
        case Failure(ex) => new Gasto(0L, "", BigDecimal(1), "", 0, 0, "", 0)
      }
      request.session.get("connected").map { user =>
        fileUploadDAO.getFilesByNoGasto(idGasto).map(files =>
          Ok(views.html.administrador.gastos(idOrg, gasto, files, FileUploadForm.form))
        )
      }.getOrElse {
        Future.successful(Unauthorized("No ha iniciado sesión"))
      }
    }.getOrElse {
      Future.successful(Unauthorized("No ha iniciado sesión"))
    }
  }

  def upload(idOrg: Int, idGasto: Long) = Action.async(parse.multipartFormData) { implicit request =>
    request.body.file("PDF").map { pdf =>
      request.body.file("XML").map { xml =>
        val orga = getOrg(idOrg)
        import java.io.File
        val pdfPath = s"files/${orga.user}/$idGasto/${pdf.filename}"
        val xmlPath = s"files/${orga.user}/$idGasto/${xml.filename}"
        new File(pdfPath).mkdirs()
        new File(xmlPath).mkdirs()
        pdf.ref.moveTo(new File(pdfPath), replace = true)
        xml.ref.moveTo(new File(xmlPath), replace = true)
        FileUploadForm.form.bindFromRequest.fold(
          errors => Future.successful(Redirect(routes.Administracion.gastos(idOrg, idGasto))),
          data => {
            val now  = Calendar.getInstance()
            val month = new SimpleDateFormat("MMM").format(now.getTime)
            val day = now.get(Calendar.DAY_OF_MONTH)
            val year = now.get(Calendar.YEAR)
            val newFile = new FileUpload(0, pdfPath, xmlPath, data.importe, month, day, year, idGasto)
            fileUploadDAO.add(newFile).map {res =>
              Redirect(routes.Administracion.gastos(idOrg, idGasto))
            }
          }
        )
      }.getOrElse {
        Future.successful(Redirect(routes.Administracion.gastos(idOrg, idGasto)).flashing(
          "error" -> "Missing XML"))
      }
    }.getOrElse {
      Future.successful(Redirect(routes.Administracion.gastos(idOrg, idGasto)).flashing(
        "error" -> "Missing PDF"))
    }
  }

  def getOrg(id: Int): Organizacion = {
    val qResult = Await.ready(organizacionesDAO.get(id), Duration.Inf).value.get
    val orga: Organizacion = qResult match {
      case Success(option) => option match {
        case Some(o) => o
        case None => new Organizacion(0,"","")
      }
      case Failure(ex) => new Organizacion(0,"","")
    }
    orga
  }


  def getListOfSubDirectories(directoryName: String): List[String] =
    new File(directoryName).listFiles.filter(_.isDirectory).map(_.getName).toList


  def deleteOrg(id: Int) = Action.async { implicit request =>
    organizacionesDAO.delete(id).map(res =>
      Ok("Deleted")
    )
  }

  def deleteGasto(noGasto: Long) = Action.async { implicit request =>
    request.session.get("connected").map { user =>
      if (user.equals("administrador")) {
        gastoDAO.delete(noGasto).map(res =>
          Ok("Deleted")
        )
      } else {
        Future.successful(Unauthorized("No puede borrar un gasto"))
      }
    }.getOrElse {
      Future.successful(Unauthorized("No ha iniciado sesión"))
    }
  }

  def updateOrg(idOrg: Int) = Action.async { implicit request =>
    OrganizacionForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Redirect(routes.Administracion.dashboard())),
      data => {
        val newOrga = Organizacion(idOrg, data.user, data.password)
        organizacionesDAO.update(idOrg, newOrga).map(res =>
          Redirect(routes.Administracion.dashboard())
        )
      }
    )
  }

  def updateGasto(idOrganizacion: Int, noGasto: Long) = Action.async { implicit request =>
    GastoForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Redirect(routes.Administracion.organizacion(idOrganizacion))),
      data => {
        val newGasto =
          Gasto(data.noGasto, data.concepto, data.importe, data.mes, data.dia, data.anio, data.comprobado, idOrganizacion)
        gastoDAO.update(noGasto, newGasto).map(res =>
          Redirect(routes.Administracion.organizacion(idOrganizacion))
        )
      }
    )
  }

  def downloadOrg(idOrg: Int) = Action {
    val orga = getOrg(idOrg)
    createZip(s"files/${orga.user}", s"files/${orga.user}.zip")
    Ok.sendFile(new File(s"files/${orga.user}.zip"))
  }

  def downloadGasto(idOrg: Int, noGasto: Long) = play.mvc.Results.TODO


  def createZip(sourceFolder: String, outputFile: String) = {

    def addDir(zos: ZipOutputStream, srcFile: File): Unit = {
      val files = srcFile.listFiles()
      files.foreach(f => {
        if (f.isDirectory) addDir(zos, f)
        else {
          val buffer = new Array[Byte](1024)
          val fis = new FileInputStream(f)
          zos.putNextEntry(new ZipEntry(f.getName))
          var length = fis.read(buffer)
          while (length >= 0) {
            zos.write(buffer, 0, length)
            length = fis.read(buffer)
          }
          zos.closeEntry()
          fis.close()
        }
      })
    }

    val fos = new FileOutputStream(outputFile)
    val zos = new ZipOutputStream(fos)
    val srcFile = new File(sourceFolder)
    addDir(zos, srcFile)
    zos.close()

  }

}
