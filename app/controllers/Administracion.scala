package controllers

import javax.inject.Inject

import model._
import play.api.mvc._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Administracion @Inject()(organizacionesDAO: OrganizacionesDAO, gastoDAO: GastoDAO) extends Controller {

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
      gastoDAO.getGastoByOrg(idOrganizacion).map(gastos =>
        Ok(views.html.administrador.organizacion(idOrganizacion, gastos, GastoForm.form))
      )
    }.getOrElse {
      Future.successful(Unauthorized("No ha iniciado sesión"))
    }
  }

  def addGasto(idOrganizacion: Int) = Action.async { implicit request =>
    GastoForm.form.bindFromRequest.fold(
      errorForm => Future.successful(Redirect(routes.Administracion.organizacion(idOrganizacion))),
      data => {
        val newGasto =
          Gasto(0, data.concepto, data.importe, data.mes, data.dia, data.anio, data.comprobado, idOrganizacion)
        gastoDAO.add(newGasto).map(res =>
          Redirect(routes.Administracion.organizacion(idOrganizacion))
        )
      }
    )
  }

}
