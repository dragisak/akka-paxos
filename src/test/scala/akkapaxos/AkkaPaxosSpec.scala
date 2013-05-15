package akkapaxos

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll

class AkkaPaxosSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpec with MustMatchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("AkkaPaxosSpec"))

  val messages = 10000

  override def afterAll() {
    system.shutdown()
  }

  case class Finished(state: Seq[Int])

  trait MyState extends DistributedState[Seq[Int], Int] {
    override def startState = Seq()

    override def append(state: => Seq[Int], event: Int) = {
      val newState = state :+ event
      if (newState.size >= messages) self ! Finished(newState)
      newState
    }
  }

  "all replicas" must {
    "receive all messages in same order" in {
      val crashes = 6

      val numReplicas = crashes + 1
      val numLeaders = 5
      val numAcceptors = crashes * 2 + 1

      val acceptors = (for (i <- 0 until numAcceptors) yield (system.actorOf(Props[Acceptor[Int]], name = s"acceptor-$i"))).toSet
      val replicas = (for (i <- 0 until numReplicas) yield (system.actorOf(Props(new Replica[Seq[Int], Int](numLeaders) with MyState), name = s"replica-$i"))).toSet
      val leaders = for (i <- 0 until numLeaders) yield (system.actorOf(Props(new Leader[Int](i, acceptors, replicas)), name = s"leader-$i"))

      Thread.sleep(100)

      val messageRange = 0 until messages

      for (i <- messageRange) {
        val req = Request[Int](Command("yo", i, i))
        replicas.foreach(_ ! req)
      }

      val res = receiveN(numReplicas, 60 seconds).asInstanceOf[Seq[Finished]]

      res.size must be(numReplicas)
      res.head.state.size must be(messages)
      val s = res.map(_.state).toSet

      s.size must be(1)

      s.head.toSet must be(messageRange.toSet)

    }
  }

}
