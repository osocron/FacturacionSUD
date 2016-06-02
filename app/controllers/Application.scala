package controllers

import play.api._
import play.api.mvc._

class Application extends Controller {

  def login = Action {
    Ok(views.html.login("The main login page"))
  }

}