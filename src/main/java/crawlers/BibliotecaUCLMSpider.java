package crawlers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;

import java.io.IOException;
import java.util.LinkedList;

public class BibliotecaUCLMSpider {
    private LinkedList<String> links;

    public BibliotecaUCLMSpider() {
        this.links = new LinkedList<String>();
    }

    public boolean search(String query) {
        Document resultPage;

        try {
            resultPage = getResultPage(query);
            System.out.println(resultPage);
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
            ;
        } catch (IOException e) {
            throw new CannotSearchException("Cannot connect to web");
        }

        return resp;
    }
}
