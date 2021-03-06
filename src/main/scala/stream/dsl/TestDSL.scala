package stream.dsl

import akka.stream.ClosedShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{GraphDSL, RunnableGraph}

/**
  * Created by pabloperezgarcia on 11/06/2017.
  *
  * Using Graph is a really cool way to create DSLs and use it for implement architectures with compensations
  * such as distributed sagas.
  *
  * You just need to use the GraphDSL.Builder to use it to glue your partialFunctions, where you will introduce
  * your business logic. And then depending on the numeric output of that logic, we will create new flows of execution
  * of your code.
  *
  * All the operators created, Source, Flow and Sink can be plugged together in this DSL using ~> <~ in any direction.
  */
object TestDSL extends MainDSL with App {

  RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

    init(builder)

    Given("This_is_a_hello_world") ~> When(s"I change character _") ~> \

    \.out(0) ~> AndThen("I expect to receive 200") ~> Then("I expect to receive 200")

    ClosedShape

  }).run()

}


