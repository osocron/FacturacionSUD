package controllers

import javax.inject.Inject

import model.GastoDAO
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
  * Created by osocron on 6/28/16.
  */
class Barrio @Inject()(gastoDAO: GastoDAO) extends Controller {

  def organizacion(idOrganizacion: Int) = Action.async { request =>
    request.session.get("connected").map { user =>
      gastoDAO.getGastoByOrg(idOrganizacion).map(gastos =>
        Ok(views.html.barrio.barrio(idOrganizacion, gastos))
      )
    }.getOrElse {
      Future.successful(Unauthorized("No ha iniciado sesión"))
    }
  }

}
