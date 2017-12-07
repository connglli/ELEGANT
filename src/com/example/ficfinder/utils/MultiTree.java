package com.example.ficfinder.utils;

import java.util.ArrayList;
import java.util.List;

public class MultiTree<D> {

    //
    // TODO complete it
    //

    public interface Visitor<D, R> {
        void visit(Node<D> n, R result);
    }

    public enum VisitManner {
        DEPTH_FIRST, BREADTH_FIRST,
        PREORDER, POSTORDER
    }

    public static class Node<D> {

        private D data = null;
        private List<Node<D>> children = new ArrayList<>();

        public Node() { }

        public Node(D data) {
            this.data = data;
        }

        public D getData() {
            return data;
        }

        public void setData(D data) {
            this.data = data;
        }

        public List<Node<D>> getChildren() {
            return children;
        }

        public void setChildren(List<Node<D>> children) {
            this.children = children;
        }

        public void addChild(Node<D> c) {
            this.children.add(c);
        }

    }

    private Node<D> root;

    public MultiTree() { }

    public MultiTree(Node<D> root) {
        this.root = root;
    }

    public Node<D> getRoot() {
        return root;
    }

    public void setRoot(Node<D> root) {
        this.root = root;
    }

    public <R> void accept(VisitManner manner, Visitor<D, R> visitor, R result) {
        switch (manner) {
            case PREORDER: acceptByPreorder(this.root, visitor, result); break;
            default: break;
        }
    }

    private  <R> void acceptByPreorder(Node<D> n, Visitor<D, R> visitor, R result) {
        visitor.visit(n, result);
        for (Node<D> c : n.getChildren()) {
            acceptByPreorder(c, visitor, result);
        }
    }

}
