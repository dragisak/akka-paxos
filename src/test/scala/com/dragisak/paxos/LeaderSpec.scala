package com.dragisak.paxos

import akka.actor.ActorRef
import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers

class LeaderSpec extends FlatSpec with ShouldMatchers with MockitoSugar {

  def commands = Array(
    Command("foo", 222, Operation("1")),
    Command("bar", 223, Operation("2"))
  )

  def ballots = Array(
    Ballot(123, 1, mock[ActorRef]),
    Ballot(123, 2, mock[ActorRef]),
    Ballot(123, 3, mock[ActorRef])
  )

  "pmax" should "return map of maximum values" in {


    val pVals = Set(
      PValue(ballots(0), 1, commands(0)),
      PValue(ballots(1), 2, commands(1)),
      PValue(ballots(2), 2, commands(0))
    )

    Leader.pmax(pVals) should equal (Map(
      1L -> Proposal(1, commands(0)),
      2L -> Proposal(3, commands(1))
    ))
  }

  "plus" should "return elements of y as well as elements of x that are not in y" in {
    val x = Map(
      1L -> Proposal(1, commands(0)),
      2L -> Proposal(1, commands(1))
    )
  }


}
