package controllers;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import models.Book;
import utils.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class BookChooser extends Agent {
    MessageTemplate template = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
            MessageTemplate.MatchSender(new AID("sender", AID.ISLOCALNAME)));

    public void setup() {
        addBehaviour(new Chooser(this));
    }

    private class Chooser extends Behaviour {
        List<Book> amazonBooks, cdlBooks;
        LinkedList<Book> books, libraryBooks;

        public Chooser(Agent a) {
            super(a);
        }

        public void action() {
            ACLMessage message = receive(template);
            if (message != null) {
                try {
                    books = (LinkedList<Book>) message.getContentObject();
                } catch (UnreadableException e) {
                    sendEmptyMessage(message);
                    return;
                }
                chooseBooksWisely();
                sendMessagesToViewAgent();
            }
        }

        private void sendMessagesToViewAgent() {
            AID view = new AID("view", AID.ISLOCALNAME);
            addBehaviour(new SimplySender(myAgent, libraryBooks, ACLMessage.CONFIRM, view));
            addBehaviour(new SimplySender(myAgent, mixBooks(), ACLMessage.DISCONFIRM, view));
        }

        private LinkedList<Book> mixBooks() {
            LinkedList<Book> booksToSend = new LinkedList<Book>();
            booksToSend.addAll(amazonBooks);
            booksToSend.addAll(cdlBooks);
            return booksToSend;
        }

        /**
         * In this function the books has been extracted. In this version simply sends
         * the first 5 books for every crawler except for the library, that sends all the books
         *
         * @return
         */
        private void chooseBooksWisely() {
            amazonBooks = new ArrayList<Book>();
            cdlBooks = new ArrayList<Book>();
            libraryBooks = new LinkedList<Book>();

            for (Book book : books) {
                if (book.getSource().equals(Constants.AMAZON_SOURCE)) {
                    amazonBooks.add(book);
                } else if (book.getSource().equals(Constants.CDL_SOURCE)) {
                    cdlBooks.add(book);
                } else {
                    libraryBooks.add(book);
                }
            }
            int maxSizeAmazon = (amazonBooks.size() < 4) ? amazonBooks.size() : 4;
            int maxSizeCDL = (cdlBooks.size() < 4) ? cdlBooks.size() : 4;
            amazonBooks = amazonBooks.subList(0, maxSizeAmazon);
            cdlBooks = cdlBooks.subList(0, maxSizeCDL);
        }

        /**
         * Sends and empty message to the ViewAgent
         *
         * @param message
         */
        private void sendEmptyMessage(ACLMessage message) {
            ACLMessage reply = message.createReply();
            reply.setPerformative(ACLMessage.PROPOSE);
            try {
                reply.setContentObject(new ArrayList<Book>());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            send(reply);
        }


        public boolean done() {
            return false;
        }
    }

    private class SimplySender extends OneShotBehaviour {
        LinkedList<Book> books;
        int performative;
        AID sender;

        public SimplySender(Agent a, LinkedList<Book> books, int performative, AID sender) {
            super(a);
            this.books = books;
            this.performative = performative;
            this.sender = sender;
        }

        public void action() {
            ACLMessage msg = new ACLMessage(performative);
            msg.addReceiver(sender);
            try {
                msg.setContentObject(books);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            send(msg);
        }
    }
}
