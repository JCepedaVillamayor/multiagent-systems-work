package communicators;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;
import models.Book;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by jcepeda on 12/01/17.
 */
public class TaskAgent extends Agent {
    private int receiversLeft;
    private LinkedList<Book> booksAdquired;
    MessageTemplate template = MessageTemplate.and(
            MessageTemplate.MatchSender(new AID("view", AID.ISLOCALNAME)),
            MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

    public void setup() {
        receiversLeft = 0;
        booksAdquired = new LinkedList<Book>();
        addBehaviour(new QueryHandler(this));
    }

    /**
     * Behaviour dedicated to send to the crawlers the query to search
     */
    private class QueryHandler extends Behaviour {
        LinkedList<AID> receivers;

        /**
         * Constructor of the class. Initialices the AID of the crawlers and sets
         * the number of crawlers in the agent
         *
         * @param a
         */
        public QueryHandler(Agent a) {
            super(a);
            receivers = new LinkedList<AID>();
            receivers.add(new AID("casadelibro", AID.ISLOCALNAME));
            receivers.add(new AID("uclm", AID.ISLOCALNAME));
            receivers.add(new AID("amazon", AID.ISLOCALNAME));
            receiversLeft = receivers.size();
        }

        /**
         * This function creates a message per crawler, and creates the Behaviour
         * that will process the interaction with the crawler
         */
        public void action() {
            ACLMessage msg = receive(template);
            if (msg != null) {
                String query = msg.getContent();
                for (AID receiver : receivers) {
                    ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
                    message.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
                    message.setContent(query);
                    message.addReceiver(receiver);
                    addBehaviour(new QuerySender(myAgent, message));
                }
                addBehaviour(new CheckerAgent(myAgent));
            }
        }

        public boolean done() {
            return false;
        }
    }

    /**
     * Behaviour that process the interaction for the crawler.
     */
    private class QuerySender extends AchieveREInitiator {
        /**
         * Constructor of the class
         *
         * @param a
         * @param message
         */
        public QuerySender(Agent a, ACLMessage message) {
            super(a, message);
        }

        /**
         * Receives the agree reply from the crawler
         *
         * @param agree
         */
        protected void handleAgree(ACLMessage agree) {
            System.out.println("Query Accepted from " + agree.getSender().getLocalName()
                    + ". Waiting for response");

            System.out.println("Receivers Left:" + receiversLeft);
        }

        /**
         * The crawler informs that the query couldn't be search or the query has no results.
         *
         * @param refuse
         */
        protected void handleRefuse(ACLMessage refuse) {
            System.out.println("Query Refused from " + refuse.getSender().getLocalName());
            receiversLeft--;
            System.out.println("Receivers Left:" + receiversLeft);
        }

        /**
         * The crawler has not understood the message.
         *
         * @param notUnderstood
         */
        protected void handleNotUnderstood(ACLMessage notUnderstood) {
            System.out.printf("Not understood from " + notUnderstood.getSender().getLocalName());
            receiversLeft--;

            System.out.println("Receivers Left:" + receiversLeft);
        }

        /**
         * The crawler has processes correctly the request. This function extracts the books from the
         * message and adds to the Agent all the books extracted
         *
         * @param inform
         */
        protected void handleInform(ACLMessage inform) {
            System.out.println("Books received from " + inform.getSender().getLocalName()
                    + ".trying to decode the books");
            LinkedList<Book> books;

            receiversLeft--;

            System.out.println("Receivers Left:" + receiversLeft);
            try {
                books = (LinkedList<Book>) inform.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
                return;
            }

            booksAdquired.addAll(books);
        }

        /**
         * The crawler could't do the query so it cannot be processed.
         *
         * @param fallo
         */
        protected void handleFailure(ACLMessage fallo) {
            System.out.println(this.myAgent.getLocalName() + ": Se ha producido un fallo.");
            receiversLeft--;

            System.out.println("Receivers Left:" + receiversLeft);
        }
    }

    /**
     * This Behaviour checks if the query has been done. If it has been done,
     * sends the books to the Agent responsible to choose the best books for the client
     */
    private class CheckerAgent extends Behaviour {
        /**
         * Constructor of the class
         *
         * @param a
         */
        public CheckerAgent(Agent a) {
            super(a);
        }

        /**
         * Inherited from Behaviour.
         * This behaviour does not need to do nothing inside this function
         */
        public void action() {

        }

        /**
         * Checks if all the crawlers have finished their work and informs to their behaviours
         *
         * @return
         */
        public boolean done() {
            return receiversLeft == 0;
        }

        /**
         * Sends the message to the Agent responsible to choose the best books before dying
         *
         * @return
         */
        public int onEnd() {
            System.out.println("Number of books decoded: " + booksAdquired.size());
            ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);

            try {
                msg.setContentObject(booksAdquired);
            } catch (IOException e) {
                e.printStackTrace();
            }
            msg.addReceiver(new AID("chooser", AID.ISLOCALNAME));
            send(msg);

            return 0;
        }
    }
}
