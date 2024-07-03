package STable;

public class ReferencedEntry {
    private TableEntryType actualType;
    private TableEntryType referencedType;

    private int d1; // ident[d1]
    private int d2; // ident[d1][d2]

    public ReferencedEntry(TableEntryType actualType, TableEntryType referencedType, int d1, int d2) {
        this.actualType = actualType;
        this.referencedType = referencedType;
        this.d1 = d1;
        this.d2 = d2;
    }

    public ReferencedEntry(TableEntryType actualType, TableEntryType referencedType, int d1) {
        this.actualType = actualType;
        this.referencedType = referencedType;
        this.d1 = d1;
        this.d2 = -1;
    }

    public TableEntryType getActualType() {
        return actualType;
    }
}
