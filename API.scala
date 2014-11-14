package stellar

import play.api.libs.json.{JsArray, Json, JsValue}

import scalaj.http.{HttpOptions, Http}

object API {
  val serverUrl = "https://live.stellar.org:9002"
  val connTimeout = 30000
  val readTimeout = 100000

  def account_tx(account: String, ledgerIndexMin: Int = 0, perPage: Int = 1000): List[Transaction] = {


    // I use pagination terminology
    def getPage(marker: Option[JsValue]) : (List[Transaction], Option[JsValue]) = {

      val markerJson = marker match {
        case Some(m) =>
          Json.obj("marker" -> m)
        case None =>
          Json.obj()
      }
      val data: JsValue = Json.obj(
        "method" -> "account_tx",
        "params" ->  Json.arr(
          Json.obj(
            "account" -> account,
            "ledger_index_min" -> ledgerIndexMin,
            "forward" -> true,
            "limit" -> perPage
          ) ++ markerJson
        )
      )

      // actual api request
      val request = Http.postData(serverUrl, data.toString())
        .option(HttpOptions.connTimeout(connTimeout))
        .option(HttpOptions.readTimeout(readTimeout))

      if (request.responseCode != 200)
        throw new Exception(s"Server answered with ${request.responseCode}")
      val res = Json.parse(request.asString) \ "result"
      require((res \ "status").as[String] == "success")

      val transactions = Transaction.parseList((res \ "transactions").as[JsArray])
      //println(Json.prettyPrint(res))
      val newMarker = (res \ "marker").asOpt[JsValue]
      (transactions, newMarker)
    }

    // gets all pages after the marker
    def allPages(marker: Option[JsValue]=None) : List[Transaction] = {
        val (newList, newMarker) = getPage(marker)
        println(newMarker)
        newMarker match {
          case Some(m) =>
            if (! newList.isEmpty)
              newList ++ allPages(newMarker)
            else
              newList
          case None => newList
        }
    }

    return allPages()
  }
}

