package variance

class Animal {
  def exist(): Unit = println("...")
}

class Dog() extends Animal {
  def bark(): Unit = println("멍")
}

class Greyhound(name: String) extends Dog

class GermanShepherd(age: Int) extends Dog

object Variance extends App {

  def f(fn: Dog => Dog): Unit = {
    val a = new GermanShepherd(1)
    fn(a).bark()
  }

  def g1: Greyhound => Greyhound = x => x
  def g2: Greyhound => Animal = x => new Animal()
  def g3: Animal => Animal = x => x
  def g4: Animal => Greyhound = x => new Greyhound("sss")

//  f(g1)
//  f(g2)
//  f(g3)
  f(g4)

}
