trait Enum { //DIY enum type
  import java.util.concurrent.atomic.AtomicReference //Concurrency paranoia

  type EnumVal <: Value //This is a type that needs to be found in the implementing class

  private val _values = new AtomicReference(Vector[EnumVal]()) //Stores our enum values

  //Adds an EnumVal to our storage, uses CCAS to make sure it's thread safe, returns the ordinal
  private final def addEnumVal(newVal: EnumVal): Int = { import _values.{get, compareAndSet => CAS}
    val oldVec = get
    val newVec = oldVec :+ newVal
    if((get eq oldVec) && CAS(oldVec, newVec)) newVec.indexWhere(_ eq newVal) else addEnumVal(newVal)
  }

  def values: Vector[EnumVal] = _values.get //Here you can get all the enums that exist for this type

  //This is the trait that we need to extend our EnumVal type with, it does the book-keeping for us
  protected trait Value extends Ordered[Value] { self: EnumVal => //Enforce that no one mixes in Value in a non-EnumVal type
  final val ordinal = addEnumVal(this) //Adds the EnumVal and returns the ordinal

    def compare(that: Value) = this.ordinal - that.ordinal

    def name: String //All enum values should have a name

    override def toString = name //And that name is used for the toString operation
    override def equals(other: Any) = this eq other.asInstanceOf[AnyRef]
    override def hashCode = 31 * (this.getClass.## + name.## + ordinal)
  }
}

object Environment extends Enum {

  sealed abstract class EnumVal(
                                val host : String,
                                val port: Int) extends Value {
    override def toString = s"$host:$port"
  }

  val LOCAL = new EnumVal("localhost", 8888) {val name = "local"}
  val RC = new EnumVal("monorail-in-00.servers.despegar.it", 9290) {val name = "rc"}
  val PROD = new EnumVal("proxy.despexds.net", 80) {val name = "prod"}

  def fromName(name: String) = {
    values.find(e => e.name == name)
  }
}
