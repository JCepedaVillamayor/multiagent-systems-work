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
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import utils.Constants;
import utils.Types;

import java.io.IOException;
import java.util.LinkedList;

public class BibliotecaUCLMSpider extends Agent {
    MessageTemplate senderTemplate = MessageTemplate.MatchSender(new AID("sender", AID.ISLOCALNAME));
    MessageTemplate protocolTemplate = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
    MessageTemplate template = MessageTemplate.and(senderTemplate, protocolTemplate);

    /**
     * Setup the agent
     */
    public void setup() {
        addBehaviour(new SearchBehaviour(this, template));
    }

    private class SearchBehaviour extends AchieveREResponder {
        private LinkedList<String> bookLinks;
        private LinkedList<Book> books;

        /**
         * Class constructor
         *
         * @param a
         * @param template
         */
        public SearchBehaviour(Agent a, MessageTemplate template) {
            super(a, template);
            books = new LinkedList<Book>();
            bookLinks = new LinkedList<String>();
        }

        /**
         * This function extracts the query and search it into UCLM library.
         *
         * @param request
         * @return
         * @throws NotUnderstoodException
         * @throws RefuseException
         */

        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            Document resultPage;

            try {
                resultPage = getResultPage(request.getContent());
            } catch (CannotSearchException e) {
                throw new RefuseException("Cannot contact to the web");
            }
            try {
                getBookLinks(resultPage);
            } catch (NotFoundException e) {
                throw new RefuseException("The query does not have results");
            }

            ACLMessage agree = request.createReply();
            agree.setPerformative(ACLMessage.AGREE);

            return agree;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            extractBooksFromLinks();
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);

            try {
                inform.setContentObject(books);
            } catch (IOException e) {
                e.printStackTrace();
                throw new FailureException("Error passing the objects to message");
            }

            return inform;
        }

        private void extractBooksFromLinks() {
            for (String link : bookLinks) {
                obtainBookInfo(link);
            }
        }

        /**
         * Navigate throug the main page of UCLM library, then
         * obtain the result url and obtain the page where the results of the queries
         * are stored
         *
         * @param query the query to search
         * @return The Result page
         * @throws CannotSearchException
         */
        private Document getResultPage(String query) throws CannotSearchException {
            Document nextWeb, mainWeb, resultWeb;
            mainWeb = getWeb();
            nextWeb = fillFormAndObtainPage(mainWeb, query);
            String endPoint = getRedirectQuery(nextWeb);

            try {
                resultWeb = Jsoup.connect(Constants.BIBLIO_CATALOG + endPoint)
                        .userAgent(Constants.USER_AGENT)
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
                throw new CannotSearchException("The url is invalid");
            }

            return resultWeb;
        }

        /**
         * Obtain the meta attribute Refresh, which has the result page given a query
         *
         * @param baseWeb the web with the attribute Refresh
         * @return The result url
         */
        private String getRedirectQuery(Document baseWeb) {
            Element refreshElement = baseWeb.getElementsByAttributeValue("http-equiv", "Refresh").first();
            String rawEndpoint = refreshElement.attr("content");
            String endPoint = rawEndpoint.split("URL=")[1];
            System.out.println(endPoint);
            return endPoint;
        }

        /**
         * Fill the form given a query and obtain the page that has the
         * meta attribute which redirect to the result page
         *
         * @param mainWeb the base web that has the form
         * @param query   the query to search
         * @return the web with the attribute refres
         * @throws CannotSearchException
         */
        private Document fillFormAndObtainPage(Document mainWeb, String query) throws CannotSearchException {
            Document nextWeb;

            FormElement searchForm = (FormElement) mainWeb.getElementById("abnform");
            Element queryText = searchForm.getElementById("labelBusquedaCatalogoP");
            queryText.val(query);
            try {
                nextWeb = searchForm.submit().get();
            } catch (IOException e) {
                throw new CannotSearchException("The web cannot be reached");
            }

            return nextWeb;
        }

        /**
         * Obtain the main web
         *
         * @return the main web
         * @throws CannotSearchException
         */
        private Document getWeb() throws CannotSearchException {
            Document resp;

            try {
                resp = Jsoup.connect(Constants.BIBLIO_URL)
                        .userAgent(Constants.USER_AGENT)
                        .method(Connection.Method.GET)
                        .get();
            } catch (IOException e) {
                throw new CannotSearchException("Cannot connect to web");
            }

            return resp;
        }

        /**
         * Extract the book bookLinks from the web page and creates a thread
         * per link to obtain the information about the book
         *
         * @param resultPage The web where the book bookLinks are available
         */
        private void getBookLinks(Document resultPage) throws NotFoundException {
            bookLinks = new LinkedList();
            Elements noResult = resultPage.getElementsByClass("nores");

            if (noResult.size() > 1) {
                throw new NotFoundException();
            }

            String baseUri = resultPage.baseUri().split("ACC")[0].replace("?", "");
            Elements books = resultPage.getElementsByClass("coverlist");

            for (Element book : books) {
                Element bookUri = book.getElementsByTag("a").get(1);
                String link = baseUri + bookUri.attr("href");
                bookLinks.add(link);
            }
        }

        private void obtainBookInfo(String link) {
            Document doc;
            try {
                doc = Jsoup.connect(link).timeout(100000).get();
            } catch (IOException e) {
                System.out.println("The web " + link + " cannot be reached");
                return;
            }
            Book book = new Book();
            book.setAuthor(obtainAuthorFromPage(doc));
            book.setTitle(obtainTitleFromPage(doc));
            book.setType(Types.LIBRARY_BOOK);
            book.setSource(Constants.BIBLIO_SOURCE);
            book.setUrl(link);
            book.setPrice(0.0);

            books.add(book);

        }

        private String obtainTitleFromPage(Document doc) {
            Element htmlTitle = doc.getElementsByClass("titn").get(1);
            return htmlTitle.text();
        }

        public String obtainAuthorFromPage(Document doc) {
            Element htmlTitle = doc.getElementsByClass("titn").get(0);
            return htmlTitle.text();
        }
    }

}