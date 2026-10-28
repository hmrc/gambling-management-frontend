package models.agent

object AgentClientUtil {
  val clientsList: List[AgentClient] = List(
    AgentClient(
      regime = "MGD",
      regNumber = "XVM00000001001",
      clientName = Some("""ABC Ltd"""),
      agentOwnRef = Some("EZ00100")
    ),
    AgentClient(
      regime = "MGD",
      regNumber = "XAM00000001002",
      clientName = Some("""XYZ Builders"""),
      agentOwnRef = Some("CD002")
    ),
    AgentClient(
      regime = "MGD",
      regNumber = "XCM00000001003",
      clientName = Some("""Betelgeuse Construction 3"""),
      agentOwnRef = Some("UP374")
    ),
    AgentClient(
      regime = "MGD",
      regNumber = "XEM00000001004",
      clientName = Some("""Heritage Contractors 4"""),
      agentOwnRef = Some("WY080")
    )
  )
}
