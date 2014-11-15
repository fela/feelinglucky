// Test account
// ghWhBkFnzWRwZxY5EMbyrswJdNk1rvfCJj

class TransactionStore {

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

  def mainLoop(): Unit = {
    val txLog = getTransactionList()
    val (in, out) = splitInOut(txLog)
    markCompletedOutTransactions(out)
    val unprocessedOut = findUnprocessedInTransactions(in)
    createOutTransactions(unprocessedOut)
    runOutTransactions()
  }

  def splitInOut(txLog: List[Transaction]): (List[Transaction], List[Transaction]) = {
    ??? 
  }

  def getTransactionList(): List[Transaction] = {
    API.account_tx(account)
  }
  def markCompletedOutTransactions(transactions: List[Transaction]): Unit = ???
  def findUnprocessedInTransactions(transactions: List[Transaction]): List[Transaction] = ???
  def createOutTransactions(transactions: List[Transaction]): Unit = ???
  def runOutTransactions(): Unit = ???
}
