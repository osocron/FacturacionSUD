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
        val gastosQuery = Await.ready(gastoDAO.getAll, Duration.Inf).value.get
        val gastos: Seq[Gasto] = gastosQuery match {
          case Success(seq) => seq
          case Failure(ex) => Seq()
        }
        if (!gastos.exists(_.noGasto == data.noGasto)) {
          val newGasto =
            Gasto(data.noGasto, data.concepto, data.importe, data.mes, data.dia, data.anio, data.comprobado, idOrganizacion)
          gastoDAO.add(newGasto).map(res =>
            Redirect(routes.Administracion.organizacion(idOrganizacion))
          )
        }
        else Future.successful(Redirect(routes.Administracion.organizacion(idOrganizacion)))
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
        val pdfFileName = pdf.filename
        val xmlFileName = xml.filename
        val pdfInputStream = new FileInputStream(pdf.ref.file)
        val xmlInputStream = new FileInputStream(xml.ref.file)
        val contentPDF = new Array[Byte](pdf.ref.file.length().asInstanceOf[Int])
        val contentXMl = new Array[Byte](xml.ref.file.length().asInstanceOf[Int])
        pdfInputStream.read(contentPDF)
        xmlInputStream.read(contentXMl)
        if (pdfInputStream != null && xmlInputStream != null) pdfInputStream.close(); xmlInputStream.close()
        FileUploadForm.form.bindFromRequest.fold(
          errors => Future.successful(Redirect(routes.Administracion.gastos(idOrg, idGasto))),
          data => {
            val now  = Calendar.getInstance()
            val month = new SimpleDateFormat("MMM").format(now.getTime)
            val day = now.get(Calendar.DAY_OF_MONTH)
            val year = now.get(Calendar.YEAR)
            val newFile = new FileUpload(0, pdfFileName, xmlFileName, contentPDF, contentXMl, data.importe, month, day, year, idGasto)
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
    val gastosQuery = Await.ready(gastoDAO.getGastoByOrg(idOrg), Duration.Inf).value.get
    val gastos: Seq[Gasto] = gastosQuery match {
      case Success(seq) => seq
      case Failure(ex) => Seq()
    }
    val fileQuery: Seq[Try[Seq[FileUpload]]] =
      for {gasto <- gastos} yield Await.ready(fileUploadDAO.getFilesByNoGasto(gasto.noGasto), Duration.Inf).value.get
    val files: Seq[FileUpload] = fileQuery.flatMap(ty => ty match {
      case Success(seq) => seq
      case Failure(exception) => Seq()
    })
    val orga = getOrg(idOrg)
    val zipFile = File.createTempFile(orga.user, ".zip")
    createZip(files, zipFile)
    Ok.sendFile(zipFile)
  }

  def downloadGasto(idOrg: Int, noGasto: Long) = Action {
    val fileQuery = Await.ready(fileUploadDAO.getFilesByNoGasto(noGasto), Duration.Inf).value.get
    val files = fileQuery match {
      case Success(seq) => seq
      case Failure(exception) => Seq()
    }
    val orga = getOrg(idOrg)
    val zipFile = File.createTempFile(orga.user+noGasto+"_", ".zip")
    createZip(files, zipFile)
    Ok.sendFile(zipFile)
  }

  def createZip(files: Seq[FileUpload], zipFile: File) = {
    val disFiles = distinctFiles(files)
    val fos = new FileOutputStream(zipFile)
    val zos = new ZipOutputStream(fos)
    for {f <- disFiles } {
      zos.putNextEntry(new ZipEntry(f.namePDF))
      zos.write(f.contentPDF, 0, f.contentPDF.length)
      zos.closeEntry()
      zos.putNextEntry(new ZipEntry(f.nameXML))
      zos.write(f.contentXML, 0, f.contentXML.length)
      zos.closeEntry()
    }
    zos.close()
    zipFile.deleteOnExit()
  }

  def distinctFiles(files: Seq[FileUpload]): Seq[FileUpload] = {
    val indexed = files.zipWithIndex
    val diff = indexed.map(t => (FileUpload(t._1.idFile, t._2+t._1.namePDF,
      t._2+t._1.nameXML, t._1.contentPDF, t._1.contentXML,
      t._1.importe, t._1.mesUpload, t._1.diaUpload, t._1.anioUpload, t._1.noGasto), t._2))
    diff.unzip._1
  }

  def delUpload(idOrg: Int, noGasto: Long, idUpload: Int) = Action.async { implicit request =>
    request.session.get("connected").map { user =>
      if (user.equals("administrador")) {
        fileUploadDAO.delete(idUpload).map(res =>
          Ok("Deleted")
        )
      } else {
        Future.successful(Unauthorized("No puede borrar los archivos"))
      }
    }.getOrElse {
      Future.successful(Unauthorized("No ha iniciado sesión"))
    }
  }


  def updateFile(idOrg: Int, idGasto: Long, idUpload: Int) = Action.async {implicit request =>
    FileUploadForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Redirect(routes.Administracion.gastos(idOrg, idGasto))),
      data => {
        fileUploadDAO.updateImporte(idUpload, data.importe).map(res =>
          Redirect(routes.Administracion.gastos(idOrg, idGasto))
        )
      }
    )
  }

  def downloadFile(idOrg: Int, idGasto: Long, idUpload: Int) = Action {
    val fileQuery = Await.ready(fileUploadDAO.get(idUpload), Duration.Inf).value.get
    val file: FileUpload = fileQuery match {
      case Success(f) => f match {
        case Some(mFile) => mFile
        case None => new FileUpload(0,"","",Array[Byte](),Array[Byte](),BigDecimal(0),"",0,0,0)
      }
      case Failure(exception) => new FileUpload(0,"","",Array[Byte](),Array[Byte](),BigDecimal(0),"",0,0,0)
    }
    val orga = getOrg(idOrg)
    val zipFile = File.createTempFile(file.namePDF+"_", ".zip")
    createZip(Seq(file), zipFile)
    Ok.sendFile(zipFile)
  }


}
