package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.HashSet;

/**
 * This class flattens the stream graph
 */

public class RawFlattener extends at.dms.util.Utils implements FlatVisitor
{
    private static FlatNode currentNode;
    private static StringBuffer buf;
    
    public static FlatNode top;
        
    public static void flatten(SIROperator toplevel) 
    {
	createGraph(toplevel);
    }
    
    private static void createGraph(SIROperator current) 
    {
	if (current instanceof SIRFilter) {
	    FlatNode node = new FlatNode(current);
	    if (top == null) {
		currentNode = node;
		top = node;
	    }
	    
	    currentNode.addEdgeTo(node);
	    currentNode = node;
	}
	if (current instanceof SIRPipeline){
	    SIRPipeline pipeline = (SIRPipeline) current;
	    
	    for (int i=0; i<pipeline.size(); i++) {
		createGraph(pipeline.get(i));
	    }
	}
	if (current instanceof SIRSplitJoin) {
	    SIRSplitJoin sj = (SIRSplitJoin) current;
	    FlatNode splitterNode = new FlatNode (sj.getSplitter());
	    if (top == null) {
		currentNode = splitterNode;
		top = splitterNode;
	    }
	    
	    FlatNode joinerNode = new FlatNode (sj.getJoiner());
	    	    
	    currentNode.addEdgeTo(splitterNode);
	    for (int i = 0; i < sj.size(); i++) {
		currentNode = splitterNode;
		createGraph(sj.get(i));
		currentNode.addEdgeTo(joinerNode);
	    }
	    currentNode = joinerNode;
	    
	}
	if (current instanceof SIRFeedbackLoop) {
	    SIRFeedbackLoop loop = (SIRFeedbackLoop)current;
	    FlatNode joinerNode = new FlatNode(loop.getJoiner());
	    if (top == null) {
		currentNode = joinerNode;
		top = joinerNode;
	    }
	    FlatNode splitterNode = new FlatNode(loop.getSplitter());

	    currentNode.addEdgeTo(joinerNode);
	    	    
	    currentNode = joinerNode;
	    createGraph(loop.getBody());
	    currentNode.addEdgeTo(splitterNode);
	    
	    currentNode = splitterNode;
	    createGraph(loop.getLoop());
	    currentNode.addEdgeTo(joinerNode);
	    currentNode = splitterNode;
	}
    }

    public static void dumpGraph() 
    {
	buf = new StringBuffer();
	
	buf.append("digraph Flattend {\n");
	top.accept(new RawFlattener(), new HashSet(), true);
	buf.append("}\n");
	try {
	    FileWriter fw = new FileWriter("flatgraph.dot");
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Could not print flattened graph");
	}
	
    }

    public void visitNode(FlatNode node) 
    {
	for (int i = 0; i < node.ways; i++) {
	    buf.append(Namer.getName(node.contents) + " -> " 
		       + Namer.getName(node.edges[i].contents));
	    buf.append("[label=\"" + node.weights[i] + "\"];\n");
	}
    }
}



