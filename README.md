rillit
======

Experimental functional lenses for Scala using Macros.

Uses `Dynamic` in combination with Macros to create a nice interface.

Better documentation is on it's way, but here's the gist:

```scala
case class Electronic(email: String, web: String)
case class Contact(electronic: Electronic)
case class Name(first: String, last: String)
case class Person(name: Name, age: Int, contact: Contact)

object Main {
  val aki = Person(
    name = Name("Aki", "Saarinen"),
    age  = 27,
    contact = Contact(
      electronic = Electronic(
        email = "aki@akisaarinen.fi",
        web   = "http://akisaarinen.fi"
      )
    )
  )

  def main(args: Array[String]) {
    val email = new LensBuilder[Person].contact.electronic.email.apply

    val aki2 = email.set(aki, "aki2@akisaarinen.fi")

    println("Original: %s".format(email.get(aki)))
    println("Updated:  %s".format(email.get(aki2)))
  }
}
```

When run, this will produce the following: 

```
Original: aki@akisaarinen.fi
Updated:  aki2@akisaarinen.fi
```
