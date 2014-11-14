package stellar

import play.api.libs.json.{JsArray, Json, JsValue}

import scalaj.http.{HttpOptions, Http}

object API {
  val serverUrl = "https://live.stellar.org:9002"
  val connTimeout = 30000
  val readTimeout = 100000

  def account_tx(account: String, ledger_min: Int = -1, limit: Int = 5, offset: Int = 0): Unit = {

    // create request json object
    val data: JsValue = Json.obj(
      "method" -> "account_tx",
      "params" ->  Json.arr(
        Json.obj(
          "account" -> account,
          "ledger_min" -> -1,
          "forward" -> true,
          "limit" -> limit,
          "offset" -> offset
        )
      )
    )

    // actual api request
    val request = Http.postData(serverUrl, data.toString())
      .option(HttpOptions.connTimeout(connTimeout))
      .option(HttpOptions.readTimeout(readTimeout))

    if (request.responseCode != 200)
      throw new Exception(s"Server answered with ${request.responseCode}")

    // parse response
    val res = Json.parse(request.asString) \ "result"
    val transactions : JsArray = (res \ "transactions").as[JsArray]
    val tx_values : Seq[JsValue] = transactions.value

    for (json <- tx_values) {
      val transactionType = (json \ "tx" \ "TransactionType").as[String]
      if (transactionType == "Payment") {
        val tx = new PaymentDescription(json)
        if (tx.destination == account) {
          println(s"received ${tx.amount}")
        } else {
          if (tx.account != account)
            throw new Exception(s"Transaction does not include ${account}")
          println(s"send ${tx.amount}")
        }
      } else {
        println(s"Non payment transaction: $transactionType")
      }
    }
  }
}
