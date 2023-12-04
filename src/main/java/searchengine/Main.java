package searchengine;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Main {
    public static void main(String[] args){
        String link = "http://radiomv.ru";
        try {
            Connection.Response response = Jsoup.connect(link).execute();
            Document document = Jsoup.connect(link)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                    .header("referrer", "http://www.google.com")
                    .get();

            Elements elements = document.select("a[href]");
            elements.forEach(element -> {
                String href = element.attr("abs:href");
                System.out.println(href);
            });
        } catch (HttpStatusException e) {
            System.out.println(e.getStatusCode());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
