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
  // starting ledger index
  val index = 506133

  def main(args: Array[String]) = {
    while (true) {
      mainLoop()
    }
  }

  // IN transactions that we don't need to handle anymore
  var processedInTransactions = Set[String]()

  // OUT transactions that we don't need to resubmit (hashes)
  //var processedOutTransactions = Set[String]()

  // OUT transaction that we DO need to resubmit
  var inProcessOutTransactions = Set[OutTransaction]()

  case class IncomingTxLog(txs: Set[Transaction])
  case class OutgoingTxLog(txs: Set[Transaction])


  def mainLoop(): Unit = {
    println("\n\nmainLoop()")
    val txLog = getTransactionList()
    val (out, in) = splitOutIn(txLog)
    markCompletedOutTransactions(out)
    runOutTransactions()
    val unprocessedOut = findUnprocessedInTransactions(in)
    createOutTransactions(unprocessedOut)
  }

  def getTransactionList(): Set[Transaction] = {
    val res = API.account_tx(account, ledgerIndexMin=index).toSet
    println(s"got ${res.size} transactions from the log")
    for (t <- res) {
      print(s"${t.hash}: ")
      printTransaction(t)
    }
    res
  }

  def splitOutIn(txLog: Set[Transaction]): (OutgoingTxLog, IncomingTxLog) = {
    val (outgoing, incoming) = txLog.filter(_.isPayment).partition(_.account == account) //assumption: all others are incoming i.e. destination is our account
    println(s"got ${incoming.size} incoming transactions log")
    println(s"got ${outgoing.size} outgoing transactions log")
    (OutgoingTxLog(outgoing), IncomingTxLog(incoming))
  }

  def markCompletedOutTransactions(transactions: OutgoingTxLog): Unit = {
    // remove completed transactions from inProcessOutTransactions
    val hashes = transactions.txs.map(_.hash)
    println(s"inProcessOutTransactions before: ${inProcessOutTransactions.size}")
    //println(s"processedOutTransactions before: ${processedOutTransactions.size}")
    inProcessOutTransactions --= inProcessOutTransactions.filter( t => hashes.contains(t.hash))
    //processedOutTransactions ++= hashes
    println(s"inProcessOutTransactions after: ${inProcessOutTransactions.size}")
    //println(s"processedOutTransactions after: ${processedOutTransactions.size}")
  }

  def findUnprocessedInTransactions(transactions: IncomingTxLog): Set[Transaction] = {
    // finds transactions not in processedInTransactions
    val res: Set[Transaction] = transactions.txs.filter(t => !processedInTransactions(t.hash))
    println("## Unprocessed: ##")
    for (t <- res) {
      print(s"${t.hash}: ")
      printTransaction(t)
    }
    res
  }

  def createOutTransactions(transactions: Set[Transaction]): Unit = {
    // TODO: proper lottery, now I just return the same amount
    def getAmount(t: Transaction) : Int = t match {
      case p: PaymentTransaction => p.amount.toInt / 10
      case _ => throw new Exception("Can only get amount of payment")
    }

    println(s"inProcessOutTransactions before: ${inProcessOutTransactions.size}")
    println(s"processedInTransactions before: ${processedInTransactions.size}")
    for (t <- transactions) {
      val out = API.sign(account, t.account, secret1, getAmount(t))
      println(s"=====\nbefore ${inProcessOutTransactions.size}")
      inProcessOutTransactions += out
      println(s"after ${inProcessOutTransactions.size}")
      processedInTransactions += t.hash
      out.submit() // try to submit
    }
    println(s"inProcessOutTransactions after: ${inProcessOutTransactions.size}")
    println(s"processedInTransactions after: ${processedInTransactions.size}")
  }

  def runOutTransactions(): Unit = {
    println(s"! Running ${inProcessOutTransactions.size} transactions again")
    // runs all transactions in inProcessOutTransactions again
    for (t <- inProcessOutTransactions) {
      t.submit()
    }
  }

  private def printTransaction(t: Transaction): Unit = {
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
