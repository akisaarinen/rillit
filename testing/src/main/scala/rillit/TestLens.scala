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
    // Create lens to email and update (i.e. create a new instance
    // of Person).
    val email = Lenser[Person]().contact.electronic.email.apply
    val akiNewEmail = email.set(aki, "aki2@akisaarinen.fi")
    println("Email update: %s -> %s".format(email.get(aki), email.get(akiNewEmail)))

    // Create two lenses, first and last, and update both names
    // (again creates a new instance of Person)
    val first = Lenser[Person]().name.first.apply
    val last  = Lenser[Person]().name.last.apply

    val setFirst: (Person => Person) = (p => first.set(p, "Chuck"))
    val setLast:  (Person => Person) = (p => last.set(p, "Norris"))
    val akiNewName = (setFirst andThen setLast)(aki)

    println("Original person: %s".format(aki))
    println("Updated fields:  %s".format(akiNewName))
  }
}
