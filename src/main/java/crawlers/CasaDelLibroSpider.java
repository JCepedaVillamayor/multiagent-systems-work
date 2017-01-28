package crawlers;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import models.Book;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.Constants;
import utils.Types;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;

public class CasaDelLibroSpider extends Agent {
    LinkedList<String> links;
    MessageTemplate senderTemplate = MessageTemplate.MatchSender(new AID("sender", AID.ISLOCALNAME));
    MessageTemplate protocolTemplate = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
    MessageTemplate template = MessageTemplate.and(senderTemplate, protocolTemplate);


    public void setup() {
        addBehaviour(new SearchBehaviour(this, template));
    }

    private class SearchBehaviour extends AchieveREResponder {
        private String query;
        private Elements rawBooks;

        /**
         * Constructor of the class
         *
         * @param a        the agent that has created the behaviour
         * @param template the template that filters the messages received
         */
        public SearchBehaviour(Agent a, MessageTemplate template) {
            super(a, template);
        }


        /**
         * this function tries to resolve the query and search it into CasaDelLibro. If the query is successful,
         * returns AGREE performative. If not, returns REFUSE to the sender
         *
         * @param request the message containing the query to search
         * @return the message agreeing to the query or not
         * @throws NotUnderstoodException
         * @throws RefuseException
         */
        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            String content = request.getContent();

            try {
                this.query = URLEncoder.encode(content, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RefuseException("cannot resolve query");
            }

            try {
                rawBooks = doQueryAndObtainRawBooks();
            } catch (CannotSearchException e) {
                throw new RefuseException("Problem loading page");
            } catch (NotFoundException e) {
                throw new RefuseException("Cannot obtain results from Query");
            }

            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.AGREE);

            return reply;
        }

        /**
         * this function extracts the rawBooks from the page and sends the rawBooks to the sender
         *
         * @param request
         * @param response
         * @return message containing the rawBooks
         * @throws FailureException
         */
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);

            LinkedList<Book> books = obtainBooksFromLinks();

            try {
                inform.setContentObject(books);
            } catch (IOException e) {
                e.printStackTrace();
                throw new FailureException("Error passing the objects to message");
            }

            return inform;
        }

        /**
         * this function extracts the rawBooks from the raw html rawBooks
         *
         * @return the list of books that has been parsed
         */
        private LinkedList<Book> obtainBooksFromLinks() {
            LinkedList<Book> books = new LinkedList<Book>();
            for (Element rawBook : rawBooks) {
                Book book = new Book();
                try {
                    book.setTitle(obtainTitleFromLink(rawBook));
                    book.setPrice(obtainPriceFromLink(rawBook));
                    book.setAuthor(obtainAuthorFromLink(rawBook));
                    book.setUrl(obtainUrlFromLink(rawBook));
                    book.setSource(Constants.CDL_SOURCE);
                    book.setType(Types.SELL_BOOK);
                    books.add(book);
                } catch (Exception e) {
                }
            }
            return books;
        }

        private String obtainUrlFromLink(Element link) {
            Element url = link.getElementsByClass("title-link").get(0);
            return Constants.CASA_DEL_LIBRO_URL + url.attr("href");
        }

        private String obtainAuthorFromLink(Element link) throws IndexOutOfBoundsException {
            Element author = link.getElementsByClass("mod-libros-author").get(0);
            return author.text();
        }

        private double obtainPriceFromLink(Element link){
            Element price = link.getElementsByClass("currentPrice").get(0);
            String bookPrice = price.text().replaceFirst("€", "");
            return Double.parseDouble(bookPrice);
        }

        private String obtainTitleFromLink(Element link) {
            Element title = link.getElementsByClass("title-link").get(0);
            return title.text();
        }

        /**
         * Do the query to CasaDelLibro
         *
         * @return all the bookLinks related to the query
         * @throws CannotSearchException
         * @throws NotFoundException
         */
        private Elements doQueryAndObtainRawBooks() throws CannotSearchException, NotFoundException {
            Document resultPage;
            try {
                resultPage = Jsoup.connect(Constants.CASA_DEL_LIBRO_QUERY_URL + query)
                        .timeout(100000)
                        .userAgent(Constants.USER_AGENT)
                        .get();
            } catch (IOException e) {
                throw new CannotSearchException("Cannot connect to the page");
            }

            // Looks if there aren't results to the query
            if (resultPage.getElementsByClass("title01-adv-busc").size() > 0) {
                throw new NotFoundException();
            }

            Elements links = resultPage.getElementsByClass("mod-list-item");
            return links;
        }
    }
}
