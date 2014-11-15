// Test account
// ghWhBkFnzWRwZxY5EMbyrswJdNk1rvfCJj

class OutTransaction {

}

import stellar.{Transaction, PaymentTransaction, API}

object Main {
  val account100 = "gwy1o8ZBuNxxz66ge6Ru4x1CzBc7RbMTWb"
  val accountFela = "gHbvXso6jQEz9WLYvvQXqmzMa2knynrU41"
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

  var inProcessOutTransactions = Set[OutTransaction]()
  var processedOutTransactions = Set[String]()
  var processedInTransactions = Set[String]()

  case class IncomingTxLog(txs: List[Transaction])
  case class OutgoingTxLog(txs: List[Transaction])


  def mainLoop(): Unit = {
    val txLog = getTransactionList()
    val (out, in) = splitOutIn(txLog)
    markCompletedOutTransactions(out)
    val unprocessedOut = findUnprocessedInTransactions(in)
    createOutTransactions(unprocessedOut)
    runOutTransactions()
  }

  def getTransactionList(): List[Transaction] = {
    API.account_tx(account)
  }

  def splitOutIn(txLog: List[Transaction]): (OutgoingTxLog, IncomingTxLog) = {
    val (outgoing, incoming) = txLog.filter(_.isPayment).partition(_.account == account)
    (OutgoingTxLog(outgoing), IncomingTxLog(incoming))
  }

  def markCompletedOutTransactions(transactions: OutgoingTxLog): Unit = {
    // move transactions from inProcessOutTransactions to processedOutTransactions
    ???
  }

  def findUnprocessedInTransactions(transactions: IncomingTxLog): List[Transaction] = {
    // finds transactions not in processedInTransactions
    ???
  }

  def createOutTransactions(transactions: List[Transaction]): Unit = {
    // adds object to inProcessOutTransactions and removes to processedInTransactions
    ???
  }

  def runOutTransactions(): Unit = {
    /// runs all transactions in inProcessOutTransactions again
    ???
  }
}