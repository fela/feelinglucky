import stellar.API

import scalaj.http.{HttpOptions, Http}
import play.api.libs.json.{JsArray, JsValue, Json}

object Hi {

  def main(args: Array[String]) = {
    //API.account_tx("ganVp9o5emfzpwrG5QVUXqMv8AgLcdvySb")
    API.account_tx("gHbvXso6jQEz9WLYvvQXqmzMa2knynrU41")
  }
}
