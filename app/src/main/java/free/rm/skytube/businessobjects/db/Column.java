package free.rm.skytube.businessobjects.db;

class Column {

    final String name;
    final String type;
    public Column(final String name, final String type) {
        this.name = name;
        this.type = type;
    }

    public String format() {
        return name + ' ' + type + ", ";
    }
}
