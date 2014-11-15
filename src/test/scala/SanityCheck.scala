import org.scalatest._
import play.api.libs.json.{JsArray, JsValue, Json}
import stellar.{OutTransaction, Transaction}

import Main._

class SanityCheck extends FlatSpec with Matchers {

  private val txnlog: String = io.Source.fromInputStream(getClass.getResourceAsStream("/txns.log")).getLines().mkString("")

  private val jsv: JsValue = Json.parse(txnlog) \ "result"
  private val txns: List[Transaction] = Transaction.parseList((jsv \ "transactions").as[JsArray])

  println(txns.take(3))

  "A SanityCheck" should "given an empty inProcessOutExceptions doesn't add any to the processedoutTransactions" in {
    val set = txns.toSet
    println(set)
    val (out, in): (OutgoingTxLog, IncomingTxLog) = Main.splitOutIn(txns.toSet)
    val (newOutTransactions, processedTransactions): (Set[OutTransaction], Set[Transaction]) = markCompletedOutTransactions2(out, Set(), Set())

    processedTransactions.size should be(0)
  }

}
