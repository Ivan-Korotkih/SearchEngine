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
    private final int[] ratioSnippet = {25, 20, 15, 15, 10, 10, 5, 5, 5, 5};

    public String distributionOfWorks(String[] queryArray, String content) {

        snippet = new StringBuilder();
        String text = Jsoup.parse(content).text();
        fastSearchSnippet(queryArray, text);
        return snippet.toString();
    }

    public void fastSearchSnippet(String[] queryArray, String text) {

        String query = String.join(" ", queryArray);

        if (text.contains(query)) {
            int start = text.indexOf(query);
            String snippetBold = text.substring(start, Math.min(start + 190, text.length()))
                                     .replaceAll(query, "<b>" + query + "</b>");
            snippet.append(snippetBold);
        } else {
            lengthSnippet = ratioSnippet[queryArray.length];
            List<String> queryList = new ArrayList<>(List.of(queryArray));
            String[] textArray = text.split("\\s+");
            lemmas = new ArrayList<>(checkingSearchQuery.lemmaListOfQuery);
            slowSearchSnippet(queryList, textArray, text);
        }
    }

    public void slowSearchSnippet(List<String> queryList, String[] contentArray, String text) {

        int counter = 0;
        int position = -1;
        for (int i = 0; i < contentArray.length; i++) {
            String w = contentArray[i].toLowerCase(Locale.ROOT).replaceAll("([^а-яa-z\\s])", "");
            if (queryList.contains(w)) {
                counter = lengthSnippet;
                position = i;
                queryList.remove(w);
                lemmas.remove(w);
            }
            if (counter > 0) {
                snippet.append(position == i ? "<b>" + contentArray[i] + "</b>" : contentArray[i]).append(" ");
                counter--;
            }
        }
        if (queryList.size() != 0) {
            List<String> lemmasCopy = new ArrayList<>(comparisonLemmasQueryAndText(text));
            slowSearchSnippet(lemmasCopy, contentArray, text);
        }
    }

    public HashSet<String> comparisonLemmasQueryAndText(String text) {

        String[] rusWords = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");

        HashSet<String> lemmasCopy = new HashSet<>();

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
                            if (!rusWord.equals(normalForms.get(0))) {
                                lemmasCopy.add(rusWord);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return lemmasCopy;
    }
}