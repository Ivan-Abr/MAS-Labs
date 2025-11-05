package LR2KT.ontology

import LR2T.ontology.Book
import LR2T.ontology.BookTradingVocabulary
import LR2T.ontology.Costs
import LR2T.ontology.Sell
import jade.content.onto.BasicOntology
import jade.content.onto.Ontology
import jade.content.onto.OntologyException
import jade.content.schema.AgentActionSchema
import jade.content.schema.ConceptSchema
import jade.content.schema.ObjectSchema
import jade.content.schema.PredicateSchema
import jade.content.schema.PrimitiveSchema

class BookTradingOntology private constructor() : Ontology(ONTOLOGY_NAME, BasicOntology.getInstance()),
    BookTradingVocabulary {
    // Private constructor
    init {
// The Book-trading ontology extends the basic ontology
        try {
            add(ConceptSchema(BookTradingVocabulary.BOOK), Book::class.java)
            add(PredicateSchema(BookTradingVocabulary.COSTS), Costs::class.java)
            add(AgentActionSchema(BookTradingVocabulary.SELL), Sell::class.java)

            // Structure of the schema for the Book concept
            val cs = getSchema(BookTradingVocabulary.BOOK) as ConceptSchema
            cs.add(BookTradingVocabulary.BOOK_TITLE, getSchema(BasicOntology.STRING) as PrimitiveSchema?)
            cs.add(
                BookTradingVocabulary.BOOK_AUTHORS, getSchema(BasicOntology.STRING) as PrimitiveSchema?, 0,
                ObjectSchema.UNLIMITED
            )
            cs.add(
                BookTradingVocabulary.BOOK_EDITOR,
                getSchema(BasicOntology.STRING) as PrimitiveSchema?,
                ObjectSchema.OPTIONAL
            )

            // Structure of the schema for the Costs predicate
            val ps = getSchema(BookTradingVocabulary.COSTS) as PredicateSchema
            ps.add(BookTradingVocabulary.COSTS_ITEM, cs)
            ps.add(BookTradingVocabulary.COSTS_PRICE, getSchema(BasicOntology.INTEGER) as PrimitiveSchema?)

            // Structure of the schema for the Sell agent action
            val `as` = getSchema(BookTradingVocabulary.SELL) as AgentActionSchema
            `as`.add(BookTradingVocabulary.SELL_ITEM, getSchema(BookTradingVocabulary.BOOK) as ConceptSchema?)
        } catch (oe: OntologyException) {
            oe.printStackTrace()
        }
    }

    companion object {
        // The name identifying this ontology
        const val ONTOLOGY_NAME: String = "Book-trading-ontology"
        // Retrieve the singleton Book-trading ontology instance
        // The singleton instance of this ontology
        val instance: Ontology = BookTradingOntology()
    }
}
