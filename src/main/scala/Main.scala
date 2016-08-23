import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    if(!validateArgs(args)) printUsageAndExit

    val env: Environment.EnumVal = Environment.fromName(args(0)).get
    val from: Int = args(1).toInt
    val amount: Int = args(2).toInt
    val prefix: String = args(3)

    sendRequests(from, amount, prefix, env)
    System.exit(0)
  }

  def validateArgs(args: Array[String]): Boolean = {
    val isValid = args.length == 4 || args(2).toInt > 0
    isValid && Environment.fromName(args(0)).isDefined
  }

  def printUsageAndExit = {
    println(
      """
        |Usage: monorail-producer [prod | rc] START AMOUNT PREFIX
        |
        | START: Starting event index
        | AMOUNT: Amount of events
        | PREFIX: Event prefix
        |
        |Example: monorail-producer rc 0 500 myEvent
      """.stripMargin)
    System.exit(0)
  }

  def sendRequests(from: Int, amount: Int, prefix: String, env: Environment.EnumVal) = {
    println(s"Starting to send $amount events, starting from $from, with prefix '$prefix'\n")

    val connector = new MonorailConnector(env)

    (from until from + amount).map { case each =>
      Await.ready(connector.issuePostRequest(DummyEvent(each, prefix)).map(fr => treatResponse(fr)), 10 seconds)
    }

    println("\nAll done!")
  }

  def treatResponse(response: Either[String, DummyEvent]) = {
    response match {
      case Right(entity) =>
        println(s"Successfully sent event '$entity'")

      case Left(s) =>
        println(s"Something went wrong. Reason: $s")
    }
  }
}

case class DummyEvent(n: Int, prefix: String) {
  override def toString: String = s"$prefix-$n"
}