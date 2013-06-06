package akkapaxos

import akka.actor._
import Commander._

class Commander[E](
  val acceptors: Set[ActorRef],
  val replicas: Set[ActorRef],
  val pValue: PValue[E]
) extends Actor with LoggingFSM[CommanderState, WaitFor] {

  override def preStart() {
    acceptors.foreach(_ ! Phase2a(self, pValue))
  }

  startWith(RUNNING, WaitFor(acceptors))

  when(RUNNING) {

    case Event(Phase2b(a, b1), waitFor) if b1 == pValue.b =>
      val newState = waitFor - a
      if (newState.acceptors.size < acceptors.size / 2) {
        log.debug("Reached consensus on {}", pValue)
        val decision = Decision(pValue.s, pValue.p)
        replicas.foreach(_ ! decision)
        context.parent ! decision // let leader know that decision has been reached
        stop()
      } else {
        stay using newState
      }

    case Event(Phase2b(_, b1), _) =>
      context.parent ! Preempted(b1)
      stop()
  }

}

sealed trait CommanderState

object Commander {

  case object RUNNING extends CommanderState

  case class WaitFor(acceptors: Set[ActorRef]) {
    def -(acceptor: ActorRef) = copy(acceptors - acceptor)

    override lazy val toString = s"acceptors.size=${acceptors.size}"
  }

}
