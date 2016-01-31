package akkapaxos

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._

import scala.util.Random

case class Finished(state: Seq[Int])

trait MyState extends DistributedState[Seq[Int], Int] {

  val messages: Int

  val me :ActorRef

  override def startState = Seq()

  override def append(state: => Seq[Int], event: Int) = {
    val newState = state :+ event
    if (newState.size >= messages) me ! Finished(newState)
    newState
  }
}

private class TestReplica(leaders: Int, messageCnt: Int, myself: ActorRef) extends Replica[Seq[Int], Int](leaders) with MyState {
  override val messages = messageCnt
  override val me = myself
}

class AkkaPaxosSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("AkkaPaxosSpec"))

  val messages = 10000
  val numReplicas = 7

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }


  s"all $numReplicas replicas" must {
    s"receive all $messages messages in the same order" in {

      val acceptors = (for (i <- 0 until numReplicas) yield system.actorOf(Props[Acceptor[Int]], name = s"acceptor-$i")).toSet
      val replicas = (for (i <- 0 until numReplicas) yield system.actorOf(Props(classOf[TestReplica], numReplicas, messages, self), name = s"replica-$i")).toSet
      val leaders = for (i <- 0 until numReplicas) yield system.actorOf(Props(classOf[Leader[Int]], i, acceptors, replicas), name = s"leader-$i")

      Thread.sleep(100)

      val messageRange = 0 until messages

      for (i <- messageRange) {
        val req = Request[Int](Command(self, i, i))
        // Pick random nodes from list of replicas
        Random.shuffle(replicas).take(2).foreach(_ ! req)
      }

      val res = receiveN(numReplicas, 4 minutes).asInstanceOf[Seq[Finished]]

      res must have size numReplicas
      res.head.state must have size messages
      val s = res.map(_.state).toSet

      s must have size 1

      s.head.toSet === messageRange.toSet

    }
  }

}
