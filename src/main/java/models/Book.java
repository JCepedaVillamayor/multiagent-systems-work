package models;

import java.io.Serializable;

/**
 * Cre+ated by jcepeda on 29/12/16.
 */
public class Book implements Serializable {
    private String url, source, title, author;
    private double price;
    private String type;

    public Book() {

    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int compareTo(Book other) {
        if (price < other.getPrice()) {
            return -1;
        } else if (price == other.getPrice()) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return "Book{" +
                "\nurl='" + url + '\'' +
                ",\nsource='" + source + '\'' +
                ",\ntitle='" + title + '\'' +
                ",\nauthor='" + author + '\'' +
                ",\nprice=" + price +
                ",\ntype='" + type + '\'' +
                "}\n";
    }

    public String prettyPrint() {
        String str = "\n";
        str += "Title: " + title + "\n";
        str += "Author: " + author + ". Source: " + source + "\n";
        str += "URL: " + url + "\n";
        return str;
    }
}
