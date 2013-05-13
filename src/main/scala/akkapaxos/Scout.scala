package akkapaxos

import akka.actor._
import Scout._

class Scout(
  val acceptors: Set[ActorRef],
  val b: Ballot
) extends Actor with LoggingFSM[ScoutState, ScoutData] {

  override def preStart() {
    acceptors.foreach(_ ! Phase1a(self, b))
  }

  startWith(RUNNING, ScoutData(acceptors))

  when(RUNNING) {
    case Event(Phase1b(a, b1, r), data) if b1 == b =>

      val newData = data ++ r - a

      if (newData.waitFor.size * 2 < acceptors.size) {
        context.parent ! Adopted(b, newData.pValues)
        stop()
      } else {
        stay using newData
      }

    case Event(Phase1b(a, b1, r), _) =>
      context.parent ! Preempted(b1)
      stop()

  }
}

sealed trait ScoutState

object Scout {

  case object RUNNING extends ScoutState

  case class ScoutData(waitFor: Set[ActorRef], pValues: Set[PValue] = Set()) {
    def ++(vals: Iterable[PValue]) = copy(pValues = pValues ++ vals)

    def -(a: ActorRef) = copy(waitFor = waitFor - a)
  }

}


