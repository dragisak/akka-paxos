package akkapaxos

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import concurrent.{Await, Future}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll

class AkkaPaxosSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpec with MustMatchers with BeforeAndAfterAll {

  implicit val timeout = Timeout(5 seconds)
  implicit val ec = system.dispatcher

  def this() = this(ActorSystem("AkkaPaxosSpec"))


  override def afterAll() {
    system.shutdown()
  }

  "all replicas" must {
    "receive all messages in same order" in {
      val crashes = 6
      val cntLeaders = 5
      val messages = 10000

      val acceptors = (for (i <- 0 until crashes * 2 + 1) yield (system.actorOf(Props[Acceptor], name = s"acceptor-$i"))).toSet
      val replicas = (for (i <- 0 until crashes + 1) yield (system.actorOf(Props(new Replica(cntLeaders)), name = s"replica-$i"))).toSet
      val leaders = for (i <- 0 until cntLeaders) yield (system.actorOf(Props(new Leader(i, acceptors, replicas)), name = s"leader-$i"))

      Thread.sleep(100)

      for (i <- 0 until messages) {
        val req = Request(Command("yo", i, Operation(i.toString)))
        replicas.foreach(_ ! req)
      }

      Thread.sleep(20000)

      val res = Await.result(Future.sequence(replicas.toSeq.map(r => (r ? GetState).mapTo[Seq[Operation]])), 20.seconds)

      res.size must be(replicas.size)
      res.head.size must be (messages)
      val s = res.toSet

      s.size must be  (1)

    }
  }

}
