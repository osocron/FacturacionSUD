package controllers

import javax.inject.Inject

import model.{OrganizacionForm, OrganizacionesDAO}
import play.api.mvc._

import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(organizacionesDAO: OrganizacionesDAO) extends Controller {

  def login= Action {
    Ok(views.html.login(OrganizacionForm.form))
  }

  def authenticate() = Action.async { implicit request =>
    OrganizacionForm.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      data => organizacionesDAO.getAll.map(res =>
        if (res.exists(org => org.user.equals(data.user) && org.password.equals(data.password))) {
          data.user match {
            case "master" => Redirect(routes.Application.dashboard()).withSession("connected" -> "administrador")
          }
        }
        else Redirect(routes.Application.login())
      )
    )
  }

  def uploads = Action {
    Ok(views.html.barrio.uploads("primaria","Subir Facturas"))
  }

  def dashboard() = Action { request =>
    request.session.get("connected").map { user =>
      Ok(views.html.administrador.dashboard(user))
    }.getOrElse {
      Unauthorized("No ha iniciado sesión")
    }
  }

  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      import java.io.File
      val filename = picture.filename
      val contentType = picture.contentType
      picture.ref.moveTo(new File(s"files/$filename"))
      Ok("File uploaded")
    }.getOrElse {
      Redirect(routes.Application.uploads).flashing(
        "error" -> "Missing file")
    }
  }

}