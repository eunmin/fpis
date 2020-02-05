package step7

class Example {
  def main() = {
    val x = "Hello, World"
    val r1 = x.reverse
    val r2 = x.reverse

    val x = new StringBuiler("Hello")
    val y = x.append(", World")
    val r1 = y.toString
    val r2 = y.toString
  }
}
