Rillit
======

Rillit provides experimental functional lenses for Scala 2.10. 

Right now it is mostly an experiment in providing minimum effort creation of
lenses using a `Lenser`, implemented with Scala 2.10 macros and `Dynamic`.
Longer-term aim of `rillit` is to be a stand-alone functional lens
implementation.

Why?
====

Functional lenses are composable getters and setters for immutable data
structures, i.e. usually case classes in Scala.

Say you have the following classes and one class instance:

```scala
case class A(b: B)
case class B(c: C)
case class C(d: Int)

val a = A(B(C(3)))
```

Now, you want to modify the integer value `d` from 3 to 4 and create a new
instance of `A` with that value changed. This pattern comes up very often when
you're writing functional code with immutable data structures.

Using pure Scala, you would do this:

```
scala> a.copy(b = a.b.copy(c = a.b.c.copy(d = 4)))
res3: A = A(B(C(4)))
```

However, this is ugly and using lenses we can do better. Using the lens builder
from rillit, called `Lenser`, we can just write this:

```
scala> Lenser[A].b.c.d.set(a, 4)
res4: A = A(B(C(4)))
```

Now, this is clearly a lot better. There is a whole lot more we can do with
lenses (i.e. you can for example compose your lenses together, forming new
lenses), but just solving this case is great on its own.

Difference to other implementations
===================================

The lenses themselves are very bare-bones here, the main point is to
demonstrate the ability to create lenses in a boilerplate-free way for nested
case classes. `Lenser` does just that, using macros and
`Dynamic`. 

Lens features included in e.g. Scalaz or Shapeless lenses could be combined
with the functionality of `Lenser`, to make lens both lens creation and usage
as convenient as possible. At the moment of writing this, creation of lenses in
both Scalaz and Shapeless contains more boilerplate than in Rillit.

Also, this being a very early proof-of-concept experiment, the code is not very
pretty (luckily there's not very much of it).

A longer example
================

```scala
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

  // Simples possible
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
```

When run, this will produce the following: 

```
Getter example:
  Email: aki@akisaarinen.fi
Setter example:
  Original person: Person(Name(Aki,Saarinen),Contact(aki@akisaarinen.fi,http://akisaarinen.fi))
  Updated person:  Person(Name(Aki,Saarinen),Contact(foo@foobar.com,http://akisaarinen.fi))
Composed lens example:
  Getter: aki
  Setter: Person(Name(Aki,Saarinen),Contact(john@akisaarinen.fi,http://akisaarinen.fi))
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

Following projects motivated me to create Rillit, either because of their lens
implementations, use of Scala macros, or both:

* Scalaz: https://github.com/scalaz/scalaz
* Shapeless: https://github.com/milessabin/shapeless
* Macrocosm: https://github.com/retronym/macrocosm
* sqltyped: https://github.com/jonifreeman/sqltyped

License
=======

Rillit is released under the Apache License, Version 2.0.
