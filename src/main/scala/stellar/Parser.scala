package stellar

import play.api.libs.json.{JsArray, JsString, JsValue, Json}

object Transaction {
  def parseList(json: JsValue) : List[Transaction] = {
    val list = json.as[JsArray].value.toList
    list.map(parse _).toList
  }

  def parse(json: JsValue) : Transaction = {
    // println(Json.prettyPrint(json))
    if ((json \ "tx" \ "TransactionType").as[String] == "Payment")
      new PaymentTransaction(json)
    else
      new Transaction(json)
  }
}


class Transaction(json: JsValue) {
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
  // hash
  val hash = (tx \ "hash").as[String]
  // inLedger
  // ledger_index

  // ## validated ##
  val validated = (json \ "validated").as[Boolean]


  override def toString: String = {
    s"$transactionType $transactionResult"
  }
}

class PaymentTransaction(val json: JsValue) extends Transaction(json) {
  require(transactionType == "Payment")

  val destination = (json \ "tx" \ "Destination").as[String]

  val jsonAmount = json \ "tx" \ "Amount"
  val (amount, currency) = jsonAmount match {
    case JsString(value) => (value, "STR")
    case _ => ( (jsonAmount \ "value").as[String], (jsonAmount \ "currency"))
  }

  def valid = transactionResult == "tesSUCCESS" && validated

  override def toString: String = {
    val prettyAmount = if (currency == "STR") amount.toFloat / 1000000.0 else amount.toFloat
    var str =  s"$prettyAmount $currency"
    if (transactionResult != "tesSUCCESS")
      str += s" ${transactionResult}"
    if (!validated)
      str += " not validated!"
    str
  }
}
