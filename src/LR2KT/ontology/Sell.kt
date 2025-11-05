package LR2KT.ontology

import jade.content.AgentAction

data class Sell(
    var book: Book? = null,
) : AgentAction
