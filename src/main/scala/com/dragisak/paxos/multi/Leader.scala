package com.dragisak.paxos.multi

import akka.actor._

class Leader(
  val name: String,
  val acceptors: Set[ActorRef],
  val replicas: Set[ActorRef]
  ) extends Actor with ActorLogging {

  var ballotNum = name + ":0"
  var active = false
  var proposals = Set[Proposal]()


  override def preStart = spawnScout(ballotNum)


  def receive = {

    case prop@Proposal(s, p) =>

      log.info("Got " + prop)

      if (proposals.find(_.s == s).isEmpty) {
        proposals = proposals + prop
        if (active) {
          spawnCommander(PValue(ballotNum, s, p))
        }
      }

    case Adopted(b, pVals) =>
      log.info("Got Adopted(%s)" format b)
      proposals = plus(proposals, pmax(pVals))


      proposals.foreach {
        case Proposal(s, p) => spawnCommander(PValue(ballotNum, s, p))
      }

      active = true

    case Preempted(b1) =>
      log.info("Preempted(%s)" format b1)
      if (b1 > ballotNum) {
        active = false
        val split = b1.split(":")
        ballotNum = split(0) + ":" + (split(1).toLong + 1L).toString
        spawnScout(ballotNum)
      }

  }

  private def spawnCommander(pVal: PValue) {
    context.actorOf(Props(new Commander(self, acceptors, replicas, pVal)))
  }

  private def spawnScout(b: String) {
    context.actorOf(Props(new Scout(self, acceptors, b)))
  }

  private def pmax(pVals: Set[PValue]) = pVals.groupBy(_.s)
    .map(_._2.max(Ordering.by[PValue, String](_.b)))
    .map(v => Proposal(v.s, v.p))
    .toSet

  /**
   * Elements of y as well as elements of x that are not in y
   */
  private def plus(x: Set[Proposal], y: Set[Proposal]) = y ++ x.filter(v => y.find(_.s == v.s).isEmpty) // TODO


}
