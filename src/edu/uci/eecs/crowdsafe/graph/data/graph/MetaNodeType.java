package edu.uci.eecs.crowdsafe.graph.data.graph;

public enum MetaNodeType {
	NORMAL("N", true, true),
	SINGLETON("S", false, true),
	TRAMPOLINE("T", true, true),
	RETURN("R", true, true),
	SIGNAL_HANDLER("SH", true, true),
	SIGRETURN("SR", true, true),
	CLUSTER_ENTRY("E"), // not generated by the VM
	CLUSTER_EXIT("X"); // not generated by the VM

	public final String code;
	public final boolean isExecutable;
	public final boolean isApplicationNode;

	private MetaNodeType(String code) {
		this(code, false, false);
	}

	private MetaNodeType(String code, boolean isExecutable, boolean isApplicationNode) {
		this.code = code;
		this.isExecutable = isExecutable;
		this.isApplicationNode = isApplicationNode;
	}
}
