package org.lucky7.feelinglucky

import stellar._

import scala.collection
import scala.collection.parallel.mutable

object Main {
  val account100 = "gwy1o8ZBuNxxz66ge6Ru4x1CzBc7RbMTWb"
  val accountFela = "gHbvXso6jQEz9WLYvvQXqmzMa2knynrU41"
  val address1 = "ghWhBkFnzWRwZxY5EMbyrswJdNk1rvfCJj"
  // starting ledger index
  val index = 509049

  def main(args: Array[String]) = {
    //start(LotteryCredentials("gsMxVfhj1GmHspP5iARzMxZBZmPya9NALr", io.Source.fromURL(getClass.getResource("/secret1")).getLines.mkString))
    start(LotteryCredentials("g48BvpjobAFYVZurnbSFM3C5SGyTsjzTzt", "s3VPPYmPTTVe6Rb3GkZ6pSFqyuqkF2vMTAribpWzStb8BPMVLmv"))
  }

  @volatile private var stopped = false
  //todo we want to restart the lottery which isn't possible right now!
  //passing lottery all over the place isn't ideal!!!

  def stop(): Unit = {
    println(s"!!!!!!! STOP Lottery")
    this.stopped = true
  }

  def start(lottery: LotteryCredentials) = {
    println(s"!!!!!!! STARTING Lottery $lottery")
    init(lottery)
    while (!stopped) {
      mainLoop(lottery)
    }
    println(s"!!!!!!! STOPPED Lottery $lottery")
    //API.close(5000)
  }

  // IN transactions that we don't need to handle anymore
  var processedInTransactions = Set[String]()

  // OUT transaction that we DO need to resubmit
  var inProcessOutTransactions = Set[OutTransaction]()

  case class IncomingTxLog(txs: Set[Transaction])
  case class OutgoingTxLog(txs: Set[Transaction])


  def init(lottery: LotteryCredentials): Unit = {
    // set all IN transactions as processed
    val txLog = getTransactionList(lottery.id)
    val (_, in) = splitOutIn(lottery.id, txLog)
    processedInTransactions = in.txs.map(_.hash)
  }


  def mainLoop(lottery: LotteryCredentials): Unit = {
    val account: String = lottery.id
    val txLog = getTransactionList(account)
    val (out, in) = splitOutIn(account, txLog)
    markCompletedOutTransactions(out)
    runOutTransactions()
    val unprocessedOut = findUnprocessedInTransactions(in)
    createOutTransactions(lottery, unprocessedOut)
    println("\n\nwaiting 10 seconds")
    Thread sleep 10000
  }

  def getTransactionList(account: String): Set[Transaction] = {
    val res = API.account_tx(account, ledgerIndexMin=index).toSet
    println(s"got ${res.size} transactions from the log")
/*
    for (t <- res) {
      print(s"${t.hash}: ")
      printTransaction(t)
    }
*/
    res
  }

  def splitOutIn(account: String, txLog: Set[Transaction]): (OutgoingTxLog, IncomingTxLog) = {
    val (outgoing, incoming) = txLog.filter(_.isPayment).partition(_.account == account) //assumption: all others are incoming i.e. destination is our account
    println(s"got ${incoming.size} incoming transactions log")
    println(s"got ${outgoing.size} outgoing transactions log")

    (OutgoingTxLog(outgoing), IncomingTxLog(incoming.filter(_.transactionResult == "tesSUCCESS")))
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
/*
    for (t <- res) {
      print(s"${t.hash}: ")
      printTransaction(t)
    }
*/
    res
  }

  def createOutTransactions(lottery: LotteryCredentials, transactions: Set[Transaction]): Unit = {
    def getAmount(t: Transaction) : BigInt = Lottery.play(BigInt(t.amount))

    println(s"inProcessOutTransactions before: ${inProcessOutTransactions.size}")
    println(s"processedInTransactions before: ${processedInTransactions.size}")
    for (t <- transactions) {
      val out = API.sign(lottery.id, t.account, lottery.hash, getAmount(t))
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

  private def printTransaction(account: String, t: Transaction): Unit = {
        if (t.account == account)
          println(s"$t ->")
        else if (t.destination == account)
          println(s"$t <-")
        else
        println(t)

  }
}
