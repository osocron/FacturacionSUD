package controllers

import javax.inject.Inject

import model.{Organizacion, OrganizacionForm, OrganizacionesDAO}
import play.api.mvc._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.{Await, Future}
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
            case "Obispado" => Redirect(routes.Administracion.dashboard()).withSession("connected" -> "administrador")
            case _  => Redirect(routes.Barrio.organizacion(
              res.find(org =>
                org.user.equals(data.user) && org.password.equals(data.password)).get.id)
            ).withSession("connected" -> "orga")
          }
        }
        else Redirect(routes.Application.login())
      )
    )
  }


}