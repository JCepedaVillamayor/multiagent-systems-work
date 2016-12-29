package crawlers;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Created by jcepeda on 26/12/16.
 */

public class PageBehaviour extends OneShotBehaviour {
    private String link;

    /**
     * Constructor of the class
     *
     * @param link the book link to obtain the information
     */
    public PageBehaviour(String link, Agent agent) {

        super(agent);
        this.link = link;
    }

    /**
     * Inherited from the Behaviour class, this action obtains the book
     * and adds the information to the Agent
     */
    public void action() {
        Document doc;
        try {
            doc = Jsoup.connect(link).timeout(100000).get();
        } catch (IOException e) {
            System.out.println("The web " + link + " cannot be reached");
            return;
        }
        synchronized (((BibliotecaUCLMSpider) myAgent).links) {
            ((BibliotecaUCLMSpider) myAgent).links.add(doc.text());
        }
        System.out.println(link);
    }
}
