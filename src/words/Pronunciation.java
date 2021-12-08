package words;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import utils.Pair;

import static config.Configuration.LOG;

/**
 * I could use a JSONObject instead of this class, which will have a lot of null
 * attributes. Provided they stay null the memory footprint should not be
 * unreasonable.
 */
public class Pronunciation {

    public class SubPronunciation {
        String ipa;
        ArrayList<Syllable> syllables;
        ArrayList<Syllable> primaryRhyme;
        ArrayList<ArrayList<Syllable>> secondaryRhymes;
        Emphasis emphasis;

        ArrayList<Syllable> populateRhyme(int startingEmphasis) {
            ArrayList<Syllable> rhyme = new ArrayList<>();
            Syllable source = syllables.get(startingEmphasis);
            Syllable start = new Syllable("", source.getNucleus(), source.getCoda());
            rhyme.add(start);
            for (int i = startingEmphasis + 1; i < syllables.size(); i++) {
                rhyme.add(syllables.get(i));
            }
            return rhyme;
        }

        /**
         * Not that this does not create copies of syllables aside from the stressed
         * ones, so changing the syllables later could lead to unexpected behaviour. At
         * present, not such changes occur.
         */
        void populateRhymes() {
            /* primary rhyme */
            this.primaryRhyme = populateRhyme(this.emphasis.getPrimary());

            /* secondary rhymes */
            if (this.emphasis.getSecondary() != null) {
                for (Integer secondaryEmphasis : this.emphasis.getSecondary()) {
                    this.secondaryRhymes = utils.NullListOperations.addToNull(this.secondaryRhymes,
                            populateRhyme(secondaryEmphasis));
                }
            }
        }

        private boolean rhymeMatch(ArrayList<Syllable> rhyme1, ArrayList<Syllable> rhyme2) {
            if (rhyme1.size() != rhyme2.size()) {
                return false;
            }
            for (int i = 0; i < rhyme1.size(); i++) {
                if (!rhyme1.get(i).rhymes(rhyme2.get(i))) {
                    return false;
                }
            }
            return true;
        }

        boolean rhymesWith(SubPronunciation other) {
            /* primary to primary */
            if (this.rhymeMatch(this.primaryRhyme, other.primaryRhyme)) {
                return true;
            }

            /* primary to secondary */
            if (other.secondaryRhymes != null) {
                for (ArrayList<Syllable> secondary : other.secondaryRhymes) {
                    if (this.rhymeMatch(this.primaryRhyme, secondary)) {
                        return true;
                    }
                }
            }

            /* secondary to primary */
            if (this.secondaryRhymes != null) {
                for (ArrayList<Syllable> secondary : this.secondaryRhymes) {
                    if (this.rhymeMatch(secondary, other.primaryRhyme)) {
                        return true;
                    }
                }
            }

            return false;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            stringBuilder.append("ipa: " + ipa);
            stringBuilder.append(", ");
            stringBuilder.append("syllables: " + syllables.toString());
            stringBuilder.append(", ");
            stringBuilder.append("emphasis: " + emphasis.toString());
            stringBuilder.append(", ");
            stringBuilder.append("primary rhyme: " + primaryRhyme.toString());
            if (this.secondaryRhymes != null) {
                stringBuilder.append(", ");
                stringBuilder.append("secondary rhymes: " + secondaryRhymes.toString());
            }
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private ArrayList<String> plaintextSyllables = new ArrayList<>();

    private SubPronunciation noun;
    private SubPronunciation pronoun;
    private SubPronunciation verb;
    private SubPronunciation adjective;
    private SubPronunciation adverb;
    private SubPronunciation preposition;
    private SubPronunciation conjunction;
    private SubPronunciation all;

    public ArrayList<String> getPlaintextSyllables() {
        return plaintextSyllables;
    }

    /**
     * 
     * @param syllablesObject JSONObject of the form {"count":x, "list":[]}
     */
    public void setSyllables(JSONObject syllablesObject) {
        if (syllablesObject.has("count") && syllablesObject.has("list")) {
            this.plaintextSyllables = new ArrayList<>();
            JSONArray syllableArray = syllablesObject.getJSONArray("list");

            for (int i = 0; i < syllablesObject.getInt("count"); i++) {
                this.plaintextSyllables.add(syllableArray.getString(i));
            }
        }
    }

    /**
     * 
     * @param pronunciationObject JSONObject of the form {<part-of-speech>:<ipa>}
     *                            e.g.
     *                            {"noun":"'kɑntrækt", "verb":"kɑn'trækt"}
     *                            {"all":"'fridəm"}
     * 
     */
    public void setIPA(JSONObject pronunciationObject) {
        Pair<ArrayList<Syllable>, Emphasis> syllablesAndEmphasis;

        if (pronunciationObject.has("noun")) {
            this.noun = new SubPronunciation();
            this.noun.ipa = pronunciationObject.getString("noun");
            syllablesAndEmphasis = IPAHandler.getSyllables(this.noun.ipa);
            this.noun.syllables = syllablesAndEmphasis.one();
            this.noun.emphasis = syllablesAndEmphasis.two();
            this.noun.populateRhymes();
        }

        if (pronunciationObject.has("pronoun")) {
            this.pronoun = new SubPronunciation();
            this.pronoun.ipa = pronunciationObject.getString("pronoun");
            syllablesAndEmphasis = IPAHandler.getSyllables(this.pronoun.ipa);
            this.pronoun.syllables = syllablesAndEmphasis.one();
            this.pronoun.emphasis = syllablesAndEmphasis.two();
            this.pronoun.populateRhymes();
        }

        if (pronunciationObject.has("verb")) {
            this.verb = new SubPronunciation();
            this.verb.ipa = pronunciationObject.getString("verb");
            syllablesAndEmphasis = IPAHandler.getSyllables(this.verb.ipa);
            this.verb.syllables = syllablesAndEmphasis.one();
            this.verb.emphasis = syllablesAndEmphasis.two();
            this.verb.populateRhymes();
        }

        if (pronunciationObject.has("adjective")) {
            this.adjective = new SubPronunciation();
            this.adjective.ipa = pronunciationObject.getString("adjective");
            syllablesAndEmphasis = IPAHandler.getSyllables(this.adjective.ipa);
            this.adjective.syllables = syllablesAndEmphasis.one();
            this.adjective.emphasis = syllablesAndEmphasis.two();
            this.adjective.populateRhymes();
        }

        if (pronunciationObject.has("adverb")) {
            this.adverb = new SubPronunciation();
            this.adverb.ipa = pronunciationObject.getString("adverb");
            syllablesAndEmphasis = IPAHandler.getSyllables(this.adverb.ipa);
            this.adverb.syllables = syllablesAndEmphasis.one();
            this.adverb.emphasis = syllablesAndEmphasis.two();
            this.adverb.populateRhymes();
        }

        if (pronunciationObject.has("preposition")) {
            this.preposition = new SubPronunciation();
            this.preposition.ipa = pronunciationObject.getString("preposition");
            syllablesAndEmphasis = IPAHandler.getSyllables(this.preposition.ipa);
            this.preposition.syllables = syllablesAndEmphasis.one();
            this.preposition.emphasis = syllablesAndEmphasis.two();
            this.preposition.populateRhymes();
        }

        if (pronunciationObject.has("conjunction")) {
            this.conjunction = new SubPronunciation();
            this.conjunction.ipa = pronunciationObject.getString("conjunction");
            syllablesAndEmphasis = IPAHandler.getSyllables(this.conjunction.ipa);
            this.conjunction.syllables = syllablesAndEmphasis.one();
            this.conjunction.emphasis = syllablesAndEmphasis.two();
        }

        if (pronunciationObject.has("all")) {
            // "present" is a noun, verb and adjective, with pronunications for "noun",
            // "verb" and "all": "all" is useful even when other fields are filled
            this.all = new SubPronunciation();
            this.all.ipa = pronunciationObject.getString("all");
            syllablesAndEmphasis = IPAHandler.getSyllables(this.all.ipa);
            this.all.syllables = syllablesAndEmphasis.one();
            this.all.emphasis = syllablesAndEmphasis.two();
            this.all.populateRhymes();
        }

    }

    public void setIPA(String all) {
        this.all = new SubPronunciation();
        this.all.ipa = all;
        Pair<ArrayList<Syllable>, Emphasis> syllablesAndEmphasis = IPAHandler.getSyllables(this.all.ipa);
        this.all.syllables = syllablesAndEmphasis.one();
        this.all.emphasis = syllablesAndEmphasis.two();
        this.all.populateRhymes();
    }

    public SubPronunciation getSubPronunciation(SubWord.PartOfSpeech partOfSpeech) {
        SubPronunciation requested = this.all;
        switch (partOfSpeech) {
            case ADJECTIVE:
                if (this.adjective != null)
                    requested = adjective;
                break;
            case ADVERB:
                if (this.adverb != null)
                    requested = adverb;
                break;
            case CONJUCTION:
                if (this.conjunction != null)
                    requested = conjunction;
                break;
            case NOUN:
                if (this.noun != null)
                    requested = noun;
                break;
            case PREPOSITION:
                if (this.preposition != null)
                    requested = preposition;
                break;
            case PRONOUN:
                if (this.pronoun != null)
                    requested = pronoun;
                break;
            case VERB:
                if (this.verb != null)
                    requested = verb;
                break;
            case DEFINITE_ARTICLE:
            case UNKNOWN:
            default:
                // leave request as all
                break;
        }
        if (requested == null) {
            LOG.writePersistentLog(String.format("No pronunciation of \"%s\" could be found for \"%s\"",
                    this.plaintextSyllables.toString(), partOfSpeech));
        }
        return requested;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append("plaintext-syllables: " + plaintextSyllables.toString());
        if (noun != null) {
            stringBuilder.append(", ");
            stringBuilder.append("noun: " + noun.toString());
        }
        if (pronoun != null) {
            stringBuilder.append(", ");
            stringBuilder.append("pronoun: " + pronoun.toString());
        }
        if (verb != null) {
            stringBuilder.append(", ");
            stringBuilder.append("verb: " + verb.toString());
        }
        if (adjective != null) {
            stringBuilder.append(", ");
            stringBuilder.append("adjective: " + adjective.toString());
        }
        if (adverb != null) {
            stringBuilder.append(", ");
            stringBuilder.append("adverb: " + adverb.toString());
        }
        if (preposition != null) {
            stringBuilder.append(", ");
            stringBuilder.append("preposition: " + preposition.toString());
        }
        if (conjunction != null) {
            stringBuilder.append(", ");
            stringBuilder.append("conjunction: " + conjunction.toString());
        }
        if (all != null) {
            stringBuilder.append(", ");
            stringBuilder.append("all: " + all.toString());
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

}
