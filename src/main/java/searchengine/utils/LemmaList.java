package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class LemmaList {
    private String[] rusWords;
    private String[] engWords;
    private List<String> rusLemmas;
    private List<String> engLemmas;
    private final LuceneMorphology luceneMorphRus = new RussianLuceneMorphology();
    private final LuceneMorphology luceneMorphEng = new EnglishLuceneMorphology();

    public LemmaList() throws IOException {
    }

    public List<String> getLemmaList(String content) {

            List<String> lemmaList = new ArrayList<>();
            if (!content.equals("")) {
                rusLemmas = new ArrayList<>();
                engLemmas = new ArrayList<>();
                conversionContentToWords(content);
                for (String w : rusWords) {
                    String lemma = getRusLemma(w);
                    if (lemma != null) {
                        rusLemmas.add(lemma);
                    }
                }
                for (String w : engWords) {
                    String lemma = getEngLemma(w);
                    if (lemma != null) {
                        engLemmas.add(lemma);
                    }
                }
                lemmaList = combiningLemmaLists();
            }
            return lemmaList;
        }

        private void conversionContentToWords(String content) {

            String text = Jsoup.parse(content).text().toLowerCase(Locale.ROOT);
            rusWords = text.replaceAll("([^а-яёЁ\\s])", " ").trim().split("\\s+");
            engWords = text.replaceAll("([^a-z\\s])", " ").trim().split("\\s+");
        }

        private String getRusLemma(String rusWord) {
            if (rusWord.length() > 1) {
                List<String> morphInfo = luceneMorphRus.getMorphInfo(rusWord);
                String word = morphInfo.get(0);
                if (word.contains(" С") || word.contains(" П") || word.contains(" ПРИЧАСТИЕ") ||
                        word.contains(" МС") || word.contains(" Г") || word.contains(" КР") || word.contains(" Н")
                        || word.contains(" ИНФИНИТИВ") || word.contains(" ДЕЕПРИЧАСТИЕ") || word.contains(" ЧИСЛ")) {
                    List<String> normalForms = luceneMorphRus.getNormalForms(rusWord);
                    return normalForms.get(0);
                }
            }
            return null;
        }

        private String getEngLemma(String engWord) {
            if (!engWord.equals("")) {
                List<String> morphInfo = luceneMorphEng.getMorphInfo(engWord);
                String word = morphInfo.get(0);
                if (word.contains(" NOUN") || word.contains(" VERB") || word.contains(" ADJECTIVE") ||
                        word.contains(" ADVERB") || word.contains(" NUMERAL") || word.contains(" MOD") ||
                        word.contains(" PN") || word.contains(" VBE")) {
                    List<String> normalForms = luceneMorphEng.getNormalForms(engWord);
                    return normalForms.get(0);
                }
            }
            return null;
        }

        private List<String> combiningLemmaLists() {
            List<String> lemmasListAll = new ArrayList<>();
            if (!rusLemmas.isEmpty()) {
                lemmasListAll.addAll(rusLemmas);
            }
            if (!engLemmas.isEmpty()) {
                lemmasListAll.addAll(engLemmas);
            }
            return lemmasListAll;
        }
}
