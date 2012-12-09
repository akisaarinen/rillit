package rillit

package object util {
  case class Tree[A](value: A, depth: Int, children: List[Tree[A]]) {
    def histo[B](f: (A, Int, List[Tree[B]]) => B): Tree[B] = {
      val newChildren = children.map(_ histo f)
      Tree(f(value, depth, newChildren), depth, newChildren)
    }

    def flatten: List[A] = value :: children.flatMap(_.flatten).toList
  }

  object Tree {
    def unfold[A](values: List[A])(f: A => List[A]): Tree[Option[A]] = {
      def unfoldBranches(value: A, depth: Int): Tree[Option[A]] = Tree(
        Some(value),
        depth,
        f(value).map(v => unfoldBranches(v, depth + 1))
      )

      Tree(None, 0, values.map(v => unfoldBranches(v, 1)))
    }
  }
}

