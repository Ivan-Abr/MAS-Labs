package LR2KT.ontology

import jade.content.Predicate

data class Costs(
    var book: Book? = null,
    var price: Int? = null
) : Predicate
