package searchengine.component;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class SearchSnippet {
    @Autowired
    CheckingSearchQuery checkingSearchQuery;
    List<String> lemmas;
    StringBuilder snippet;
    private int lengthSnippet;
    private final int[] ratioSnippet = {250, 170, 120, 120, 70, 70};

    public String distributionOfWorks(String[] queryArray, String content) {

        snippet = new StringBuilder();
        lengthSnippet = ratioSnippet[queryArray.length];
        String text = Jsoup.parse(content).text();
        fastSearchSnippet(queryArray, text);
        return snippet.toString();
    }

    public void fastSearchSnippet(String[] queryArray, String text) {

        String query = String.join(" ", queryArray);
        if (text.toLowerCase().contains(query)) {
            formingSnippet(query, text);
        } else {
            lemmas = new ArrayList<>(checkingSearchQuery.lemmaListOfQuery);
            comparisonLemmasQueryAndText(text);
        }
    }

    public void formingSnippet(String query, String text) {

        int start = text.toLowerCase(Locale.ROOT).indexOf(query);
        String textSnippet = text.substring(start, Math.min(start + lengthSnippet, text.length()));
        snippet.append("...<b>").append(textSnippet, 0, query.length()).append("</b>")
                .append(textSnippet.substring(query.length())).append("... ");
    }

    public void comparisonLemmasQueryAndText(String text) {

        String[] rusWords = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s");
        try {
            LuceneMorphology luceneMorphRus = new RussianLuceneMorphology();

            for (String rusWord : rusWords) {
                if (rusWord.length() > 1) {
                    List<String> morphInfo = luceneMorphRus.getMorphInfo(rusWord);
                    String word = morphInfo.get(0);
                    if (word.contains(" С") || word.contains(" П") || word.contains(" ПРИЧАСТИЕ") ||
                            word.contains(" МС") || word.contains(" Г") || word.contains(" КР") || word.contains(" Н")
                            || word.contains(" ИНФИНИТИВ") || word.contains(" ДЕЕПРИЧАСТИЕ") || word.contains(" ЧИСЛ")) {
                        List<String> normalForms = luceneMorphRus.getNormalForms(rusWord);
                        if (lemmas.contains(normalForms.get(0))) {
                            lemmas.remove(normalForms.get(0));
                            formingSnippet(rusWord, text);
                            if (lemmas.isEmpty()) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}