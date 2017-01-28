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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.Map;

public class AmazonSpider extends Agent {
    MessageTemplate senderTemplate = MessageTemplate.MatchSender(new AID("sender", AID.ISLOCALNAME));
    MessageTemplate protocolTemplate = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
    MessageTemplate template = MessageTemplate.and(senderTemplate, protocolTemplate);

    /**
     * Initializes the Behaviour that search in Amazon.es
     */
    public void setup() {
        addBehaviour(new SearchBehaviour(this, template));
    }

    /**
     * This class search in Amazon.es for books
     */
    private class SearchBehaviour extends AchieveREResponder {
        private LinkedList<Book> books;
        private Elements bookLinks;

        /**
         * Constructor of the class
         *
         * @param a
         * @param template
         */
        private SearchBehaviour(Agent a, MessageTemplate template) {
            super(a, template);
        }

        /**
         * This function do the query to Amazon.es and looks if there are results for the query given
         *
         * @param request the message that the Behaviour receives
         * @return the reply: AGREE if the query has been successful or REFUSE if not.
         * @throws NotUnderstoodException
         * @throws RefuseException        this exception is throwed if Amazon.es cannot be reached
         *                                or if the query has no results
         */
        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            String query;

            try {
                query = URLEncoder.encode(request.getContent(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RefuseException("cannot resolve query");
            }

            try {
                searchInAmazon(query);
            } catch (CannotSearchException e) {
                e.printStackTrace();
                throw new RefuseException("The web cannot be reached");
            } catch (NotFoundException e) {
                e.printStackTrace();
                throw new RefuseException("The query does not have results");
            }

            ACLMessage agree = request.createReply();
            agree.setPerformative(ACLMessage.AGREE);

            return agree;
        }

        /**
         * This function is responsible to search in Amazon.es to find the result for the query
         *
         * @param query the query to search
         * @throws CannotSearchException if Amazon.es cannot be reached
         * @throws NotFoundException     if there are no results for the query
         */
        private void searchInAmazon(String query) throws CannotSearchException, NotFoundException {
            Document resultPage;

            try {
                resultPage = amazonSearch(query);
            } catch (CannotSearchException e) {
                e.printStackTrace();
                throw new CannotSearchException("The web cannot be reached");
            }

            if (resultPage.getElementById("noResultsTitle") != null) {
                throw new NotFoundException("There are no results");
            } else if (resultPage.getElementsByClass("s-item-container").size() == 0) {
                throw new NotFoundException("There are no results");

            }

            bookLinks = resultPage.getElementsByClass("s-item-container");
        }

        private Document amazonSearch(String query) throws CannotSearchException {
            Connection con;
            Document mainPage, resultPage;
            Map<String, String> cookies;
            con = Jsoup.connect(Constants.AMAZON_URL)
                    .userAgent(Constants.USER_AGENT)
                    .timeout(10000);
            cookies = con.response().cookies();
            try {
                mainPage = con.get();
            } catch (IOException e) {
                throw new CannotSearchException("The web cannot be reached");
            }
            FormElement form;
            try {
                form = (FormElement) mainPage.getElementsByClass("nav-searchbar").get(0);
            } catch (IndexOutOfBoundsException e) {
                throw new CannotSearchException("the form couldn't be obtained");
            }
            Element queryText = form.getElementById("twotabsearchtextbox");
            queryText.val(query);

            try {
                Connection conn = form.submit().userAgent(Constants.USER_AGENT).timeout(10000);
                cookies.putAll(conn.response().cookies());
                resultPage = conn.get();
            } catch (IOException e) {
                throw new CannotSearchException("The web cannot be reached");
            }


            return resultPage;
        }

        /**
         * Extract the information from the raw html
         *
         * @throws CannotSearchException
         */
        private void extractbookLinks() throws CannotSearchException {

            books = new LinkedList<Book>();
            for (Element link : bookLinks) {
                try {
                    Book book = new Book();
                    book.setPrice(obtainPriceFromLink(link));
                    book.setTitle(obtainTitleFromLink(link));
                    book.setUrl(obtainUrlFromLink(link));
                    book.setAuthor(obtainAuthorFromBookLink(book.getUrl()));
                    book.setType(Types.SELL_BOOK);
                    book.setSource(Constants.AMAZON_SOURCE);
                    books.add(book);
                } catch (Exception e) {
                }
            }
        }

        /**
         * Due to the difficult to extract the author from the result page, it is needed to go
         * into the specific book result link to find the Author. This function acces to the page and
         * extracts the author
         *
         * @param url the url to access
         * @return the name of the author
         * @throws CannotSearchException if the web cannot be reached
         */
        private String obtainAuthorFromBookLink(String url) throws CannotSearchException {
            Document authorWeb;
            try {
                authorWeb = Jsoup.connect(url)
                        .timeout(10000)
                        .userAgent(Constants.USER_AGENT).get();
            } catch (IOException e) {
                throw new CannotSearchException("Cannot connect to the web");
            }
            Element author = authorWeb.select("#byline > span > a").get(0);
            return author.text();
        }

        private String obtainUrlFromLink(Element link) {
            Element url = link.getElementsByClass("a-link-normal").get(0);
            return url.attr("href");

        }

        private String obtainTitleFromLink(Element link) {
            return link.getElementsByClass("s-access-title").get(0).text();
        }

        private double obtainPriceFromLink(Element link)
                throws IndexOutOfBoundsException, NumberFormatException {
            String rawPrice = link.getElementsByClass("s-price").get(0).text();
            rawPrice = rawPrice.replace("EUR ", "");
            rawPrice = rawPrice.replace(",", ".");
            double price;
            Double.parseDouble(rawPrice);
            return Double.parseDouble(rawPrice);
        }

        /**
         * When the behaviour has send the AGREE reply, it prepares the information to send.
         * This behaviour extracts the information from the book result page, converts to Book object, serialize the
         * books and replies to the requests.
         *
         * @param request
         * @param response
         * @return the reply to the message
         * @throws FailureException if the books cannot be serialized
         */
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage inform = request.createReply();

            try {
                extractbookLinks();
            } catch (CannotSearchException e) {
                throw new FailureException("Error accessing to the page to extract information about the book");
            }

            try {
                inform.setContentObject(books);
            } catch (IOException e) {
                e.printStackTrace();
                throw new FailureException("la que se ha liao Paco");
            }

            inform.setPerformative(ACLMessage.INFORM);
            return inform;
        }
    }
}
