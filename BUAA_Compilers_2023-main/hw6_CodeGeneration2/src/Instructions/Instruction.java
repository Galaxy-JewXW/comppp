package Instructions;

import STable.TableEntryType;
import Type.Type;
import Values.User;
import Values.Value;

public class Instruction extends User {

    public Instruction(String name, Type type, Value parent, Value... values) {
        super(name, type, parent, values);
    }
}
