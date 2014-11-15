import org.scalatest._
import play.api.libs.json.{JsArray, JsValue, Json}
import stellar.{OutTransaction, Transaction}

/*import Main._

class SanityCheck extends FlatSpec with Matchers {

  private val txnlog: String = io.Source.fromInputStream(getClass.getResourceAsStream("/txns.log")).getLines().mkString("")

  private val jsv: JsValue = Json.parse(txnlog) \ "result"
  private val txns: List[Transaction] = Transaction.parseList((jsv \ "transactions").as[JsArray])

  "A SanityCheck" should "given an empty inProcessOutTransactions doesn't add any to the processedoutTransactions" in {
    val (out, in): (OutgoingTxLog, IncomingTxLog) = Main.splitOutIn(txns.toSet)


    val (newOutTransactions, processedTransactions): (Set[OutTransaction], Set[Transaction]) = immutableMarkCompletedOutTransactions(out, Set(), Set())

    processedTransactions.size should be(0)
  }

  it should "given one inProcessOutTransaction that is also present in outtransactions then it should be moved to processedTransactions" in {
    val set = txns.toSet
    val (out, in): (OutgoingTxLog, IncomingTxLog) = Main.splitOutIn(txns.toSet)
    val (newOutTransactions, processedTransactions): (Set[OutTransaction], Set[Transaction]) = immutableMarkCompletedOutTransactions(out, Set(OutTransaction("", "5C9ADB369590980BEEB59CAF6CCA564D573EB9D85E24A566107DF44C1F99BF9C")), Set())
    out.txs.size should be(1)
    processedTransactions.size should be(1)
  }

  it should "given one inProcessOutTransaction that is not present in outtransactions then it should not be moved to processedTransactions" in {
    val set = txns.toSet
    val (out, in): (OutgoingTxLog, IncomingTxLog) = Main.splitOutIn(txns.toSet)
    val (newOutTransactions, processedTransactions): (Set[OutTransaction], Set[Transaction]) = immutableMarkCompletedOutTransactions(out, Set(OutTransaction("", "5C9ADB369590980kjsdksdjksdjks")), Set())
    out.txs.size should be(1)
    processedTransactions.size should be(0)
  }

  it should "given empty processedInTransactions findUnprocessedInTransactions should return all incoming tansactions" in {
    val set = txns.toSet
    val (out, in): (OutgoingTxLog, IncomingTxLog) = Main.splitOutIn(txns.toSet)
    val unprocessedInTransactions = findUnprocessedInTransactions(in, Set())

    in.txs.size should be(3)
    unprocessedInTransactions.size should be(3)
  }

  it should "given non empty processedInTransactions findUnprocessedInTransactions should return all incoming tansactions that aren't in the processedInTransactions" in {
    val set = txns.toSet
    val (out, in): (OutgoingTxLog, IncomingTxLog) = Main.splitOutIn(txns.toSet)
    val unprocessedInTransactions = findUnprocessedInTransactions(in, Set(in.txs.head))

    in.txs.size should be(3)
    unprocessedInTransactions.size should be(2)
  }

}*/
