public class ReferencedEntry {
    private TableEntryType actualType;
    private TableEntryType referencedType;

    private final int dimension1; // ident[d1]
    private final int dimension2; // ident[d1][dimension2]

    public ReferencedEntry(TableEntryType actualType, TableEntryType referencedType, int dimension1, int dimension2) {
        this.actualType = actualType;
        this.referencedType = referencedType;
        this.dimension1 = dimension1;
        this.dimension2 = dimension2;
    }

    public ReferencedEntry(TableEntryType actualType, TableEntryType referencedType, int dimension1) {
        this.actualType = actualType;
        this.referencedType = referencedType;
        this.dimension1 = dimension1;
        this.dimension2 = -1;
    }

    public TableEntryType getActualType() {
        return actualType;
    }
}
