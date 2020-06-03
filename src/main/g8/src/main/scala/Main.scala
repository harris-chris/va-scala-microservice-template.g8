package com.visualalpha.service

object Main extends App {
  import Connection._
  import CLI.startCli

  printConnectionParams()
  // Get connection actor, based on environment variables provided
  val connectionActor = defaultConnectionActor
  // Create an actor that will monitor the output channel, so we know what's being sent
  val outputMonitor = startMonitoringOutputChannel(connectionActor)
  // An example function that responds to input we receive
  val exampleResponseFunction: String => String = s => s.toUpperCase
  // Run this function on any new messages which arrive in the input queue
  val inputQueue = startRespondingOnInputQueue(connectionActor, exampleResponseFunction)
  // Publish a message on startup, if required
  publishToDefaultOutput(connectionActor, "I've started up!") 
  // Run some messages through to see if the system is responding as expected
  for (x <- 1 to 2) {
    sendToInputChannel(inputQueue, "make this capitals")
  } 
  // Start the CLI Running
  startCli(connectionActor, inputQueue)

  def quit = {
    Connection.shutdownSystem(connectionActor)
    System.exit(0)
  }
}
