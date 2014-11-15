import stellar.{OutTransaction, Transaction, PaymentTransaction, API}

import scala.collection
import scala.collection.parallel.mutable

object Main {
  val account100 = "gwy1o8ZBuNxxz66ge6Ru4x1CzBc7RbMTWb"
  val accountFela = "gHbvXso6jQEz9WLYvvQXqmzMa2knynrU41"
  val address1 = "ghWhBkFnzWRwZxY5EMbyrswJdNk1rvfCJj"
  val address2 = ""
  val secret1 = io.Source.fromURL(getClass.getResource("/secret1")).getLines.mkString
  println(secret1)
  //val secret2 = readFileContent("secret2")
  val account = account100
  def main(args: Array[String]) = {
    val transactions = API.account_tx(account)
    println(transactions.length)
    for (t <- transactions) {
      t match {
        case payment : PaymentTransaction =>
          if (payment.account == account)
            println(s"$t ->")
          if (payment.destination == account)
            println(s"$t <-")
        case _ =>
          println(t)
      }
    }
  }

  var processedInTransactions = collection.mutable.Set[Transaction]()
  var processedOutTransactions = collection.mutable.Set[Transaction]()

  var inProcessOutTransactions = collection.mutable.Set[OutTransaction]()

  case class IncomingTxLog(txs: Set[Transaction])
  case class OutgoingTxLog(txs: Set[Transaction])


  def mainLoop(): Unit = {
    val txLog = getTransactionList()
    val (out, in) = splitOutIn(txLog)
    markCompletedOutTransactions(out)
    val unprocessedOut = findUnprocessedInTransactions(in)
    createOutTransactions(unprocessedOut)
    runOutTransactions()
  }

  def getTransactionList(): Set[Transaction] = {
    API.account_tx(account).toSet
  }

  def splitOutIn(txLog: Set[Transaction]): (OutgoingTxLog, IncomingTxLog) = {
    val (outgoing, incoming) = txLog.filter(_.isPayment).partition(_.account == account)
    (OutgoingTxLog(outgoing), IncomingTxLog(incoming))
  }

  def markCompletedOutTransactions(transactions: OutgoingTxLog): Unit = {
    // move transactions from inProcessOutTransactions to processedOutTransactions
    val filter: collection.mutable.Set[OutTransaction] = inProcessOutTransactions.filter(ot => transactions.txs.find(_.hash == ot.hash).isDefined)
    inProcessOutTransactions.retain(!filter.contains(_))
    processedOutTransactions ++= transactions.txs.filter(x => filter.find(_.hash == x.hash).isDefined)
  }


  def markCompletedOutTransactions2(transactions: OutgoingTxLog, inProcessOutTransactions: Set[OutTransaction], processedOutTransactions: Set[Transaction]):  (Set[OutTransaction], Set[Transaction])= {
    // move transactions from inProcessOutTransactions to processedOutTransactions
    val (tmpinprocess, tmpouttra) = (collection.mutable.Set[OutTransaction](inProcessOutTransactions.toArray : _*), collection.mutable.Set[Transaction](processedOutTransactions.toArray : _*))
    val filter: collection.mutable.Set[OutTransaction] = tmpinprocess.filter(ot => transactions.txs.find(_.hash == ot.hash).isDefined)
    tmpinprocess.retain(!filter.contains(_))
    tmpouttra ++= transactions.txs.filter(x => filter.find(_.hash == x.hash).isDefined)
    (tmpinprocess.toSet, tmpouttra.toSet)
  }

  def findUnprocessedInTransactions(transactions: IncomingTxLog): List[Transaction] = {
    // finds transactions not in processedInTransactions
    ???
  }

  def createOutTransactions(transactions: List[Transaction]): Unit = {
    // TODO: proper lottery, now I just return the same amount
    def getAmount(t: Transaction) : Int = t match {
      case p: PaymentTransaction => p.amount.toInt
      case _ => throw new Exception("Can only get amount of payment")
    }
    inProcessOutTransactions ++= transactions.map(t =>
      API.sign(account, t.account, "TODO", getAmount(t))
    )
  }

  def runOutTransactions(): Unit = {
    /// runs all transactions in inProcessOutTransactions again
    for (t <- inProcessOutTransactions) {
      t.submit()
    }
  }
}
