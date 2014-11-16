package org.lucky7.feelinglucky

import org.purang.net.http._
import org.purang.net.http.ning._
import play.api.libs.json.JsValue
import scalaz._, Scalaz._
import argonaut._, Argonaut._

object Accounts {
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
      println(x)
      Parse.decodeEither[Balance](x)
    }
  }, 200)


  def main(args: Array[String]) {
    //println(create)
    println(balance("gJE2Yf8JLjLTnuwceWZA5B6Dw4UPzp1iRS"))
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



  """
    |{
    |  "result": {
    |    "account_data": {
    |      "Account": "gMnXp1Eb5tAoRM5qK9uX84Ew5MXf7dNokK",
    |      "Balance": "1000000000",
    |      "Flags": 0,
    |      "LedgerEntryType": "AccountRoot",
    |      "OwnerCount": 0,
    |      "PreviousTxnID": "75607A9C466F4DE07A56DA67F1F604014F719F3512EF8387306D62CC5D419B94",
    |      "PreviousTxnLgrSeq": 507371,
    |      "Sequence": 1,
    |      "index": "24E86EF0A2BE205B92E94251B341EBC17A3FE5364F260880D5EB410142BAB6F6"
    |    },
    |    "ledger_current_index": 507378,
    |    "status": "success"
    |  }
    |}
  """.stripMargin

}
