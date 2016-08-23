import java.io.IOException

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MonorailConnector(environment: Environment.EnumVal) {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  lazy val connectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(environment.host, environment.port)

  private def request(request:HttpRequest): Future[HttpResponse] =
    Source.single(request).via(connectionFlow).runWith(Sink.head)

  def issuePostRequest(event: DummyEvent) : Future[Either[String, DummyEvent]] = {
    println(s"Sending event '$event'")
    val doc =
      s"""
        |{
        | "application":"monorail-producer-test-app",
        | "topic":"seq-nums",
        | "event":"$event"
        |}
      """.stripMargin
//    request(HttpRequest(uri = "/client", method = HttpMethods.POST, entity = HttpEntity(doc))).flatMap { response =>
    request(HttpRequest(uri = "/in/data", method = HttpMethods.PUT, entity = HttpEntity(doc))).flatMap { response =>
      response.status match {
        case OK | Accepted => Future.successful(Right(event))
        case BadRequest => Future.successful(Left(s"bad request"))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"FAIL - ${response.status}"
          Future.failed(new IOException(error))
        }
      }
    }
  }
}
