package akkapaxos

/**
 * The state that we want to distribute by Paxos protocol
 * @tparam E Event type
 * @tparam S State type
 */
trait DistributedState[S,E] {

  /**
   * Beginning state
   * @return Start state
   */
  def startState : S

  /**
   * Append new event to the state
   * @param state Previous state
   * @param event Event
   * @return New State
   */
  def append(state: => S, event: E ) :S

}
