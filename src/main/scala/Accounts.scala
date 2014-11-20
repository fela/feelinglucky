package org.lucky7.feelinglucky

import org.purang.net.http._
import org.purang.net.http.ning._
import play.api.libs.json.{Json, JsValue}
import stellar.{Transaction, API}
import scala.collection.immutable.IndexedSeq
import scala.collection.mutable.ArrayBuffer
import scalaz._, Scalaz._
import argonaut._, Argonaut._

case class LotteryCredentials(id: String, hash: String)

object Accounts {
  /*implicit class RBoolean(val bool: Boolean) extends AnyVal {
    def fold[A](t: =>A,f: =>A) = if (bool) t else f
  }*/

  implicit val sse = java.util.concurrent.Executors.newScheduledThreadPool(5)

  case class Account(result: AR)
  case class AR(accountId: String, masterSeed: String, masterSeedHex: String, publicKey: String, publicKeyHex: String, status: String)

  case class Balance(result: BR)
  case class BR(ad: AccountData, status: String )
  case class AccountData(balance: Long)


  implicit def ARDecodeJson: DecodeJson[AR] =
    jdecode6L(AR.apply)("account_id", "master_seed", "master_seed_hex", "public_key", "public_key_hex", "status")

  implicit def AccountDecodeJson: DecodeJson[Account] =
    jdecode1L(Account.apply)("result")


  implicit def BalanceDecodeJson: DecodeJson[Balance] =
    jdecode1L(Balance.apply)("result")
  implicit def BRDecodeJson: DecodeJson[BR] =
    jdecode2L(BR.apply)("account_data", "status")
  implicit def AccountDataDecodeJson: DecodeJson[AccountData] =
    jdecode1L(AccountData.apply)("Balance")


  private val stellar = "https://test.stellar.org:9002/"

  private val createAccount: String = """{"method":"create_keys"}"""

  private def friendbot(id: String) = s"""https://api-stg.stellar.org/friendbot?addr=$id"""

  private def accountCheckEntity(id: String) = s"""{"method":"account_info","params":[{"account":"$id"}]}"""

  private def accountCheck(id: String) = accountCheckEntity(id)

  private def close(): Unit = {
    pool.shutdownNow()
    sse.shutdownNow()
    nonblockingexecutor.client.close()
  }

  //the following is for lottery as lottery has to take stuff from everyone
  def getTransactions(myAccountId: String): List[Transaction] = API.account_tx(myAccountId, 0, 10000).filter(_.isPayment)

  //the following is for player as player is only interested in the stuff involving the lottery
  def getTransactionsForPlayer(myAccountId: String, targetSystemId: String): List[Transaction] =
    getTransactions(myAccountId).filter(x => x.destination == targetSystemId || x.account == targetSystemId)

  case class IncomingTxLog(txs: List[Transaction])
  case class OutgoingTxLog(txs: List[Transaction])
  

  //splits the account tx log into date-sorted outgoing and incoming transactions that are valid
  def split(myaccount: String, txLog: List[Transaction], filter: Transaction => Boolean = _.valid ): (OutgoingTxLog, IncomingTxLog) = {
    val (outgoing, incoming) = txLog.filter(_.isPayment).partition(_.account == myaccount)
    (OutgoingTxLog(outgoing.filter(filter).sortWith(_.date >_.date)), IncomingTxLog(incoming.filter(filter).sortWith(_.date >_.date)))
  }

  //finds the related pairs of transactions where there might be some kind of two way transfer, all others are returned as such
  //(List((out,in)), List(out)) //for players
  //assumes: logs only has lottery related stuff. See getTransactionsForPlayer
  def pairOut(out: OutgoingTxLog,in: IncomingTxLog): (List[(Transaction, Transaction)], List[Transaction]) = {
    //don't even think about runtime-complexity here ;)
    var buff : ArrayBuffer[(Transaction, Transaction)] = ArrayBuffer()
    var left : ArrayBuffer[Transaction] = ArrayBuffer()
    out.txs.foreach(
      o => in.txs.find(i => i.tag == o.tag).fold({left += o; ()})(i => buff += Tuple2(o, i)) //pair if tags are equal
    )
   /* println(
      s"""
        |pairs: ${buff.toList.mkString(",")}
        |
        |others: ${left.toList.mkString(",")}
        |
        |out: ${out.txs.mkString(",")}
        |
        |in: ${in.txs.mkString(",")}
      """.stripMargin)*/
    (buff.toList, left.toList)
  }

  //(List((out,in)), List(in)) // for lottery
  def pairIn(out: OutgoingTxLog,in: IncomingTxLog): (List[(Transaction, Transaction)], List[Transaction]) = {
    //don't even think about runtime-complexity here ;)
    var buff : ArrayBuffer[(Transaction, Transaction)] = ArrayBuffer()
    var left : ArrayBuffer[Transaction] = ArrayBuffer()
    in.txs.foreach(
      i => out.txs.find(o => (o.destination+o.tag) == (i.account +i.tag)).fold({left += i; ()})(o => buff += Tuple2(o, i)) //pair if tag and
    )
    (buff.toList, left.toList)
  }

  def create: \/[String, Account] = {
    val account: \/[String, Account] = post(stellar, createAccount, {
      x => {
        //println(x)
        Parse.decodeEither[Account](x)
      }
    }, 200)
    val success: \/[String, String] = account.map(a => get(friendbot(a.result.accountId), (x => a.result.accountId), 200))
    account
  }
  
  def balance(id: String): \/[String, Balance] = post(stellar, accountCheck(id), {
    x => {
      Parse.decodeEither[Balance](x)
    }
  }, 200)

  def paymentData(sender: String,
                  secret: String,
                  receiver: String,
                  amount: String,
                  dt: Int): String = {
    val data: JsValue = Json.obj(
      "method" -> "submit",
      "params" ->  Json.arr(
        Json.obj(
          "secret" -> secret,
          "tx_json" -> Json.obj(
            "TransactionType" -> "Payment",
            "Account" -> sender,
            "Destination" -> receiver,
            "DestinationTag" -> dt,
            "Amount" -> amount
          )
        )
      )
    )
    data.toString
  }

  def makePayment(sender: String, secret: String,  receiver: String,  amount: String,
                  dt: Int): \/[String, String] = {
    post(stellar, paymentData(sender, secret, receiver, amount, dt), {
      body => {
        //println(body)
        val res = Json.parse(body) \ "result"
        ((res \ "status").as[String] == "success").fold (
          \/-("success"),
          -\/((res \ "error_message").as[String]))
        }
      })
    }

  def main(args: Array[String]) {
    val primary = create
    println(primary)

    @volatile var dt = 10000
    def next()  = {
      dt =  dt + 1
      dt
    }
    primary.map(
      p => makePayments(p)(20)
    )

    def makePayments(p: Account)(n: Int) = {
      val accounts: IndexedSeq[\/[String, Account]] = for (i <- 1 to n) yield create
      for {
        account <- accounts
        acc <- account
        r = acc.result
      } {
        Thread.sleep(3000)
        println(makePayment(r.accountId, r.masterSeed, p.result.accountId, "500" + "000000", next()))
      }
    }

    //println(balance("g48BvpjobAFYVZurnbSFM3C5SGyTsjzTzt"))
    primary.map(p => println(balance(p.result.accountId)))
    close()
  }

  private def get[T](url : => String,
             deserialized: (String) => T,
             status: Int = 200): T = {
    (GET > url).~>((x: ExecutedRequest) => x.fold(
      t => throw t._1,
      {
        case (`status`, _, Some(body), _) => deserialized(body)
        case e => throw new RuntimeException(e.toString)
      }), 10000)
  }

  private def post[T](url : => String,
              serialized: => String,
              deserialized: (String) => T,
              status: Int = 200): T = {
    (POST > url >>> serialized).~>((x: ExecutedRequest) => x.fold(
      t => throw t._1,
      {
        case (`status`, _, Some(body), _) => deserialized(body)
        case e => throw new RuntimeException(e.toString)
      }), 10000)
  }
}
