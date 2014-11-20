package stellar

import play.api.libs.json.{JsArray, JsString, JsValue, Json}

//the entire class hierarchy is horrible

object Transaction {
  def parseList(json: JsValue): List[Transaction] = {
    val list = json.as[JsArray].value.toList
    list.map(parse _).toList
  }

  def parse(json: JsValue): Transaction = {
    // println(Json.prettyPrint(json))
    if ((json \ "tx" \ "TransactionType").as[String] == "Payment")
      new PaymentTransaction(json)
    else
      new NonPaymentTransaction(json)
  }
}


trait Transaction {

  def rawJson:  JsValue

  def isPayment: Boolean


  // ## meta ##
  def meta: JsValue

  // Affected Nodes
  // Transaction Index
  // Transaction Result
  def transactionResult: String

  // ## tx ##
  def tx: JsValue

  // Account
  def account: String

  // Fee
  def fee: BigInt

  // Flags
  // Sequence
  // SigningPubKey
  // TransactionType
  def transactionType: String

  // TxnSignature
  // date
  def date: Int

  // hash
  def hash: String

  // inLedger
  // ledger_index

  // ## validated ##
  def validated: Boolean


  def destination: String

  def tag: Option[String]

  def jsonAmount: JsValue

  def amount: String

  def currency: String


  def valid = transactionResult == "tesSUCCESS" && validated

  override def toString: String = {
    val prettyAmount = if (currency == "STR") amount.toFloat / 1000000.0 else amount.toFloat
    var str = s"$prettyAmount $currency"
    if (transactionResult != "tesSUCCESS")
      str += s" ${transactionResult}"
    if (!validated)
      str += " not validated!"
    str
  }
}

case class PaymentTransaction(val json: JsValue) extends Transaction{


  def isPayment = transactionType == "Payment"

  val rawJson = json


  // ## meta ##
  val meta = json \ "meta"
  // Affected Nodes
  // Transaction Index
  // Transaction Result
  val transactionResult = (meta \ "TransactionResult").as[String]

  // ## tx ##
  val tx = json \ "tx"
  // Account
  val account = (tx \ "Account").as[String]
  // Fee
  val fee = BigInt((tx \ "Fee").as[String])
  // Flags
  // Sequence
  // SigningPubKey
  // TransactionType
  val transactionType = (tx \ "TransactionType").as[String]
  // TxnSignature
  // date
  val date = (tx \ "date").as[Int]

  // hash
  val hash = (tx \ "hash").as[String]
  // inLedger
  // ledger_index

  // ## validated ##
  val validated = (json \ "validated").as[Boolean]


  val destination = (tx \ "Destination").as[String]

  val tag = (tx \ "DestinationTag").as[Option[String]]

  val jsonAmount = tx \ "Amount"
  val (amount, currency) = jsonAmount match {
    case JsString(value) => (value, "STR")
    case _ => ((jsonAmount \ "value").as[String], (jsonAmount \ "currency").as[String])
  }


  override def toString: String = {
    val prettyAmount = if (currency == "STR") amount.toFloat / 1000000.0 else amount.toFloat
    var str = s"$prettyAmount $currency"
    if (transactionResult != "tesSUCCESS")
      str += s" ${transactionResult}"
    if (!validated)
      str += " not validated!"
    str
  }
}


//TODo replace the following : Stringabomination because of JSON modeling go argonaut

class NonPaymentTransaction(json: JsValue) extends Transaction {
  def rawJson = ???
  def isPayment: Boolean = false


  // ## meta ##
  def meta: JsValue = ???

  // Affected Nodes
  // Transaction Index
  // Transaction Result
  def transactionResult: String = ???

  // ## tx ##
  def tx: JsValue = ???

  // Account
  def account: String = ???

  // Fee
  def fee = ???

  // Flags
  // Sequence
  // SigningPubKey
  // TransactionType
  def transactionType: String = ???

  // TxnSignature
  // date
  def date: Int = ???

  // hash
  def hash: String = ???

  // inLedger
  // ledger_index

  // ## validated ##
  def validated: Boolean = ???


  def destination: String = ???

  def tag  = ???

  def jsonAmount: JsValue = ???

  def amount: String = ???

  def currency: String = ???

}


