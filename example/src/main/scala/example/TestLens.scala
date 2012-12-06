package example

import rillit._

object Main {
  // A simple instance of Person class used for examples
  val person = Person(
    name    = Name("Aki", "Saarinen"),
    contact = Contact(
      email = Email("aki", "akisaarinen.fi"),
      web   = "http://akisaarinen.fi"
    )
  )

  // Simplest possible
  def getterExample() {
    val lens = Lenser[Person].contact.email
    println("Getter example:")
    println("  Email: %s".format(lens.get(person))) // 'aki@akisaarinen.fi'
  }

  // Use lens created on-the-fly to set a value into nested case classes.
  // The traditional way of doing this without lenses is this:
  //   val updated = person.copy(contact = person.contact.copy(email = something))
  def setterExample() {
    val updated = Lenser[Person].contact.email.set(person, Email("foo", "foobar.com"))

    println("Setter example:")
    println("  Original person: %s".format(person))  // email = 'aki@akisaarinen.fi'
    println("  Updated person:  %s".format(updated)) // email = 'foo@foobar.com'
  }

  // Here we create two simple lenses and demonstrate the ability to compose
  // them; this is very useful in practice with functional lenses. Of course in
  // this case we could just `Lenser[Person].contact.email.user` but that wouldn't
  // demonstrate composition :)
  def lensCompositionExample() {
    val user  = Lenser[Email].user
    val email = Lenser[Person].contact.email

    val lens = email andThen user

    println("Composed lens example:")
    println("  Getter: %s".format(lens.get(person)))         // 'aki'
    println("  Setter: %s".format(lens.set(person, "john"))) // email = 'john@akisaarinen.fi'
  }

  case class Person(name: Name,  contact: Contact)
  case class Name(first: String, last: String)
  case class Contact(email: Email, web: String)
  case class Email(user: String, domain: String) {
    override def toString = "%s@%s".format(user, domain)
  }

  def main(args: Array[String]) {
    getterExample()
    setterExample()
    lensCompositionExample()
  }
}
