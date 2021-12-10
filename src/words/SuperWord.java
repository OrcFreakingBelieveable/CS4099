package words;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import apis.WordsAPI;
import words.Pronunciation.SubPronunciation;
import words.SubWord.PartOfSpeech;

import static utils.NullListOperations.addToNull;
import static utils.NullListOperations.addAllToNull;
import static config.Configuration.LOG;

public class SuperWord implements Comparable<SuperWord> {

    private static HashMap<String, SuperWord> cachePopulated = new HashMap<>();
    private static HashMap<String, SuperWord> cachePlaceholder = new HashMap<>();

    private String plaintext;
    private boolean populated = false; // true iff built from a WordsAPI query
    private Pronunciation pronunciation;
    private Set<PartOfSpeech> partsOfSpeech = new HashSet<>();
    private ArrayList<SubWord> nouns;
    private ArrayList<SubWord> pronouns;
    private ArrayList<SubWord> verbs;
    private ArrayList<SubWord> adjectives;
    private ArrayList<SubWord> adverbs;
    private ArrayList<SubWord> prepositions;
    private ArrayList<SubWord> conjunctions;
    private ArrayList<SubWord> definiteArticles;
    private ArrayList<SubWord> unknowns;

    /**
     * Attempts to get a cached word, before returning a new placeholder.
     * 
     * @param plaintext
     * @return
     */
    public static SuperWord getSuperWord(String plaintext) {
        if (cachePopulated.containsKey(plaintext)) {
            LOG.writeTempLog(String.format("Retrieved populated superword for \"%s\" from cache.", plaintext));
            return cachePopulated.get(plaintext);
        }
        if (cachePlaceholder.containsKey(plaintext)) {
            LOG.writeTempLog(String.format("Retrieved placeholder superword for \"%s\" from cache.", plaintext));
            return cachePlaceholder.get(plaintext);
        }
        return new SuperWord(plaintext);
    }

    public void populate() {
        if (populated) {
            LOG.writeTempLog(String.format("Attempted to repopulate \"%s\": %s", plaintext, this.toString()));
            return;
        }

        JSONObject word = WordsAPI.getWord(plaintext);

        if (word.has("word") && !word.getString("word").equals(plaintext)) {
            LOG.writePersistentLog(String.format("WordsAPI responded with word \"%s\" when requesting \"%s\".",
                    word.getString("word"), plaintext));
        }

        if (word.has("syllables") || word.has("pronunciation"))
            this.pronunciation = new Pronunciation();

        if (word.has("syllables")) {
            JSONObject syllablesObject = word.getJSONObject("syllables");
            this.pronunciation.setSyllables(this.plaintext, syllablesObject);
        } else {
            LOG.writePersistentLog(String.format("Syllables of \"%s\" was missing", plaintext));
        }

        if (word.has("pronunciation")) {
            try {
                JSONObject pronunciationObject = word.getJSONObject("pronunciation");
                this.pronunciation.setIPA(this.plaintext, pronunciationObject);
            } catch (JSONException e) {
                LOG.writePersistentLog(String.format("Pronunciation of \"%s\" was not a JSONObject: \"%s\"", plaintext,
                        word.get("pronunciation").toString()));
                this.pronunciation.setIPA(this.plaintext, word.getString("pronunciation"));
            }
        } else {
            LOG.writePersistentLog(String.format("Pronunciation of \"%s\" was missing", plaintext));
        }

        if (word.has("rhymes")) {
            LOG.writePersistentLog(
                    String.format("Rhymes of \"%s\" was present: %s", plaintext, word.get("rhymes").toString()));
            try {
                JSONObject rhymeObject = word.getJSONObject("rhymes");
                this.pronunciation.setRhyme(this.plaintext, rhymeObject);
            } catch (JSONException e) {
                LOG.writePersistentLog(String.format("Rhymes of \"%s\" was not a JSONObject: \"%s\"", plaintext,
                        word.get("Rhymes").toString()));
                this.pronunciation.setRhyme(this.plaintext, word.getString("Rhymes"));
            }
        }

        if (word.has("results")) {
            JSONArray resultsArray = word.getJSONArray("results");
            this.setWords(resultsArray);
        } else {
            LOG.writePersistentLog(String.format("Results of \"%s\" was missing", plaintext));
            this.partsOfSpeech.add(PartOfSpeech.UNKNOWN);
        }

        populated = true;
        cachePopulated.put(this.plaintext, this);
        LOG.writeTempLog(String.format("Populated and cached \"%s\": %s", plaintext, this.toString()));
    }

    /**
     * For creating placeholders
     * 
     * @param plaintext
     */
    private SuperWord(String plaintext) {
        this.plaintext = plaintext;
        cachePlaceholder.put(this.plaintext, this);
    }

    /**
     * 
     * @param plaintexts JSONArrays are not typed, but must be have String elements.
     * @return
     */
    public static ArrayList<SuperWord> batchPlaceHolders(List<Object> plaintexts) {
        ArrayList<SuperWord> list = new ArrayList<>();
        for (Object plaintext : plaintexts) {
            list.add(getSuperWord((String) plaintext));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private void setWords(JSONArray resultsArray) {
        List<Object> results = resultsArray.toList();

        if (results.isEmpty()) {
            LOG.writePersistentLog(String.format("Results of \"%s\" was an empty array", plaintext));
            partsOfSpeech.add(PartOfSpeech.UNKNOWN);
        }

        for (Object result : results) {
            SubWord word = new SubWord(plaintext, (Map<String, Object>) result);
            partsOfSpeech.add(word.partOfSpeech());
            switch (word.partOfSpeech()) {
                case NOUN:
                    nouns = addToNull(nouns, word);
                    break;
                case PRONOUN:
                    pronouns = addToNull(pronouns, word);
                    break;
                case VERB:
                    verbs = addToNull(verbs, word);
                    break;
                case ADJECTIVE:
                    adjectives = addToNull(adjectives, word);
                    break;
                case ADVERB:
                    adverbs = addToNull(adverbs, word);
                    break;
                case PREPOSITION:
                    prepositions = addToNull(prepositions, word);
                    break;
                case CONJUCTION:
                    conjunctions = addToNull(conjunctions, word);
                    break;
                case DEFINITE_ARTICLE:
                    definiteArticles = addToNull(definiteArticles, word);
                    break;
                case UNKNOWN:
                    unknowns = addToNull(unknowns, word);
                    break;
            }
        }
    }

    public String getPlaintext() {
        return plaintext;
    }

    public ArrayList<SubWord> getNouns() {
        return this.nouns;
    }

    public ArrayList<SubWord> getPronouns() {
        return this.pronouns;
    }

    public ArrayList<SubWord> getVerbs() {
        return this.verbs;
    }

    public ArrayList<SubWord> getAdjectives() {
        return this.adjectives;
    }

    public ArrayList<SubWord> getAdverbs() {
        return this.adverbs;
    }

    public ArrayList<SubWord> getPrepositions() {
        return this.prepositions;
    }

    public ArrayList<SubWord> getConjuctions() {
        return this.conjunctions;
    }

    public ArrayList<SubWord> getDefiniteArticles() {
        return this.definiteArticles;
    }

    public Pronunciation getPronunciation() {
        return this.pronunciation;
    }

    public Pronunciation.SubPronunciation getSubPronunciation(SubWord.PartOfSpeech partOfSpeech) {
        return this.pronunciation.getSubPronunciation(plaintext, partOfSpeech);
    }

    /**
     * TODO: remove duplicates; potentially score words based on duplicate count
     * 
     * @param pos
     * @return
     */
    public ArrayList<SuperWord> getSynonyms(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> synonyms = null;
        switch (pos) {
            case ADJECTIVE:
                if (adjectives != null) {
                    for (SubWord subWord : adjectives) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
            case ADVERB:
                if (adverbs != null) {
                    for (SubWord subWord : adverbs) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
            case CONJUCTION:
                if (conjunctions != null) {
                    for (SubWord subWord : conjunctions) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
            case DEFINITE_ARTICLE:
                if (definiteArticles != null) {
                    for (SubWord subWord : definiteArticles) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
            case NOUN:
                if (nouns != null) {
                    for (SubWord subWord : nouns) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
            case PREPOSITION:
                if (prepositions != null) {
                    for (SubWord subWord : prepositions) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
            case PRONOUN:
                if (pronouns != null) {
                    for (SubWord subWord : pronouns) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
            case UNKNOWN:
                if (unknowns != null) {
                    for (SubWord subWord : unknowns) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
            case VERB:
                if (verbs != null) {
                    for (SubWord subWord : verbs) {
                        synonyms = addAllToNull(synonyms, subWord.getSynonyms());
                    }
                }
                break;
        }
        return synonyms;
    }

 /**
     * TODO: remove duplicates; potentially score words based on duplicate count
     * 
     * @param pos
     * @return
     */
    public ArrayList<SuperWord> getTypeOf(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> types = null;
        switch (pos) {
            case ADJECTIVE:
                for (SubWord subWord : adjectives) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
            case ADVERB:
                for (SubWord subWord : adverbs) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
            case CONJUCTION:
                for (SubWord subWord : conjunctions) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
            case DEFINITE_ARTICLE:
                for (SubWord subWord : definiteArticles) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
            case NOUN:
                for (SubWord subWord : nouns) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
            case PREPOSITION:
                for (SubWord subWord : prepositions) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
            case PRONOUN:
                for (SubWord subWord : pronouns) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
            case UNKNOWN:
                for (SubWord subWord : unknowns) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
            case VERB:
                for (SubWord subWord : verbs) {
                    types = addAllToNull(types, subWord.getTypeOf());
                }
                break;
        }
        return types;
    }


    /**
     * TODO: remove duplicates; potentially score words based on duplicate count
     * 
     * @param pos
     * @return
     */
    public ArrayList<SuperWord> getHasTypes(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> types = null;
        switch (pos) {
            case ADJECTIVE:
                for (SubWord subWord : adjectives) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
            case ADVERB:
                for (SubWord subWord : adverbs) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
            case CONJUCTION:
                for (SubWord subWord : conjunctions) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
            case DEFINITE_ARTICLE:
                for (SubWord subWord : definiteArticles) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
            case NOUN:
                for (SubWord subWord : nouns) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
            case PREPOSITION:
                for (SubWord subWord : prepositions) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
            case PRONOUN:
                for (SubWord subWord : pronouns) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
            case UNKNOWN:
                for (SubWord subWord : unknowns) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
            case VERB:
                for (SubWord subWord : verbs) {
                    types = addAllToNull(types, subWord.getHasTypes());
                }
                break;
        }
        return types;
    }

    /**
     * TODO: remove duplicates; potentially score words based on duplicate count
     * 
     * @param pos
     * @return
     */
    public ArrayList<SuperWord> getCommonlyTyped(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> commonlyTyped = null;
        switch (pos) {
            case ADJECTIVE:
                for (SubWord subWord : adjectives) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
            case ADVERB:
                for (SubWord subWord : adverbs) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
            case CONJUCTION:
                for (SubWord subWord : conjunctions) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
            case DEFINITE_ARTICLE:
                for (SubWord subWord : definiteArticles) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
            case NOUN:
                for (SubWord subWord : nouns) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
            case PREPOSITION:
                for (SubWord subWord : prepositions) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
            case PRONOUN:
                for (SubWord subWord : pronouns) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
            case UNKNOWN:
                for (SubWord subWord : unknowns) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
            case VERB:
                for (SubWord subWord : verbs) {
                    commonlyTyped = addAllToNull(commonlyTyped, subWord.getCommonlyTyped());
                }
                break;
        }
        return commonlyTyped;
    }

    public ArrayList<SuperWord> getHasCategories(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> categories = null;
        switch (pos) {
            case ADJECTIVE:
                for (SubWord subWord : adjectives) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
            case ADVERB:
                for (SubWord subWord : adverbs) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
            case CONJUCTION:
                for (SubWord subWord : conjunctions) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
            case DEFINITE_ARTICLE:
                for (SubWord subWord : definiteArticles) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
            case NOUN:
                for (SubWord subWord : nouns) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
            case PREPOSITION:
                for (SubWord subWord : prepositions) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
            case PRONOUN:
                for (SubWord subWord : pronouns) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
            case UNKNOWN:
                for (SubWord subWord : unknowns) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
            case VERB:
                for (SubWord subWord : verbs) {
                    categories = addAllToNull(categories, subWord.getHasCategories());
                }
                break;
        }
        return categories;
    }

    public ArrayList<SuperWord> getHasParts(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> hasParts = null;
        switch (pos) {
            case ADJECTIVE:
                for (SubWord subWord : adjectives) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
            case ADVERB:
                for (SubWord subWord : adverbs) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
            case CONJUCTION:
                for (SubWord subWord : conjunctions) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
            case DEFINITE_ARTICLE:
                for (SubWord subWord : definiteArticles) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
            case NOUN:
                for (SubWord subWord : nouns) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
            case PREPOSITION:
                for (SubWord subWord : prepositions) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
            case PRONOUN:
                for (SubWord subWord : pronouns) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
            case UNKNOWN:
                for (SubWord subWord : unknowns) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
            case VERB:
                for (SubWord subWord : verbs) {
                    hasParts = addAllToNull(hasParts, subWord.getHasParts());
                }
                break;
        }
        return hasParts;
    }

    public ArrayList<SuperWord> getPartOf(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> partOf = null;
        switch (pos) {
            case ADJECTIVE:
                for (SubWord subWord : adjectives) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
            case ADVERB:
                for (SubWord subWord : adverbs) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
            case CONJUCTION:
                for (SubWord subWord : conjunctions) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
            case DEFINITE_ARTICLE:
                for (SubWord subWord : definiteArticles) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
            case NOUN:
                for (SubWord subWord : nouns) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
            case PREPOSITION:
                for (SubWord subWord : prepositions) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
            case PRONOUN:
                for (SubWord subWord : pronouns) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
            case UNKNOWN:
                for (SubWord subWord : unknowns) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
            case VERB:
                for (SubWord subWord : verbs) {
                    partOf = addAllToNull(partOf, subWord.getPartOf());
                }
                break;
        }
        return partOf;
    }

    public ArrayList<SuperWord> getCommonCategories(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> commonCategories = null;
        switch (pos) {
            case ADJECTIVE:
                for (SubWord subWord : adjectives) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
            case ADVERB:
                for (SubWord subWord : adverbs) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
            case CONJUCTION:
                for (SubWord subWord : conjunctions) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
            case DEFINITE_ARTICLE:
                for (SubWord subWord : definiteArticles) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
            case NOUN:
                for (SubWord subWord : nouns) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
            case PREPOSITION:
                for (SubWord subWord : prepositions) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
            case PRONOUN:
                for (SubWord subWord : pronouns) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
            case UNKNOWN:
                for (SubWord subWord : unknowns) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
            case VERB:
                for (SubWord subWord : verbs) {
                    commonCategories = addAllToNull(commonCategories, subWord.getCommonCategories());
                }
                break;
        }
        return commonCategories;
    }

    /**
     * TODO: remove duplicates; potentially score words based on duplicate count
     * 
     * @param pos
     * @return
     */
    public ArrayList<SuperWord> getSimilarTo(PartOfSpeech pos) {
        if (!populated)
            this.populate();

        ArrayList<SuperWord> similarTo = null;
        switch (pos) {
            case ADJECTIVE:
                for (SubWord subWord : adjectives) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
            case ADVERB:
                for (SubWord subWord : adverbs) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
            case CONJUCTION:
                for (SubWord subWord : conjunctions) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
            case DEFINITE_ARTICLE:
                for (SubWord subWord : definiteArticles) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
            case NOUN:
                for (SubWord subWord : nouns) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
            case PREPOSITION:
                for (SubWord subWord : prepositions) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
            case PRONOUN:
                for (SubWord subWord : pronouns) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
            case UNKNOWN:
                for (SubWord subWord : unknowns) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
            case VERB:
                for (SubWord subWord : verbs) {
                    similarTo = addAllToNull(similarTo, subWord.getSimilarTo());
                }
                break;
        }
        return similarTo;
    }

    public String toString() {
        return String.format("%s: {populated: %b}", this.plaintext, this.populated);
    }

    public String toFullString() {
        String divider = "\n\t";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(plaintext + ": {");
        if (populated) {
            stringBuilder.append(divider);
        }
        stringBuilder.append("populated: " + populated);
        if (pronunciation != null) {
            stringBuilder.append(divider);
            stringBuilder.append("pronunciation: " + pronunciation.toString());
        }
        if (!partsOfSpeech.isEmpty()) {
            stringBuilder.append(divider);
            stringBuilder.append("partsOfSpeech: " + partsOfSpeech.toString());
        }
        if (nouns != null) {
            stringBuilder.append(divider);
            stringBuilder.append("nouns: " + nouns.size());
        }
        if (pronouns != null) {
            stringBuilder.append(divider);
            stringBuilder.append("pronouns: " + pronouns.size());
        }
        if (verbs != null) {
            stringBuilder.append(divider);
            stringBuilder.append("verbs: " + verbs.size());
        }
        if (adjectives != null) {
            stringBuilder.append(divider);
            stringBuilder.append("adjectives: " + adjectives.size());
        }
        if (adverbs != null) {
            stringBuilder.append(divider);
            stringBuilder.append("adverbs: " + adverbs.size());
        }
        if (prepositions != null) {
            stringBuilder.append(divider);
            stringBuilder.append("prepositions: " + prepositions.size());
        }
        if (conjunctions != null) {
            stringBuilder.append(divider);
            stringBuilder.append("conjunction: " + conjunctions.size());
        }
        if (definiteArticles != null) {
            stringBuilder.append(divider);
            stringBuilder.append("definiteArticles: " + definiteArticles.size());
        }
        if (unknowns != null) {
            stringBuilder.append(divider);
            stringBuilder.append("unknowns: " + unknowns.size());
        }
        if (populated) {
            stringBuilder.append("\n");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    /**
     * Warning: this is a messy-looking string.
     * 
     * @return a String formatted for debugging.
     */
    public String subWordsString() {
        String divider = "\n//////// ";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(plaintext + ": {");
        if (nouns != null) {
            stringBuilder.append(divider);
            stringBuilder.append("nouns\n" + nouns.toString());
        }
        if (pronouns != null) {
            stringBuilder.append(divider);
            stringBuilder.append("pronouns\n" + pronouns.toString());
        }
        if (verbs != null) {
            stringBuilder.append(divider);
            stringBuilder.append("verbs\n" + verbs.toString());
        }
        if (adjectives != null) {
            stringBuilder.append(divider);
            stringBuilder.append("adjectives\n" + adjectives.toString());
        }
        if (adverbs != null) {
            stringBuilder.append(divider);
            stringBuilder.append("adverbs\n" + adverbs.toString());
        }
        if (prepositions != null) {
            stringBuilder.append(divider);
            stringBuilder.append("prepositions\n" + prepositions.toString());
        }
        if (conjunctions != null) {
            stringBuilder.append(divider);
            stringBuilder.append("conjunction\n" + conjunctions.toString());
        }
        if (definiteArticles != null) {
            stringBuilder.append(divider);
            stringBuilder.append("definiteArticles\n" + definiteArticles.toString());
        }
        if (unknowns != null) {
            stringBuilder.append(divider);
            stringBuilder.append("unknowns\n" + unknowns.toString());
        }
        if (populated) {
            stringBuilder.append("\n");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();

    }

    private boolean rhmyesWith(String plaintext1, PartOfSpeech pos1, SubPronunciation subPronunciation1,
            String plaintext2, PartOfSpeech pos2, SubPronunciation subPronunciation2) {
        if (subPronunciation1 == null) {
            LOG.writePersistentLog(String.format("\"%s\" did not have a pronunciation for %s to rhyme against",
                    plaintext1, pos1));
            return false;
        }
        if (subPronunciation2 == null) {
            LOG.writePersistentLog(String.format("\"%s\" did not have a pronunciation for %s to rhyme against",
                    plaintext2, pos2));
            return false;
        }
        return subPronunciation1.rhymesWith(subPronunciation2);
    }

    /**
     * Iterates over the parts of speech of both words (worst case is full cross
     * product evaluation) and returns true if any part of speech pairing produces a
     * rhyme.
     * 
     * @param other
     * @return
     */
    public boolean rhymesWithWrapper(SuperWord other) {
        if (!this.populated)
            this.populate();
        if (!other.populated)
            other.populate();

        if (this.pronunciation == null || other.pronunciation == null) {
            return false;
        }

        for (PartOfSpeech pos1 : this.partsOfSpeech) {
            SubPronunciation subPronunciation1 = this.getSubPronunciation(pos1);
            for (PartOfSpeech pos2 : other.partsOfSpeech) {
                SubPronunciation subPronunciation2 = other.getSubPronunciation(pos2);
                if (rhmyesWith(this.plaintext, pos1, subPronunciation1, other.plaintext, pos2, subPronunciation2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Uses specific parts of speech if arguments are not null. For null
     * PartOfSpeech arguments, iterates over all parts of speech of corresponding
     * word.
     * 
     * @param other
     * @param pos1
     * @param pos2
     * @return
     */
    public boolean rhymesWithWrapper(SuperWord other, PartOfSpeech pos1, PartOfSpeech pos2) {
        if (!this.populated)
            this.populate();
        if (!other.populated)
            other.populate();

        if (pos1 == null && pos2 == null) {
            return rhymesWithWrapper(other);
        } else if (pos1 != null && pos2 != null) {
            SubPronunciation subPronunciation1 = this.getSubPronunciation(pos1);
            SubPronunciation subPronunciation2 = other.getSubPronunciation(pos2);
            rhmyesWith(this.plaintext, pos1, subPronunciation1, other.plaintext, pos2, subPronunciation2);
        } else if (pos1 != null) {
            SubPronunciation subPronunciation1 = this.getSubPronunciation(pos1);
            for (PartOfSpeech pos2b : other.partsOfSpeech) {
                SubPronunciation subPronunciation2 = other.getSubPronunciation(pos2b);
                if (rhmyesWith(this.plaintext, pos1, subPronunciation1, other.plaintext, pos2, subPronunciation2)) {
                    return true;
                }
            }
        } else {
            SubPronunciation subPronunciation2 = other.getSubPronunciation(pos2);
            for (PartOfSpeech pos1b : this.partsOfSpeech) {
                SubPronunciation subPronunciation1 = this.getSubPronunciation(pos1b);
                if (rhmyesWith(this.plaintext, pos1, subPronunciation1, other.plaintext, pos2, subPronunciation2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * I only want to order by plaintext, but equality should take more into
     * account. Arguably I should build a custom Comparator rather than setting the
     * default.
     */
    @Override
    public int compareTo(SuperWord anotherWord) {
        if (anotherWord == null) {
            throw new NullPointerException();
        }
        return plaintext.compareTo((anotherWord).plaintext);
    }
}
