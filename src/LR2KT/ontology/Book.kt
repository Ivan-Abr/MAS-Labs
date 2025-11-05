package LR2KT.ontology

import jade.content.Concept

data class Book(
    var title: String? = null,
    var authors: List<String>? = null,
    var editor: String? = null
) : Concept
