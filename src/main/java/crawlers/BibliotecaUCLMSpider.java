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

    public void setup() {
        this.links = new LinkedList<String>();
        query = "hola";
        ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
        addBehaviour(new SearchBehaviour());
    }

    private class SearchBehaviour extends OneShotBehaviour {
        public void action() {
            search(query);
        }

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

        private void getBooks(Document resultPage) {
            LinkedList<String> linked = new LinkedList();
            String baseUri = resultPage.baseUri().split("ACC")[0].replace("?", "");
            Elements books = resultPage.getElementsByClass("coverlist");
            for (Element book : books) {
                Element bookUri = book.getElementsByTag("a").get(1);
                String link = bookUri.attr("href");
                linked.add(link);
            }

            for (String link : linked) {
                try {
                    System.out.println(Jsoup.connect(baseUri + link).timeout(5000).get().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            System.out.println(baseUri);
        }

        private String getRedirectEndpoint(Document baseWeb) {
            Element refreshElement = baseWeb.getElementsByAttributeValue("http-equiv", "Refresh").first();
            String rawEndpoint = refreshElement.attr("content");
            String endPoint = rawEndpoint.split("URL=")[1];
            System.out.println(endPoint);
            return endPoint;
        }

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
    }
    //TODO: Extract the information of the book


}
