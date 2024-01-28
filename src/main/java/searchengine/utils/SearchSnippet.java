package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.service.SearchServiceImpl;

import java.io.IOException;
import java.util.*;

@Component
public class SearchSnippet {
    @Autowired
    SearchServiceImpl searchServiceImpl;
    private StringBuilder snippet;
    private String text;
    private int lengthSnippet;
    private final int[] ratioSnippet = {250, 170, 120, 120, 70, 70};
    private final LuceneMorphology luceneMorphRus = new RussianLuceneMorphology();

    public SearchSnippet() throws IOException {
    }

    public String getSnippet(String[] queryArray, String content) {
        searchingSnippet(queryArray, content);
        return snippet.toString();
    }

    private void searchingSnippet(String[] queryArray, String content){
        snippet = new StringBuilder();
        lengthSnippet = ratioSnippet[queryArray.length];
        text = Jsoup.parse(content).text();
        String query = String.join(" ", queryArray);
        if (text.toLowerCase().contains(query)){
            creatingSnippet(query);
        } else {
            comparingLemmasQueryAndText();
        }
    }

    private void creatingSnippet(String query) {
        int start = text.toLowerCase(Locale.ROOT).indexOf(query);
        String textSnippet = text.substring(start, Math.min(start + lengthSnippet, text.length()));
        snippet.append("...<b>").append(textSnippet, 0, query.length()).append("</b>")
                .append(textSnippet.substring(query.length())).append("... ");
    }

    private void comparingLemmasQueryAndText() {

        List<String> lemmas = new ArrayList<>(searchServiceImpl.lemmaListOfQuery);
        String[] rusWords = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яёЁ\\s])", " ")
                .trim()
                .split("\\s+");
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
                        creatingSnippet(rusWord);
                        if (lemmas.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }
    }
}