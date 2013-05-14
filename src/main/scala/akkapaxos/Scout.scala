package akkapaxos

import akka.actor._
import Scout._

class Scout[E](
  val acceptors: Set[ActorRef],
  val b: Ballot
) extends Actor with LoggingFSM[ScoutState, ScoutData[E]] {

  override def preStart() {
    acceptors.foreach(_ ! Phase1a(self, b))
  }

  startWith(RUNNING, ScoutData[E](acceptors,  Set()))

  when(RUNNING) {
    case Event(phase1b: Phase1b[E], data) if phase1b.b == b =>

      val newData = data ++ phase1b.accepted - phase1b.l

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

  case class ScoutData[E](waitFor: Set[ActorRef], pValues: Set[PValue[E]]) {
    def ++(vals: Iterable[PValue[E]]): ScoutData[E] = copy(pValues = pValues ++ vals)

    def -(a: ActorRef): ScoutData[E] = copy(waitFor = waitFor - a)

    override def toString = s"waitFo:$waitFor, pBalues.size:${pValues.size}"
  }

}


