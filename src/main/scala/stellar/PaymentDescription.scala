package stellar

import play.api.libs.json.JsValue

class PaymentDescription(json: JsValue) {
  val rawJson = json
  val validated = (json \ "validated").as[Boolean]
  val result = (json \ "meta" \ "TransactionResult").as[String]
  val amountDrops = BigInt( (json \ "tx" \ "Amount").as[String])
  val account = (json \ "tx" \ "Account").as[String]
  val destination = (json \ "tx" \ "Destination").as[String]
  val hash = (json \ "tx" \ "hash").as[String]

  def amount = amountDrops.toFloat / 1000000.0

  override def toString: String = {
    amount.toString
  }
}
