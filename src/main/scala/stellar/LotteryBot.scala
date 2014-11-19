package stellar

case class LotteryBot(account: Account, secret: Secret, startingIndex: LedgerIndex = LedgerIndex(0)) {
  def run(args: Array[String]) = {
    init()
    while (true) {
      mainLoop()
    }

    API.close(5000)
  }

  // IN transactions that we don't need to handle anymore
  var processedInTransactions = Set[String]()

  // OUT transaction that we DO need to resubmit
  // once we have a confirmation that the happened correcly
  // they will be removed from here
  var inProcessOutTransactions = Set[OutTransaction]()


  def init(): Unit = {
    // set all IN transactions as processed
    val txLog = TransactionLog(account, startingIndex)
    processedInTransactions = txLog.incomingPayments.map(_.tx.hash)
  }


  def mainLoop(): Unit = {
    val txLog = TransactionLog(account, startingIndex)
    markCompletedOutTransactions(txLog.outgoingPayments)
    runOutTransactions()
    val unprocessedIn = findUnprocessedInTransactions(txLog.incomingPayments)
    createOutTransactions(unprocessedIn)
    println("\n\nwaiting 10 seconds")
    Thread sleep 10000
  }


  def markCompletedOutTransactions(transactions: Set[OutgoingPayment]): Unit = {
    // remove completed transactions from inProcessOutTransactions
    val hashes = transactions.map(_.tx.hash)
    println(s"inProcessOutTransactions before: ${inProcessOutTransactions.size}")
    //println(s"processedOutTransactions before: ${processedOutTransactions.size}")
    inProcessOutTransactions --= inProcessOutTransactions.filter( t => hashes.contains(t.hash))
    //processedOutTransactions ++= hashes
    println(s"inProcessOutTransactions after: ${inProcessOutTransactions.size}")
    //println(s"processedOutTransactions after: ${processedOutTransactions.size}")
  }

  def findUnprocessedInTransactions(transactions: Set[IncomingPayment]): Set[IncomingPayment] = {
    // finds transactions not in processedInTransactions
    val res: Set[IncomingPayment] = transactions.filter(t => !processedInTransactions(t.tx.hash))
    println("## Unprocessed: ##")
    /*
        for (t <- res) {
          print(s"${t.hash}: ")
          printTransaction(t)
        }
    */
    res
  }

  def createOutTransactions(transactions: Set[IncomingPayment]): Unit = {
    def getAmount(t: IncomingPayment) : BigInt = t.tx match {
      case p: PaymentTransaction => Lottery.play(BigInt(p.amount))
      case _ => throw new Exception("Can only get amount of payment")
    }

    println(s"inProcessOutTransactions before: ${inProcessOutTransactions.size}")
    println(s"processedInTransactions before: ${processedInTransactions.size}")
    for (t <- transactions) {
      val out = API.sign(account.id, t.tx.account, secret.str, getAmount(t))
      println(s"=====\nbefore ${inProcessOutTransactions.size}")
      inProcessOutTransactions += out
      println(s"after ${inProcessOutTransactions.size}")
      processedInTransactions += t.tx.hash
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
}
