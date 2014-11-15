package stellar

import org.purang.net.http._
import org.purang.net.http.ning._
import play.api.libs.json.{JsArray, Json, JsValue}

import scala.util.Random
import scala.concurrent._
import scalaj.http.{HttpOptions, Http}

case class OutTransaction(blob: String, hash: String) {
  def submit(): Unit = {
    println(s"! Submit for ${hash} ")
    API.submit(blob)
  }
}

object API {
  implicit val sse = java.util.concurrent.Executors.newScheduledThreadPool(5)

  import concurrent.duration._
  import FiniteDuration._
  import ExecutionContext.Implicits.global

  def eventually[A](i: Long)(a: => A) = {
    Await.ready(Future {
      blocking(Thread.sleep(i)); a
    }, i + 100 milliseconds)
  }

  def close(to: Long): Unit = {
      pool.shutdownNow()
      sse.shutdownNow()
      nonblockingexecutor.client.close()
  }

  val serverUrl = "https://test.stellar.org:9002"
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
      post(data, body => {
        val res = Json.parse(body) \ "result"
        require((res \ "status").as[String] == "success", (res \ "error_message").as[String])

        val transactions = Transaction.parseList((res \ "transactions").as[JsArray])
        //println(Json.prettyPrint(res))
        val newMarker = (res \ "marker").asOpt[JsValue]
        (transactions, newMarker)
      })
    }

    // gets all pages after the marker
    def allPages(marker: Option[JsValue]=None) : List[Transaction] = {
        val (newList, newMarker) = getPage(marker)
        //println(newMarker)
        newMarker match {
          case Some(m) =>
            if (! newList.isEmpty)
              newList ++ allPages(newMarker)
            else
              newList
          case None => newList
        }
    }

    allPages()
  }


  def sign(account: String, destination: String, secret: String, amount: BigInt): OutTransaction = {
    val data: JsValue = Json.obj(
      "method" -> "sign",
      "params" ->  Json.arr(
        Json.obj(
          "secret" -> secret,
          "tx_json" ->  Json.obj(
            "TransactionType" -> "Payment",
            "Account" -> account,
            "Destination" -> destination,
            "Amount" -> amount.toString,
            "DestinationTag" -> Random.nextInt(100000000).toString
          )
        )
      )
    )

    post(data, body => {
      val res = Json.parse(body) \ "result"
      val status = (res \ "status").as[String]
      require(status == "success", (res \ "error_message").as[String])
      //println(Json.prettyPrint(res))
      val blob = (res \ "tx_blob").as[String]
      val hash = (res \ "tx_json" \ "hash").as[String]
      OutTransaction(blob, hash)
    }
    )
  }

  def makePayment(secret: String, receiver: String, sender: String, amount: String): Unit = {
    val data: JsValue = Json.obj(
      "method" -> "submit",
      "params" ->  Json.arr(
        Json.obj(
          "secret" -> secret,
          "tx_json" -> Json.obj(
            "TransactionType" -> "Payment",
            "Account" -> sender,
            "Destination" -> receiver,
            "Amount" -> amount
          )
        )
      )
    )
    post(data, body => {
      val res = Json.parse(body) \ "result"
      require((res \ "status").as[String] == "success", (res \ "error_message").as[String])
    })

  }

  def submit(blob: String): Unit = {
    val data: JsValue = Json.obj(
      "method" -> "submit",
      "params" ->  Json.arr(
        Json.obj(
          "tx_blob" -> blob
        )
      )
    )
    post(data, body => {
      val res = Json.parse(body) \ "result"

      require((res \ "status").as[String] == "success", (res \ "error_message").as[String])
      println(Json.prettyPrint(res))
    })

  }

  def post[T](data: JsValue, f: (String) => T, status: Int = 200): T = {
    /*(POST > serverUrl >>> data.toString).~>((x: ExecutedRequest) => x.fold(
      t => throw t._1,
      {
        case (`status`, _, Some(body), _) => f(body)
        case e => throw new RuntimeException(e.toString)

      }), readTimeout)*/
      val request = Http.postData(serverUrl, data.toString())
              .option(HttpOptions.connTimeout(connTimeout))
              .option(HttpOptions.readTimeout(readTimeout))

      if (request.responseCode != 200)
          throw new Exception(s"Server answered with ${request.responseCode}")

      f(request.asString)
  }

}

