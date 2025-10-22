package LR2T.ontology;

import jade.content.AgentAction;

public class Sell implements AgentAction {
    private Book item;

    public Book getItem() {
        return item;
    }

    public void setItem(Book item) {
        this.item = item;
    }

}
