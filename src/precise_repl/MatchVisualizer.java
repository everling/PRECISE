package precise_repl;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultWeightedEdge;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxStylesheet;

public class MatchVisualizer {

    private static <X,Y> void createAndShowGui(String title, ListenableGraph<X,Y> dw) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Map<String, Object> edgeStyle = new HashMap<String, Object>();
	    
        edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL);
	    edgeStyle.put(mxConstants.STYLE_SHAPE,    mxConstants.SHAPE_CONNECTOR);
	    edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
	    edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
	    edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
	    edgeStyle.put(mxConstants.STYLE_NOLABEL, true);
	    
	    mxStylesheet stylesheet = new mxStylesheet();
      	stylesheet.setDefaultEdgeStyle(edgeStyle);
        
        JGraphXAdapter<X, Y> graphAdapter = new JGraphXAdapter<X, Y>(dw);
        graphAdapter.setStylesheet(stylesheet);
        mxCompactTreeLayout layout = new mxCompactTreeLayout(graphAdapter);
        layout.setNodeDistance(30);
        layout.setLevelDistance(60);
        layout.execute(graphAdapter.getDefaultParent());

        frame.add(new mxGraphComponent(graphAdapter));
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Visualize a graph
     * @param title
     * @param dw
     */
    public static void visualize(final String title,final ListenableGraph<Node,DefaultWeightedEdge> dw){
    	 SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 createAndShowGui(title,dw);
             }
         });
    }

}