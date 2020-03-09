package fpis

trait RNG {
  def nextInt: (Int, RNG)
}
