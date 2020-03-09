package variance;

import java.util.ArrayList;
import java.util.List;

public class MyJavaList<A> {
    private List<A> data = new ArrayList<>();

    public void add(A item) {
        data.add(item);
    }

    public A get(Integer idx) {
        return data.get(idx);
    }

    public static void printItem(MyJavaList<Object> list) {
        System.out.println(list.get(0));
    }

    public static void main(String args[]) {
        MyJavaList<Integer> l = new MyJavaList<>();

        l.add(1);
        l.add(2);

        // printItem(l);
    }
}
