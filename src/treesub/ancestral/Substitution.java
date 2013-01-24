package treesub.ancestral;

import com.google.common.base.Predicate;

/**
 * @author tamuri@ebi.ac.uk
 *
 */
public class Substitution {
    int site;
    String codonTo, codonFrom;
    char aaTo, aaFrom;

    Substitution(int site, String codonFrom, String codonTo, char aaFrom, char aaTo) {
        this.site = site;
        this.codonFrom = codonFrom;
        this.codonTo = codonTo;
        this.aaFrom = aaFrom;
        this.aaTo = aaTo;
    }

    public boolean isSynonymous() {
        return this.aaFrom == this.aaTo;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s", aaFrom, site, aaTo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Substitution that = (Substitution) o;

        return site == that.site && !(codonFrom != null ? !codonFrom.equals(that.codonFrom) : that.codonFrom != null) && !(codonTo != null ? !codonTo.equals(that.codonTo) : that.codonTo != null);

    }

    @Override
    public int hashCode() {
        int result = site;
        result = 31 * result + (codonTo != null ? codonTo.hashCode() : 0);
        result = 31 * result + (codonFrom != null ? codonFrom.hashCode() : 0);
        return result;
    }

    static class isSynSubPredicate implements Predicate<Substitution> {
        public boolean apply(Substitution substitution) {
            return substitution.isSynonymous();
        }
    }
}
