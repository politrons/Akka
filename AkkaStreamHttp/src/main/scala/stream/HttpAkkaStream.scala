package stream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.ByteString
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object HttpAkkaStream extends App {

  implicit val system: ActorSystem = ActorSystem("AkkaStreamHttp")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  private val port = 8080

  Http().bindAndHandle(getRoutes, "localhost", port)

  println(s"Akka Stream Server running in port $port")

  // ADT & JSON FORMAT
  //####################

  /**
    * We create the request data for the POST request,
    */
  case class UserDataType(id: Int, name: String, age: Int, sex: String)

  /**
    * Json format from Spray library, that we use as implicit conversion to transform a json entry to Data type.
    * We have to use the operator [jsonFormatX] where [X] is the number of arguments of your Data type.
    */
  implicit val requestDataJsonFormat: RootJsonFormat[UserDataType] = jsonFormat4(UserDataType.apply)

  var users = Map[Int, UserDataType]()

  //  ROUTES
  //#############
  /**
    * Function where we define all routes of our service.
    *
    * We are able to use [asSourceOf] function which thanks to implicit [EntityStreamingSupport]
    * which expect a json array to chunk into array of Data type [UserDataType] into Akka [Source]
    *
    * We are able to Marshall from Json to Data type [UserDataType] thanks to implicit conversion RootJsonFormat[UserDataType]
    *
    * Once we have the Source from our request we can use [runFold] operator to be able to receive  each [UserDataType] entry,
    * to process the whole request as a stream of data.
    *
    * @return Route type that it will used by [Http().bindAndHandle] to route request to the proper handler.
    */
  private def getRoutes: Route = {

    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    path("requestStream") {
      post {
        entity(asSourceOf[UserDataType]) { source =>
          complete {
            source
              .runFold(users)((users, user) => {
                users == users ++ Map(user.id -> user)
                println(s"New user ${user.name} added")
                users
              }).map(users => s"total number of users in system ${users.size}")
          }
        }
      }
    }

    /**
      * Using [throttle] operator Sends elements downstream with speed limited to `elements/per`. In other words, this stage set the maximum rate
      * for emitting messages.
      * Backpressures when downstream backpressures or the incoming rate is higher than the speed limit
      */
    path("requestStreamGet") {
      get {
        val sourceOfInformation = Source("Request received")
          .throttle(elements = 1000, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
          .map(_.toUpper)
          .map(s => ByteString(s + "\n"))
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, sourceOfInformation))
      }
    }
  }
}