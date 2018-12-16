import java.util.ArrayList;

public class CircularList<E> extends ArrayList<E> {

    public CircularList() {
        super();
    }

    public CircularList(int initialCapacity) {
        super(initialCapacity);
    }

    E getNext(int index) {
        return get(index % size());
    }

    E getPrevious(int index) {
        if (index == 0) {
            return get(size()-1);
        }
        return get(index);
    }
}
