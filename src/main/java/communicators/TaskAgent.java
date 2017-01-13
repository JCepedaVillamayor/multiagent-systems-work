package communicators;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;
import models.Book;

import java.util.LinkedList;

/**
 * Created by jcepeda on 12/01/17.
 */
public class TaskAgent extends Agent {
    public void setup() {
        addBehaviour(new QueryHandler(this));

    }

    private class QueryHandler extends OneShotBehaviour {
        LinkedList<AID> receivers;

        public QueryHandler(Agent a) {
            super(a);
            receivers = new LinkedList<AID>();
            receivers.add(new AID("casadelibro", AID.ISLOCALNAME));
            receivers.add(new AID("uclm", AID.ISLOCALNAME));
        }

        public void action() {
            for (AID receiver : receivers) {
                ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
                message.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
                message.setContent("Risto");
                message.addReceiver(receiver);
                addBehaviour(new QuerySender(myAgent, message));
            }
        }
    }

    private class QuerySender extends AchieveREInitiator {
        public QuerySender(Agent a, ACLMessage message) {
            super(a, message);
        }

        protected void handleAgree(ACLMessage agree) {
            System.out.println("Query Accepted. Waiting for response");
        }

        protected void handleRefuse(ACLMessage refuse) {
            System.out.println("Query Refused.");
        }

        protected void handleNotUnderstood(ACLMessage notUnderstood) {
            System.out.printf("Not understood");
        }

        /**
         * @param inform
         */
        protected void handleInform(ACLMessage inform) {
            System.out.println("Books received. trying to decode the books");
            LinkedList<Book> books;

            try {
                books = (LinkedList<Book>) inform.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
                return;
            }
            for (Book book : books) {
                System.out.println(book);
            }
        }

        protected void handleFailure(ACLMessage fallo) {
            System.out.println(this.myAgent.getLocalName() + ": Se ha producido un fallo.");
        }
    }
}
