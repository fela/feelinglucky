import org.scalatest._
import stellar.Lottery

class FindIndexTest extends FlatSpec {
  behavior of "A findIndex"

  it should "return the last value if there are is only one" in {
    val list = List((7.0, 0.1))
    assert(Lottery.findIndex(2.0, list) === 7.0)
  }

  it should "return the correct value" in {
    val list = List((7.0, 0.1), (8.0, 0.4), (1.0, 0.5))
    assert(Lottery.findIndex(0.05, list) === 7.0)
    assert(Lottery.findIndex(0.15, list) === 8.0)
    assert(Lottery.findIndex(0.45, list) === 8.0)
    assert(Lottery.findIndex(0.55, list) === 1.0)
    assert(Lottery.findIndex(0.95, list) === 1.0)
    assert(Lottery.findIndex(1.05, list) === 1.0)
  }
}
