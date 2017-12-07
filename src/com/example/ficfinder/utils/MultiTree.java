package com.example.ficfinder.utils;

import java.util.ArrayList;
import java.util.List;

public class MultiTree<D> {

    interface Visitor<D, R> {
        R visit(Node<D> n);
    }

    public MultiTree() {

    }

    public static class Node<D> {

        private D data = null;
        private Node<D> parent = null;
        private List<Node<D>> children = new ArrayList<>();

        public D getData() {
            return data;
        }

        public void setData(D data) {
            this.data = data;
        }

        public Node<D> getParent() {
            return parent;
        }

        public void setParent(Node<D> parent) {
            this.parent = parent;
        }

        public List<Node<D>> getChildren() {
            return children;
        }

        public void setChildren(List<Node<D>> children) {
            this.children = children;
        }

    }

}
