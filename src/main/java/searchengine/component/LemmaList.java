package searchengine.component;

import lombok.Getter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@Getter
public class LemmaList {
    private String[] rusWords;
    private String[] engWords;
    private final List<String> rusLemmas = new ArrayList<>();
    private final List<String> engLemmas = new ArrayList<>();
    private final List<String> lemmasListAll = new ArrayList<>();

    public List<String> distributeTheWork(String content) {

        if (!content.equals("")) {
            conversionContentToWords(content);
            rusLemmas.addAll(createRusLemmaList());
            engLemmas.addAll(createEngLemmaList());
            combiningLemmaLists();
        }
        return getLemmasListAll();
    }

    public void conversionContentToWords(String content) {

        String text = Jsoup.parse(content).text();
        rusWords = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
        engWords = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^a-z\\s])", " ")
                .trim()
                .split("\\s+");
    }

    public List<String> createRusLemmaList() {

        List<String> lemmaList = new ArrayList<>();
        try {
            LuceneMorphology luceneMorphRus = new RussianLuceneMorphology();

            for (String rusWord : getRusWords()) {
                if (rusWord.length() > 1) {
                    List<String> morphInfo = luceneMorphRus.getMorphInfo(rusWord);
                    String word = morphInfo.get(0);
                    if (word.contains(" С") || word.contains(" П") || word.contains(" ПРИЧАСТИЕ") ||
                            word.contains(" МС") || word.contains(" Г") || word.contains(" КР") || word.contains(" Н")
                            || word.contains(" ИНФИНИТИВ") || word.contains(" ДЕЕПРИЧАСТИЕ") || word.contains(" ЧИСЛ")){
                        List<String> normalForms = luceneMorphRus.getNormalForms(rusWord);
                        lemmaList.add(normalForms.get(0));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return lemmaList;
    }

    public List<String> createEngLemmaList() {

        List<String> lemmaList = new ArrayList<>();
        try {
            LuceneMorphology luceneMorphEng = new EnglishLuceneMorphology();

            for (String engWord : getEngWords()) {
                if (!engWord.equals("")) {
                    List<String> morphInfo = luceneMorphEng.getMorphInfo(engWord);
                    String word = morphInfo.get(0);
                    if (word.contains(" NOUN") || word.contains(" VERB") || word.contains(" ADJECTIVE") ||
                            word.contains(" ADVERB") || word.contains(" NUMERAL") || word.contains(" MOD") ||
                            word.contains(" PN") || word.contains(" VBE")) {
                        List<String> normalForms = luceneMorphEng.getNormalForms(engWord);
                        lemmaList.add(normalForms.get(0));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return lemmaList;
    }

    public void combiningLemmaLists() {
        if (rusLemmas.size() != 0) {
            lemmasListAll.addAll(rusLemmas);
        }
        if (engLemmas.size() != 0) {
            lemmasListAll.addAll(engLemmas);
        }
    }
}
