package edu.uci.plrg.cfi.x86.graph.data.graph.modular.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uci.plrg.cfi.common.log.Log;
import edu.uci.plrg.cfi.x86.graph.data.application.ApplicationModule;
import edu.uci.plrg.cfi.x86.graph.data.graph.Edge;
import edu.uci.plrg.cfi.x86.graph.data.graph.ModuleGraph;
import edu.uci.plrg.cfi.x86.graph.data.graph.NodeIdentifier;
import edu.uci.plrg.cfi.x86.graph.data.graph.OrdinalEdgeList;
import edu.uci.plrg.cfi.x86.graph.data.graph.modular.ModuleNode;
import edu.uci.plrg.cfi.x86.graph.data.graph.modular.metadata.ModuleMetadataExecution;
import edu.uci.plrg.cfi.x86.graph.data.graph.modular.metadata.ModuleMetadataSequence;
import edu.uci.plrg.cfi.x86.graph.data.graph.modular.metadata.ModuleUIB;
import edu.uci.plrg.cfi.x86.graph.io.modular.ModularTraceDataSink;

public class ModuleGraphWriter implements ModuleDataWriter.ModularData {

	private final ModuleGraph<ModuleNode<?>> graph;

	private final Map<NodeIdentifier, Integer> nodeIndexMap = new HashMap<NodeIdentifier, Integer>();

	private final ModuleDataWriter dataWriter;

	public ModuleGraphWriter(ModuleGraph<ModuleNode<?>> graph, ModularTraceDataSink dataSink) throws IOException {
		this.graph = graph;

		dataWriter = new ModuleDataWriter(this, dataSink);
	}

	public void writeGraph() throws IOException {
		Map<Edge<ModuleNode<?>>, Integer> edgeIndexMap = writeGraphBody();
		writeMetadata(edgeIndexMap);
	}

	public Map<Edge<ModuleNode<?>>, Integer> writeGraphBody() throws IOException {
		List<Edge<ModuleNode<?>>> allEdges = new ArrayList<Edge<ModuleNode<?>>>();
		OrdinalEdgeList<ModuleNode<?>> edges;

		for (ModuleNode<?> node : graph.getAllNodes()) {
			nodeIndexMap.put(node, nodeIndexMap.size());
			dataWriter.writeNode(node);

			edges = node.getOutgoingEdges();
			try {
				for (Edge<ModuleNode<?>> edge : edges) {
					allEdges.add(edge);
				}
			} finally {
				edges.release();
			}
		}

		int edgeIndex = 0;
		Map<Edge<ModuleNode<?>>, Integer> edgeIndexMap = new HashMap<Edge<ModuleNode<?>>, Integer>();
		for (Edge<ModuleNode<?>> edge : allEdges) {
			dataWriter.writeEdge(edge);
			edgeIndexMap.put(edge, edgeIndex++);
		}
		dataWriter.flush();

		return edgeIndexMap;
	}

	public void writeMetadata(Map<Edge<ModuleNode<?>>, Integer> edgeIndexMap) throws IOException {
		for (ModuleMetadataSequence sequence : graph.metadata.sequences.values()) {
			for (ModuleMetadataExecution execution : sequence.executions) {
				for (int i = execution.uibs.size() - 1; i >= 0; i--) {
					ModuleUIB uib = execution.uibs.get(i);
					if (edgeIndexMap.get(uib.edge) == null) {
						Log.log("Error! Found a UIB for missing edge %s.", uib.edge);
						execution.uibs.remove(i);
					}
				}
			}
		}

		dataWriter.writeMetadataHistory(graph.metadata, edgeIndexMap);
		dataWriter.flush();
	}
	
	public void close() throws IOException {
		dataWriter.close();
	}

	@Override
	public ApplicationModule getModule() {
		return graph.module;
	}

	@Override
	public int getNodeIndex(NodeIdentifier node) {
		return nodeIndexMap.get(node);
	}
}
