package Values;

import Type.Type;

public class Argument extends Value {

        // 函数形参
        public Argument(String name, Type type, Value parent) {
            super("%" + name, type, parent);
        }

        public String toString() {
            return getName();
        }
}
