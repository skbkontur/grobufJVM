package grobuf

// (1..7).joinToString("\n") { i -> "class Tuple$i<${(1..i).joinToString(", ") { j -> "out T$j" }}>(${(1..i).joinToString(", ") { j -> "@JvmField val item$j: T$j" }}) : Tuple()" }

abstract class Tuple

class Tuple1<out T1>(@JvmField val item1: T1) : Tuple()
class Tuple2<out T1, out T2>(@JvmField val item1: T1, @JvmField val item2: T2) : Tuple()
class Tuple3<out T1, out T2, out T3>(@JvmField val item1: T1, @JvmField val item2: T2, @JvmField val item3: T3) : Tuple()
class Tuple4<out T1, out T2, out T3, out T4>(@JvmField val item1: T1, @JvmField val item2: T2, @JvmField val item3: T3, @JvmField val item4: T4) : Tuple()
class Tuple5<out T1, out T2, out T3, out T4, out T5>(@JvmField val item1: T1, @JvmField val item2: T2, @JvmField val item3: T3, @JvmField val item4: T4, @JvmField val item5: T5) : Tuple()
class Tuple6<out T1, out T2, out T3, out T4, out T5, out T6>(@JvmField val item1: T1, @JvmField val item2: T2, @JvmField val item3: T3, @JvmField val item4: T4, @JvmField val item5: T5, @JvmField val item6: T6) : Tuple()
class Tuple7<out T1, out T2, out T3, out T4, out T5, out T6, out T7>(@JvmField val item1: T1, @JvmField val item2: T2, @JvmField val item3: T3, @JvmField val item4: T4, @JvmField val item5: T5, @JvmField val item6: T6, @JvmField val item7: T7) : Tuple()
class Tuple8<out T1, out T2, out T3, out T4, out T5, out T6, out T7, out TRest>(@JvmField val item1: T1, @JvmField val item2: T2, @JvmField val item3: T3, @JvmField val item4: T4, @JvmField val item5: T5, @JvmField val item6: T6, @JvmField val item7: T7, @JvmField val rest: TRest) : Tuple()
