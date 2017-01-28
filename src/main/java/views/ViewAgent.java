package views;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import models.Book;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

public class ViewAgent extends Agent {
    LinkedList<Book> shopBooks, libraryBooks;
    Scanner scanner;

    public void setup() {
        scanner = new Scanner(System.in);
        scanner.useLocale(Locale.US);
        ViewBehaviour view = new ViewBehaviour(this);
        addBehaviour(view);
        view.block(5000);
    }

    private class ViewBehaviour extends OneShotBehaviour {
        public ViewBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            greetings();
            String query = scanner.nextLine();

            ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
            msg.addReceiver(new AID("sender", AID.ISLOCALNAME));
            msg.setContent(query);
            send(msg);
            addBehaviour(new ShopReceiver(myAgent));
            addBehaviour(new LibraryReceiver(myAgent));

        }

        private void greetings() {
            System.out.println("//////////////////////////////////////////////////////////////////");
            System.out.println("Hello, I'm BookAI, and I'm here to find the best books for you.\n " +
                    "Please,  Introduce your query.");
        }
    }

    private class ShopReceiver extends Behaviour {
        MessageTemplate template;

        public ShopReceiver(Agent a) {
            super(a);
            template = MessageTemplate.and(
                    MessageTemplate.MatchSender(new AID("chooser", AID.ISLOCALNAME)),
                    MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM));
        }

        public void action() {
            ACLMessage msg = blockingReceive(template);
            if (msg != null) {
                LinkedList<Book> books;
                try {
                    books = (LinkedList<Book>) msg.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                    return;
                }
                System.out.println("\nBooks received. Here you have the information of the books");
                for (Book book : books) {
                    System.out.println(book.prettyPrint());
                }
            }
        }

        public boolean done() {
            return false;
        }
    }

    private class LibraryReceiver extends Behaviour {
        MessageTemplate template;

        public LibraryReceiver(Agent a) {
            super(a);
            template = MessageTemplate.and(
                    MessageTemplate.MatchSender(new AID("chooser", AID.ISLOCALNAME)),
                    MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
        }

        public void action() {
            ACLMessage msg = receive(template);
            if (msg != null) {
                LinkedList<Book> books;
                try {
                    books = (LinkedList<Book>) msg.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                    return;
                }
                System.out.println("Books received. Here you have the information of the books from the library");
                for (Book book : books) {
                    System.out.println(book.prettyPrint());
                }
            }
        }

        public boolean done() {
            return false;
        }
    }
}
