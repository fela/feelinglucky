import stellar.API

object Playground {
  def main(args: Array[String]) = {
    println("Hello!! :)")
    val address1 = "gsMxVfhj1GmHspP5iARzMxZBZmPya9NALr"
    var address2 = "gUCkWvcHk4fkTwcKjMQugfNjxHFay6LBZj"
    val secret1 = io.Source.fromURL(getClass.getResource("/secret1")).getLines.mkString
    val secret2 = io.Source.fromURL(getClass.getResource("/secret2")).getLines.mkString
    val out = API.sign(address1, address2, secret1, BigInt("30000000"))
    out.submit()
  }
}
