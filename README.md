Rillit
======

Experimental functional lenses for Scala 2.10 using Macros.

Difference to other implementations
===================================

Rillit combines the use of `Dynamic` with macros to provide creation of
functional lenses with minimal boilerplate.

The lenses themselves are very bare-bones here, the main point is to
demonstrate the ability to create lenses in a boilerplate-free way for nested
case classes. Rillit provides a `Lenser` which does just that, using macros and
`Dynamic`. Features included in e.g. Scalaz or Shapeless lenses could be
combined with Rillit `Lenser` to make lens creation more convenient.

Also, this being a very early proof-of-concept experiment, the code is not very
pretty (luckily there's not very much of it).

Example
=======

```scala
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
    val email = Lenser[Person].contact.electronic.email.apply
    val akiNewEmail = email.set(aki, "aki2@akisaarinen.fi")
    println("Email update: %s -> %s".format(email.get(aki), email.get(akiNewEmail)))

    // Create two lenses, first and last, and update both names
    // (again creates a new instance of Person)
    val first = Lenser[Person].name.first.apply
    val last  = Lenser[Person].name.last.apply

    val setFirst: (Person => Person) = (p => first.set(p, "Chuck"))
    val setLast:  (Person => Person) = (p => last.set(p, "Norris"))
    val akiNewName = (setFirst andThen setLast)(aki)

    println("Original person: %s".format(aki))
    println("Updated fields:  %s".format(akiNewName))
  }
}
```

When run, this will produce the following: 

```
[info] Running rillit.Main 
Email update: aki@akisaarinen.fi -> aki2@akisaarinen.fi
Original person: Person(Name(Aki,Saarinen),Contact(Electronic(aki@akisaarinen.fi,http://akisaarinen.fi)))
Updated fields:  Person(Name(Chuck,Norris),Contact(Electronic(aki@akisaarinen.fi,http://akisaarinen.fi)))
```

Requirements
============

* Scala 2.10 (tested with 2.10.0-RC5, but will probably work with older release candidates as well)
* SBT 0.12
* A bit of love for functional lenses

Usage
=====

Rillit is not currently distributed as a library, as this is just an experiment on
how the functional lenses could be implemented. To try it out, install SBT 0.12,
and just run the example with:

```
sbt "project rillit-testing" run
```

Caveats
=======

Macros are an *experimental* feature of 2.10, so it is probably not a good idea
to use something like this in critical production code just yet. Also, the code
is not very pretty as-is, it's just a proof-of-concept I made to convince
myself that this is even possible with `Dynamic` and macros. 

Inspiration
===========

Functional lenses has been implemented in a variety of forms for many languages.
Implementations for Scala, which were the inspiration to create rillit, include:

* Scalaz: https://github.com/scalaz/scalaz
* Shapeless: https://github.com/milessabin/shapeless
* Macrocosm: https://github.com/retronym/macrocosm

License
=======

Rillit is released under the Apache License, Version 2.0.
