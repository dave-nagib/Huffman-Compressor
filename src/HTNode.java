public class HTNode implements Comparable<HTNode> {
    ByteWord character;
    int frequency;
    HTNode left, right;

    public HTNode(ByteWord character, int frequency) {
        this.character = character;
        this.frequency = frequency;
    }

    public void setLeft(HTNode left) {
        this.left = left;
    }

    public void setRight(HTNode right) {
        this.right = right;
    }

    @Override
    public int compareTo(HTNode o) {
        return this.frequency - o.frequency;
    }
}
