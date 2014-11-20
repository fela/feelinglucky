package stellar

import scala.util.Random

object Lottery {
  // map from multiplier to probability
  val values = List(
    (10.0, 0.02),
    (5.0, 0.03),
    (2.0, 0.15),
    (1.0, 0.2),
    (0.5, 0.1),
    (0.01, 0.5)
  )
  def play(value: BigInt): BigInt = {
    (value.toDouble * randomMultiplier()).toLong
  }

  def findIndex(index: Double, list: List[(Double, Double)]) : Double =
    list match {
      // NOTE: empty elements are not allowed
      case (mult, _) :: Nil =>
        mult // return the last element no matter what
      case (mult, topIndex) :: tail => {
        val newIndex = index-topIndex
        if (newIndex < 0)
          mult
        else
          findIndex(newIndex, tail)
      }
      case Nil =>
        throw new IllegalArgumentException("cannot findIndex in an empty list")
    }

  val random = new Random()
  def randomMultiplier() : Double = {
    val randomDouble = random.nextDouble()
    // loop values until we find what we look for
    findIndex(randomDouble, values)
  }
}
