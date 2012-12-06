package rillit

case class Person(name: Name,  contact: Contact)
case class Name(first: String, last: String)
case class Contact(electronic: Electronic)
case class Electronic(email: String, web: String)

object Main {
  val aki = Person(
    name = Name("Aki", "Saarinen"),
    contact = Contact(
      electronic = Electronic(
        email = "aki@akisaarinen.fi",
        web   = "http://akisaarinen.fi"
      )
    )
  )

  def main(args: Array[String]) {
    // Implicit conversion Lenser => Lens
    import Lenser._

    // The gist of Rillit: boiler-plate free lens creation
    val email = Lenser[Person].contact.electronic.email
    val name  = Lenser[Person].name

    // Test case 1: Update email
    val akiNewEmail = email.set(aki, "aki2@akisaarinen.fi")
    println("Email update: %s -> %s".format(email.get(aki), email.get(akiNewEmail)))

    // test case 2: Update name
    val akiNewName = name.set(aki, Name("Chuck", "Norris"))
    println("Original person: %s".format(aki))
    println("Updated name:    %s".format(akiNewName))
  }
}
