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
  val account = "gsMxVfhj1GmHspP5iARzMxZBZmPya9NALr"
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

  var processedInTransactions = Set[Transaction]()
  var processedOutTransactions = Set[Transaction]()

  var inProcessOutTransactions = Set[OutTransaction]()

  case class IncomingTxLog(txs: Set[Transaction])
  case class OutgoingTxLog(txs: Set[Transaction])


  def mainLoop(): Unit = {
    val txLog = getTransactionList()
    val (out, in) = splitOutIn(txLog)
    markCompletedOutTransactions(out)
    val unprocessedOut = findUnprocessedInTransactions(in, processedInTransactions)
    createOutTransactions(unprocessedOut)
    runOutTransactions()
  }

  def getTransactionList(): Set[Transaction] = {
    API.account_tx(account).toSet
  }

  def splitOutIn(txLog: Set[Transaction]): (OutgoingTxLog, IncomingTxLog) = {
    val (outgoing, incoming) = txLog.filter(_.isPayment).partition(_.account == account) //assumption: all others are incoming i.e. destination is our account
    (OutgoingTxLog(outgoing), IncomingTxLog(incoming))
  }

  def markCompletedOutTransactions(transactions: OutgoingTxLog): Unit = {
    // move transactions from inProcessOutTransactions to processedOutTransactions
    val (newInProcessOutTransactions, newProcessedOutTransactions): (Set[OutTransaction], Set[Transaction]) = immutableMarkCompletedOutTransactions(transactions, inProcessOutTransactions, processedOutTransactions)
    inProcessOutTransactions = newInProcessOutTransactions
    processedOutTransactions = newProcessedOutTransactions
  }


  def immutableMarkCompletedOutTransactions(transactions: OutgoingTxLog, inProcessOutTransactions: Set[OutTransaction], processedOutTransactions: Set[Transaction]):  (Set[OutTransaction], Set[Transaction])= {
    // move transactions from inProcessOutTransactions to processedOutTransactions
    val (tmpinprocess, tmpouttra) = (collection.mutable.Set[OutTransaction](inProcessOutTransactions.toArray : _*), collection.mutable.Set[Transaction](processedOutTransactions.toArray : _*))
    val filter: collection.mutable.Set[OutTransaction] = tmpinprocess.filter(ot => transactions.txs.find(_.hash == ot.hash).isDefined)
    tmpinprocess.retain(!filter.contains(_))
    tmpouttra ++= transactions.txs.filter(x => filter.find(_.hash == x.hash).isDefined)
    (tmpinprocess.toSet, tmpouttra.toSet)
  }

  def findUnprocessedInTransactions(transactions: IncomingTxLog, processedInTransactions: Set[Transaction]): Set[Transaction] = {
    // finds transactions not in processedInTransactions
    val diff: Set[Transaction] = transactions.txs.diff(processedInTransactions)
    diff
  }

  def createOutTransactions(transactions: Set[Transaction]): Unit = {
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
