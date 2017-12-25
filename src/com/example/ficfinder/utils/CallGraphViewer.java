package com.example.ficfinder.utils;

import it.uniroma1.dis.wsngroup.gexf4j.core.EdgeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.Graph;
import it.uniroma1.dis.wsngroup.gexf4j.core.Mode;
import it.uniroma1.dis.wsngroup.gexf4j.core.Node;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.Attribute;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeClass;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeList;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.StaxGraphWriter;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;
import soot.MethodOrMethodContext;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * a viewer for call graph
 *
 * reference: http://blog.csdn.net/liu3237/article/details/48827523
 */
public class CallGraphViewer {

    private Gexf gexf;

    private Graph graph;

    private CallGraph callGraph;

    private SootMethod entryPoint;

    private Map<String,Boolean> visited = new HashMap<>();

    public CallGraphViewer(CallGraph callGraph, SootMethod entryPoint) {
        this.gexf = new GexfImpl();
        this.graph = this.gexf.getGraph();
        this.gexf.getMetadata().setCreator("simonlee").setDescription("Android App Call Graph");
        this.gexf.setVisualization(true);
        this.graph.setDefaultEdgeType(EdgeType.DIRECTED).setMode(Mode.STATIC);
        this.callGraph = callGraph;
        this.entryPoint = entryPoint;
    }

    public void export(String name, String path) {
        this.visit(this.callGraph, this.entryPoint);

        String outPath = path + "/" + name + ".gexf";
        StaxGraphWriter graphWriter = new StaxGraphWriter();
        File f = new File(outPath);
        Writer out;

        try {
            out = new FileWriter(f, false);
            graphWriter.writeToStream(this.gexf, out, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Node getNodeByID(String Id) {
        List<Node> nodes = this.graph.getNodes();
        Node nodeFinded = null;
        for (Node node : nodes) {
            String nodeID = node.getId();
            if (nodeID.equals(Id)) {
                nodeFinded = node;
                break;
            }
        }
        return nodeFinded;
    }

    public void linkNodeByID(String sourceID, String targetID) {
        Node sourceNode = this.getNodeByID(sourceID);
        Node targetNode = this.getNodeByID(targetID);
        if (sourceNode.equals(targetNode)) {
            return;
        }
        if (!sourceNode.hasEdgeTo(targetID)) {
            String edgeID = sourceID + "-->" + targetID;
            sourceNode.connectTo(edgeID, "", EdgeType.DIRECTED, targetNode);
        }
    }

    public void createNode(String m) {
        String id = m;
        if (getNodeByID(id) != null) { return; }
        Node node = this.graph.createNode(id);
        node.setSize(20);
    }

    public void visit(CallGraph cg, SootMethod m){
        String identifier = m.getSignature();
        visited.put(m.getSignature(), true);
        this.createNode(m.getSignature());

        Iterator<MethodOrMethodContext> ptargets = new Targets(cg.edgesInto(m));
        if(ptargets != null) {
            while(ptargets.hasNext()) {
                SootMethod p = (SootMethod) ptargets.next();
                if(p == null) { continue; }
                if(!visited.containsKey(p.getSignature())){
                    visit(cg,p);
                }
            }
        }

        Iterator<MethodOrMethodContext> ctargets = new Targets(cg.edgesOutOf(m));
        if(ctargets != null) {
            while(ctargets.hasNext()) {
                SootMethod c = (SootMethod) ctargets.next();
                if(c == null) { continue; }
                this.createNode(c.getSignature());
                this.linkNodeByID(identifier, c.getSignature());
                if(!visited.containsKey(c.getSignature())){
                    visit(cg,c);
                }
            }
        }
    }
}