package com.cisco.vss.foundation.http.api.test

import java.util.concurrent.CountDownLatch
import java.util
import java.util.Collections
import akka.actor.{ActorSystem, Props, Actor}
import org.junit.Assert._
import akka.routing.RoundRobinRouter
import com.cisco.vss.foundation.flowcontext.FlowContextFactory
import com.cisco.vss.foundation.http.{HttpResponse, HttpClient, HttpMethod, HttpRequest}
import com.cisco.vss.foundation.http.api.test
import scala.collection.JavaConversions._

/**
 * Created by Yair Ogen on 1/23/14.
 */

class MultithreadTestUtil[S <: HttpRequest, R <: HttpResponse] {
  def testUsingActors(clientRoundRobinTest: HttpClient[S, R]) {

    val as = ActorSystem("systemActor")
    val mainActor = as.actorOf(Props(classOf[MainActor[S, R]],clientRoundRobinTest))

    mainActor ! START

    ClientTestUtil.latch.await()

    mainActor ! STOP

    Thread.sleep(1000)

    assertEquals(ClientTestUtil.NUM_OF_ITER, ClientTestUtil.results.size)

    val groupBy = test.ClientTestUtil.results.groupBy((s) => s.substring(s.indexOf(":") + 1))
    assertEquals(3, groupBy.size)

    for ((key, value) <- groupBy) {
      assertEquals(500, value.size)
    }
  }

}


object ClientTestUtil {
  val NUM_OF_ITER = 1500
  val latch = new CountDownLatch(NUM_OF_ITER)
  var results: util.List[String] = Collections.synchronizedList[String](new util.ArrayList[String]())
}


class MainActor[S <: HttpRequest, R <: HttpResponse](clientRoundRobinTest: HttpClient[S, R]) extends Actor {


  val port1 = 12345
  val port2 = 12346
  val port3 = 12347
  val port4 = 12348

  var server1 = RunTestServer.runServer(port1)
  var server2 = RunTestServer.runServer(port2)
  var server3 = RunTestServer.runServer(port3)
  var server4 = RunTestServer.runServer(port4)

  def receive: Actor.Receive = {
    case START => {

      Thread.sleep(1000)



      var groupBy = Map[String, List[String]]()

      val router = context.system.actorOf(Props[ClientActor[S, R]].withRouter(RoundRobinRouter(5)), "router")

      for (x <- 1 to ClientTestUtil.NUM_OF_ITER) {
        router ! Message(clientRoundRobinTest)
      }
      //println("***************")


    }
    case STOP => {
      server1.stop
      server2.stop
      server3.stop
      server4.stop
    }


  }

  //  def runRequest(request: HttpRequest) {
  //    FlowContextFactory.createFlowContext()
  //    val response = clientRoundRobinTest.executeWithLoadBalancer(request)
  //    if (response.isSuccess) {
  //      val result = response.getResponseAsString
  //      //        println(result)
  //      TestClient.results.add(result)
  //      //      assertEquals("dummy response", result)
  //    } else {
  //      val reason = response.getStatus;
  //      fail(s"failed: $reason")
  //    }
  //  }
}

class ClientActor[S <: HttpRequest, R <: HttpResponse] extends Actor {
  def receive: Actor.Receive = {
    case m: Message[S, R] => {
      FlowContextFactory.createFlowContext()
      val request = HttpRequest.newBuilder()
        .httpMethod(HttpMethod.POST)
        .uri("/test")
        .header("key1","value1")
        .header("key1","value1-1")
//        .queryParams("")
        .contentType("text/html")
        .build();
      val response = m.client.executeWithLoadBalancer(request.asInstanceOf[S])
      if (response.isSuccess) {
        val result = response.getResponseAsString
        ClientTestUtil.results.add(result)
      }
      ClientTestUtil.latch.countDown()
    }
  }
}

case class START()

case class STOP()

case class Message[S <: HttpRequest, R <: HttpResponse](client: HttpClient[S, R])