import org.scalatest._
import stellar.{Transaction, LotteryCredentials}

class FindIndexTest extends FlatSpec {
  behavior of "A findIndex"

  it should "return the last value if there are is only one" in {
    val list = List((7.0, 0.1))
    assert(LotteryCredentials.findIndex(2.0, list) === 7.0)
  }

  it should "return the correct value" in {
    val list = List((7.0, 0.1), (8.0, 0.4), (1.0, 0.5))
    assert(LotteryCredentials.findIndex(0.05, list) === 7.0)
    assert(LotteryCredentials.findIndex(0.15, list) === 8.0)
    assert(LotteryCredentials.findIndex(0.45, list) === 8.0)
    assert(LotteryCredentials.findIndex(0.55, list) === 1.0)
    assert(LotteryCredentials.findIndex(0.95, list) === 1.0)
    assert(LotteryCredentials.findIndex(1.05, list) === 1.0)
  }

}
class ParserTest extends FlatSpec {
  import play.api.libs.json.{JsArray, JsValue, Json}
  private val txnlog: String = io.Source.fromInputStream(getClass.getResourceAsStream("/txns.log")).getLines().mkString("")

  private val jsv: JsValue = Json.parse(txnlog) \ "result" \ "transactions"

  behavior of "A parser"

  it should "parse a bunch of transactions" in {
    val parseList: List[Transaction] = Transaction.parseList(jsv)
    assert(parseList.size > 0)
  }

}
