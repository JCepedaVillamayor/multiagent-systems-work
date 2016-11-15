package crawlers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import jade.core.Agent;
import jade.core.behaviours.*;

import java.io.IOException;
import java.util.LinkedList;

public class BibliotecaUCLMSpider extends Agent {
    private LinkedList<String> links;
    private String query;
    ThreadedBehaviourFactory tbf;
    //TODO: No results error control

    /**
     * Setup the agent
     */
    public void setup() {
        this.links = new LinkedList<String>();
        query = "hola";
        tbf = new ThreadedBehaviourFactory();
        addBehaviour(new SearchBehaviour(this));
    }

    private class SearchBehaviour extends OneShotBehaviour {
        /**
         * Constructor of the class
         *
         * @param a the Agent that has invoked the class
         */
        public SearchBehaviour(BibliotecaUCLMSpider a) {
            super(a);
        }

        /**
         * This function is inherited from Behaviour class
         * Search the query
         */
        public void action() {
            search(query);
        }

        /**
         * Obtain the main page and search the books
         *
         * @param query This is the query to search
         * @return true if the search is successful or false if not
         */
        private boolean search(String query) {
            Document resultPage;

            try {
                resultPage = getResultPage(query);
                getBooks(resultPage);
            } catch (CannotSearchException e) {
                return false;
            }

            return true;
        }

        /**
         * Search in bibliotecauclm the the query and obtain the result page, going through a redirect page
         *
         * @param query the query to search
         * @return The Result page
         * @throws CannotSearchException
         */
        private Document getResultPage(String query) throws CannotSearchException {
            Document nextWeb, mainWeb, resultWeb;
            mainWeb = getWeb();
            nextWeb = fillFormAndObtainPage(mainWeb, query);
            String endPoint = getRedirectEndpoint(nextWeb);

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
        private String getRedirectEndpoint(Document baseWeb) {
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
         * Extract the book links from the web page and creates a thread
         * per link to obtain the information about the book
         *
         * @param resultPage The web where the book links are available
         */
        private void getBooks(Document resultPage) {
            LinkedList<String> linked = new LinkedList();
            String baseUri = resultPage.baseUri().split("ACC")[0].replace("?", "");
            Elements books = resultPage.getElementsByClass("coverlist");

            for (Element book : books) {
                Element bookUri = book.getElementsByTag("a").get(1);
                String link = bookUri.attr("href");
                linked.add(baseUri + link);
            }

            for (String link : linked) {
                addBehaviour(tbf.wrap(new PageBehaviour(link)));
            }
        }
    }

    private class PageBehaviour extends OneShotBehaviour {
        private String link;

        /**
         * Constructor of the class
         *
         * @param link the book link to obtain the information
         */
        public PageBehaviour(String link) {
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
            synchronized (links) {
                links.add(doc.text());
            }
            System.out.println(link);
        }
    }
}
