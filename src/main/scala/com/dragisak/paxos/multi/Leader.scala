package com.dragisak.paxos.multi

import akka.actor._
import concurrent.duration._
import compat.Platform._
import concurrent.ExecutionContext.Implicits.global
import util.Random

class Leader(
              val id: Int,
              val acceptors: Set[ActorRef],
              val replicas: Set[ActorRef]
              ) extends Actor with ActorLogging {

  var ballotNum = Ballot(id, 0, self)
  var active = false
  var proposals = Set[Proposal]()

  val random = new Random(currentTime)


  override def preStart = spawnScout(ballotNum)

  val RAND = 2000

  def kablooy {
    if (random.nextInt(RAND) == 0) throw new RuntimeException("Kablooy " + id)
  }

  def receive = {

    case prop@Proposal(s, p) =>

      if (random.nextInt(RAND) != 0) {
        if (proposals.find(_.s == s).isEmpty) {
          proposals = proposals + prop
          if (active) {
            spawnCommander(PValue(ballotNum, s, p))
          }
        }
      } else {
        // Little chaos monkey eat this message
        log.info("Gulp %d, %s".format(id, prop))
      }

    case Adopted(b, pVals) => {
      proposals = plus(proposals, pmax(pVals))


      proposals.foreach {
        case Proposal(s, p) => spawnCommander(PValue(ballotNum, s, p))
      }

      active = true
    }

    case Preempted(b1) =>
      if (b1 > ballotNum) {
        spawnPoller(b1)
      }


    case Timeout(b) =>
      if (b > ballotNum) {
        active = false
        ballotNum = ballotNum.increment
        spawnScout(ballotNum)
      }

  }


  private def spawnCommander(pVal: PValue) {
    context.actorOf(Props(new Commander(acceptors, replicas, pVal)))
  }

  private def spawnPoller(b: Ballot) {
    context.actorOf(Props(new Poller(b, 80)))
  }

  private def spawnScout(b: Ballot) {
    context.actorOf(Props(new Scout(acceptors, b)))
  }

  private def pmax(pVals: Set[PValue]) = pVals.groupBy(_.s)
    .map(_._2.max(Ordering.by[PValue, Ballot](_.b)))
    .map(v => Proposal(v.s, v.p))
    .toSet

  /**
   * Elements of y as well as elements of x that are not in y
   */
  private def plus(x: Set[Proposal], y: Set[Proposal]) = y ++ x.filter(v => y.find(_.s == v.s).isEmpty)


  class Poller(b: Ballot, timeout: Long) extends Actor {


    var lastResponse = currentTime

    override def preStart() {
      context.system.scheduler.schedule(30.millisecond, 30.millisecond, self, Ping)
    }

    def receive = {
      case Ping => if (lastResponse + timeout < currentTime) {
        context.parent ! Timeout(b)
        self ! PoisonPill
      } else {
        b.l ! Ping
      }

      case Pong => lastResponse = currentTime
    }
  }

}
